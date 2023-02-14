package ethbr

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"io"
	"math/big"
	"sort"
	"unsafe"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/rlp"

	"github.com/icon-project/btp/chain/ethbr/binding"
	"github.com/icon-project/btp/chain/ethbr/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/log"
	btpTypes "github.com/icon-project/btp/common/types"
)

type receiveStatus struct {
	height int64
	seq    int64
	rps    []*client.ReceiptProof
}

func (r *receiveStatus) Height() int64 {
	return r.height
}

func (r *receiveStatus) Seq() int64 {
	return r.seq
}

func newReceiveStatus(height, seq int64, rps []*client.ReceiptProof) (*receiveStatus, error) {
	return &receiveStatus{
		height: height,
		seq:    seq,
		rps:    rps,
	}, nil
}

const (
	EPOCH               = 200
	EventSignature      = "Message(string,uint256,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type ethbr struct {
	l           log.Logger
	src         btpTypes.BtpAddress
	dst         btpTypes.BtpAddress
	c           *client.Client
	nid         int64
	rsc         chan link.ReceiveStatus
	rss         []*receiveStatus
	seq         int64
	startHeight int64
}

func NewEthBridge(src, dst btpTypes.BtpAddress, endpoint string, l log.Logger) *ethbr {
	c := &ethbr{
		src: src,
		dst: dst,
		l:   l,
		rsc: make(chan link.ReceiveStatus),
		rss: make([]*receiveStatus, 0),
	}
	c.c = client.NewClient(endpoint, l)
	return c
}

func (e *ethbr) Start(bs *btpTypes.BMCLinkStatus) (<-chan link.ReceiveStatus, error) {
	go func() {
		e.Monitoring(bs)
	}()

	return e.rsc, nil
}

func (e *ethbr) Stop() {
	close(e.rsc)
}

func (e *ethbr) GetStatus() (link.ReceiveStatus, error) {
	return e.rss[len(e.rss)-1], nil
}

func (e *ethbr) BuildBlockUpdate(bls *btpTypes.BMCLinkStatus, limit int64) ([]link.BlockUpdate, error) {
	e.updateReceiveStatus(bls)
	bus := make([]link.BlockUpdate, 0)
	for _, rs := range e.rss {
		bu := NewBlockUpdate(bls, rs.Height())
		bus = append(bus, bu)
	}
	return bus, nil
}

func (e *ethbr) BuildBlockProof(bls *btpTypes.BMCLinkStatus, height int64) (link.BlockProof, error) {
	return nil, nil
}

func (e *ethbr) BuildMessageProof(bls *btpTypes.BMCLinkStatus, limit int64) (link.MessageProof, error) {
	var rmSize int
	var seq int
	rps := make([]*client.ReceiptProof, 0)
	rs := e.GetReceiveHeightForSequence(bls.RxSeq + 1)
	e.l.Debugf("OnBlockOfSrc rpsCnt:%d rxSeq:%d", len(rs.rps), rs.Seq())
	if rs == nil {
		return nil, nil
	}

	rpsCnt := int64(len(rs.rps))
	offset := bls.RxSeq - (rs.Seq() - rpsCnt)
	if rpsCnt > 0 {
		for i := offset; i < rpsCnt; i++ {
			trp := &client.ReceiptProof{
				Index:  rs.rps[i].Index,
				Events: make([]*client.Event, 0),
				Height: rs.rps[i].Height,
			}
			rps = append(rps, trp)

			for _, e := range rs.rps[i].Events {
				size := sizeOfEvent(e)

				if (int(limit) < rmSize+size) && rmSize > 0 {
					return NewMessageProof(bls, bls.RxSeq+i, rps)
				}
				trp.Events = append(trp.Events, e)
				seq += 1
				rmSize += size
			}

			//last event
			if int(limit) < rmSize {
				return NewMessageProof(bls, bls.RxSeq+i, rps)
			}

			//remove last receipt if empty
			if len(trp.Events) == 0 {
				rps = rps[:len(rps)-1]
			}
		}

		return NewMessageProof(bls, bls.RxSeq+int64(seq), rps)
	}
	return nil, nil
}

func (e *ethbr) GetHeightForSeq(seq int64) int64 {
	rs := e.GetReceiveHeightForSequence(seq)
	if rs != nil {
		return e.GetReceiveHeightForSequence(seq).height
	} else {
		return 0
	}
}

func (e *ethbr) BuildRelayMessage(rmis []link.RelayMessageItem) ([]byte, error) {
	//delete blockUpdate and only mp append
	for _, rmi := range rmis {
		if rmi.Type() == link.TypeMessageProof {
			mp := rmi.(*MessageProof)
			//TODO for test
			e.l.Debugf("BuildRelayMessage height:%d data:%s ", mp.nextBls.Verifier.Height,
				base64.URLEncoding.EncodeToString(mp.Bytes()))
			return mp.Bytes(), nil
		}
	}
	return nil, nil
}

func (e *ethbr) updateReceiveStatus(bls *btpTypes.BMCLinkStatus) {
	for i, rs := range e.rss {
		if rs.Height() <= bls.Verifier.Height && rs.Seq() <= bls.RxSeq {
			e.rss = e.rss[i+1:]
			return
		}
	}
}

func (e *ethbr) Monitoring(bs *btpTypes.BMCLinkStatus) error {
	//var err error
	fq := &ethereum.FilterQuery{
		Addresses: []common.Address{common.HexToAddress(e.src.ContractAddress())},
		Topics: [][]common.Hash{
			{crypto.Keccak256Hash([]byte(EventSignature))},
			//{crypto.Keccak256Hash([]byte(r.dst.String()))}, //if 'next' is indexed
		},
	}
	e.l.Debugf("ReceiveLoop height:%d seq:%d filterQuery[Address:%s,Topic:%s]",
		bs.Verifier.Height, bs.RxSeq, fq.Addresses[0].String(), fq.Topics[0][0].Hex())
	br := &client.BlockRequest{
		Height:      big.NewInt(bs.Verifier.Height),
		FilterQuery: fq,
	}

	if bs.RxSeq != 0 {
		e.seq = bs.RxSeq
	}

	return e.c.MonitorBlock(br,
		func(v *client.BlockNotification) error {
			if len(v.Logs) > 0 {
				rpsMap := make(map[uint]*client.ReceiptProof)
			EpLoop:
				for _, el := range v.Logs {
					evt, err := logToEvent(&el)
					e.l.Debugf("event[seq:%d next:%s] seq:%d dst:%s",
						evt.Sequence, evt.Next, e.seq, e.dst.String())
					if err != nil {
						return err
					}
					if evt.Sequence.Int64() <= e.seq {
						continue EpLoop
					}
					//below statement is unnecessary if 'next' is indexed
					if evt.Next.String() != e.dst.String() {
						continue EpLoop
					}
					rp, ok := rpsMap[el.TxIndex]
					if !ok {
						rp = &client.ReceiptProof{
							Index:  int64(el.TxIndex),
							Events: make([]*client.Event, 0),
							Height: int64(el.BlockNumber),
						}
						rpsMap[el.TxIndex] = rp
					}
					rp.Events = append(rp.Events, evt)
				}
				if len(rpsMap) > 0 {
					rps := make([]*client.ReceiptProof, 0)
					for _, rp := range rpsMap {
						rps = append(rps, rp)
					}
					sort.Slice(rps, func(i int, j int) bool {
						return rps[i].Index < rps[j].Index
					})
					e.seq = rps[len(rps)-1].Events[len(rps[len(rps)-1].Events)-1].Sequence.Int64()
					rs, err := newReceiveStatus(v.Height.Int64(), e.seq, rps)
					if err != nil {
						return err
					}

					e.rss = append(e.rss, rs)
					e.l.Debugf("monitor info : Height:%d  EventCnt:%d LastSeq:%d ",
						v.Height.Int64(), len(rps[len(rps)-1].Events), e.seq)

					e.rsc <- rs
				}
			}

			return nil
		},
	)
}

func (e *ethbr) newBlockUpdate(v *client.BlockNotification) (*client.BlockUpdate, error) {
	var err error

	bu := &client.BlockUpdate{
		BlockHash: v.Hash.Bytes(),
		Height:    v.Height.Int64(),
	}

	header := client.MakeHeader(v.Header)
	bu.Header, err = codec.RLP.MarshalToBytes(*header)
	if err != nil {
		return nil, err
	}

	encodedHeader, _ := rlp.EncodeToBytes(v.Header)
	if !bytes.Equal(v.Header.Hash().Bytes(), crypto.Keccak256(encodedHeader)) {
		return nil, fmt.Errorf("mismatch block hash with BlockNotification")
	}

	update := &client.EvmBlockUpdate{}
	update.BlockHeader, _ = codec.RLP.MarshalToBytes(*header)
	//TODO get validators
	//update.Validators = header.Extra format marshal
	buf := new(bytes.Buffer)
	encodeSigHeader(buf, v.Header)
	update.EvmHeader = buf.Bytes()

	bu.Proof, err = codec.RLP.MarshalToBytes(update)
	if err != nil {
		return nil, err
	}

	return bu, nil
}

func (e *ethbr) GetReceiveHeightForSequence(seq int64) *receiveStatus {
	for _, rs := range e.rss {
		if seq <= rs.Seq() && seq >= rs.Seq() {
			return rs
		}
	}
	return nil
}

func (e *ethbr) GetReceiveHeightForHeight(height int64) *receiveStatus {
	for _, rs := range e.rss {
		if rs.Height() == height {
			return rs
		}
	}
	return nil
}

func encodeSigHeader(w io.Writer, header *types.Header) {
	err := rlp.Encode(w, []interface{}{
		big.NewInt(97),
		header.ParentHash,
		header.UncleHash,
		header.Coinbase,
		header.Root,
		header.TxHash,
		header.ReceiptHash,
		header.Bloom,
		header.Difficulty,
		header.Number,
		header.GasLimit,
		header.GasUsed,
		header.Time,
		header.Extra[:len(header.Extra)-65], // Yes, this will panic if extra is too short
		header.MixDigest,
		header.Nonce,
	})

	if err != nil {
		//panic("can't encode: " + err.Error())
	}
}

func logToEvent(el *types.Log) (*client.Event, error) {
	bm, err := binding.UnpackEventLog(el.Data)
	if err != nil {
		return nil, err
	}
	return &client.Event{
		Next:     btpTypes.BtpAddress(bm.Next),
		Sequence: bm.Seq,
		Message:  bm.Msg,
	}, nil
}

func sizeOfEvent(rp *client.Event) int {
	return int(unsafe.Sizeof(rp))
}
