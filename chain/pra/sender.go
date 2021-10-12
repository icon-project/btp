package pra

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"regexp"
	"runtime"
	"strings"
	"time"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                    = 524288 //512 * 1024 // 512kB
	txOverheadScale                  = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                      = txMaxDataSize / (1 + txOverheadScale)
	MaxBlockUpdatesPerSegment        = 3
	DefaultRetryContractCallInterval = 3 * time.Second
	defaultGasLimit                  = 10000000 // estimation for 3 blocks MaxBlockUpdatesPerSegment
)

var RetrableRelayReSendReExp = regexp.MustCompile(``)

type praSenderOptions struct {
	GasLimit uint64 `json:"gasLimit"`
}

var (
	DefaultRetryContractCall = 10 // reduce testing time
)

func init() {
	runtime.GOMAXPROCS(runtime.NumCPU())
}

type Sender struct {
	c   *Client
	w   Wallet
	src chain.BtpAddress
	dst chain.BtpAddress
	opt praSenderOptions
	log log.Logger
}

func (s *Sender) newTransactionParam(prev string, rm *RelayMessage) (*RelayMessageParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}

	rmp := &RelayMessageParam{
		Prev: prev,
		Msg:  base64.URLEncoding.EncodeToString(b),
	}

	s.log.Tracef("newTransactionParam RLPEncodedRelayMessage: %x\n", b)
	s.log.Tracef("newTransactionParam Base64EncodedRLPEncodedRelayMessage: %s\n", rmp.Msg)

	return rmp, nil
}

// Segment split the give RelayMessage into small segments
func (s *Sender) Segment(rm *chain.RelayMessage, height int64) ([]*chain.Segment, error) {
	s.log.Tracef("Segment: height %d %s", height, rm.BuRange())

	segments := make([]*chain.Segment, 0)
	var err error
	msg := &RelayMessage{
		BlockUpdates:  make([][]byte, 0),
		ReceiptProofs: make([][]byte, 0),
	}
	size := 0
	// When rm.BlockUpdates[len(rm.BlockUpdates)-1].Height <= s.bmcStatus.Verifier.Height
	// using only rm.BlockProof
	lastBlockIndex := len(rm.BlockUpdates) - 1
	if rm.BlockUpdates[lastBlockIndex].Height > height {
		for i, bu := range rm.BlockUpdates {
			if bu.Height <= height {
				continue
			}
			buSize := len(bu.Proof)
			if s.isOverSizeLimit(buSize) {
				return nil, ErrInvalidBlockUpdateProofSize
			}
			size += buSize

			// BlockUpdates should not empty in case the last bu.Height > Verifier.Height
			if (s.isOverSizeLimit(size) || s.isOverBlocksLimit(msg.numberOfBlockUpdate)) && i < lastBlockIndex {
				s.log.Tracef("Segment parachain blockupdates")
				segment := &chain.Segment{
					Height:              msg.height,
					NumberOfBlockUpdate: msg.numberOfBlockUpdate,
				}
				if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
					return nil, err
				}
				segments = append(segments, segment)
				msg = &RelayMessage{
					BlockUpdates:  make([][]byte, 0),
					ReceiptProofs: make([][]byte, 0),
				}
				size = buSize
			}

			s.log.Tracef("Segment: at %d BlockUpdates[%d]: %x", bu.Height, i, bu.Proof)
			msg.BlockUpdates = append(msg.BlockUpdates, bu.Proof)
			msg.height = bu.Height
			msg.numberOfBlockUpdate += 1
		}
	}

	bp, err := codec.RLP.MarshalToBytes(rm.BlockProof)
	if err != nil {
		return nil, err
	}

	if s.isOverSizeLimit(len(bp)) {
		return nil, ErrInvalidBlockUpdateProofSize
	}

	for i, rp := range rm.ReceiptProofs {
		if s.isOverSizeLimit(len(rp.Proof)) {
			return nil, ErrInvalidReceiptProofSize
		}
		if len(msg.BlockUpdates) == 0 {
			if rm.BlockProof == nil {
				return nil, fmt.Errorf("BlockProof must not be nil")
			}

			size += len(bp)
			msg.BlockProof = bp
			msg.height = rm.BlockProof.BlockWitness.Height
			s.log.Tracef("Segment: at %d BlockProof: %x", msg.height, msg.BlockProof)
		}

		size += len(rp.Proof)
		trp := &ReceiptProof{
			Index:       rp.Index,
			Proof:       rp.Proof,
			EventProofs: make([]*chain.EventProof, 0),
		}

		for j, ep := range rp.EventProofs {
			if s.isOverSizeLimit(len(ep.Proof)) {
				return nil, ErrInvalidEventProofProofSize
			}

			size += len(ep.Proof)
			if s.isOverSizeLimit(size) {
				if i == 0 && j == 0 && len(msg.BlockUpdates) == 0 {
					return nil, fmt.Errorf("BlockProof + ReceiptProof + EventProof > limit %v", i)
				}

				segment := &chain.Segment{
					Height:              msg.height,
					NumberOfBlockUpdate: msg.numberOfBlockUpdate,
					EventSequence:       msg.eventSequence,
					NumberOfEvent:       msg.numberOfEvent,
				}
				if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
					return nil, err
				}
				segments = append(segments, segment)

				msg = &RelayMessage{
					BlockUpdates:  make([][]byte, 0),
					BlockProof:    bp,
					ReceiptProofs: make([][]byte, 0),
				}

				trp = &ReceiptProof{
					Index:       rp.Index,
					Proof:       rp.Proof,
					EventProofs: make([]*chain.EventProof, 0),
				}

				size = len(ep.Proof)
				size += len(rp.Proof)
				size += len(bp)
			}

			trp.EventProofs = append(trp.EventProofs, ep)
			msg.eventSequence = rp.Events[j].Sequence
			msg.numberOfEvent += 1
		}

		if b, err := codec.RLP.MarshalToBytes(trp); err != nil {
			return nil, err
		} else {
			s.log.Tracef("Segment: at %d ReceiptProofs[%d]: %x", rp.Height, i, b)
			msg.ReceiptProofs = append(msg.ReceiptProofs, b)
		}
	}

	if len(msg.BlockUpdates) > 0 || len(msg.BlockProof) > 0 {
		segment := &chain.Segment{
			Height:              msg.height,
			NumberOfBlockUpdate: msg.numberOfBlockUpdate,
			EventSequence:       msg.eventSequence,
			NumberOfEvent:       msg.numberOfEvent,
		}
		if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
			return nil, err
		}
		segments = append(segments, segment)
	}
	return segments, nil
}

// UpdateSegment updates segment
func (s *Sender) UpdateSegment(bp *chain.BlockProof, segment *chain.Segment) error {
	p := segment.TransactionParam.(*RelayMessageParam)
	msg := &RelayMessage{}
	b, err := base64.URLEncoding.DecodeString(p.Msg)
	if err != nil {
		return err
	}

	if _, err = codec.RLP.UnmarshalFromBytes(b, msg); err != nil {
		return err
	}
	if msg.BlockProof, err = codec.RLP.MarshalToBytes(bp); err != nil {
		return err
	}
	segment.TransactionParam, err = s.newTransactionParam(p.Prev, msg)
	return err
}

func (s *Sender) Relay(segment *chain.Segment) (chain.GetResultParam, error) {
	p, ok := segment.TransactionParam.(*RelayMessageParam)
	if !ok {
		return nil, fmt.Errorf("casting failure")
	}

	txOpts := s.c.newTransactOpts(s.w)
	if s.opt.GasLimit > 0 {
		txOpts.GasLimit = s.opt.GasLimit
	} else {
		txOpts.GasLimit = defaultGasLimit
	}

	s.log.Tracef("Relay: TransactionOptions: %+v", txOpts)

	tries := 0
CALL_CONTRACT:
	tries++
	tx, err := s.c.bmc.HandleRelayMessage(txOpts, p.Prev, p.Msg)
	if err != nil {
		if RetrableRelayReSendReExp.MatchString(err.Error()) && tries < DefaultRetryContractCall {
			s.log.Tracef("Relay: retry with Relay err:%+v", err)
			<-time.After(DefaultRetryContractCallInterval)
			goto CALL_CONTRACT
		}
		return nil, err
	}

	return &TransactionHashParam{
		From:  txOpts.From,
		Tx:    tx,
		Param: p,
	}, nil
}

// GetResult gets the TransactionReceipt
func (s *Sender) GetResult(p chain.GetResultParam) (chain.TransactionResult, error) {
	//TODO: map right Error with the result getting from the transaction receipt

	if thp, ok := p.(*TransactionHashParam); ok {
		t := time.Now()
		s.log.Debugf("getting receipt:%s", thp.Hash())

		for {
			txr, err := s.c.GetTransactionReceipt(thp.Hash())
			if err != nil {
				<-time.After(DefaultRetryContractCallInterval)
				continue
			}

			if txr.Status == 0 {
				//TODO: handle mapError here
				s.log.Errorf("failed to send message on %v with params _prev: %v _msg: %v", thp.Hash(), thp.Param.Prev, thp.Param.Msg)
				return nil, s.parseTransactionError(thp.From, thp.Tx, txr.BlockNumber)
			}

			s.log.Debugf("got receipt:%v taken:%.2f seconds", txr.TxHash.String(), time.Now().Sub(t).Seconds())
			return txr.TxHash.Hex(), nil
		}
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (s *Sender) GetStatus() (*chain.BMCLinkStatus, error) {

	tries := 0
CALL_CONTRACT:
	tries++
	bs, err := s.c.bmc.GetStatus(nil, s.src.String())
	if err != nil {
		if tries < DefaultRetryContractCall {
			s.log.Tracef("GetStatus: retry with GetStatus err:%+v", err)
			<-time.After(DefaultRetryContractCallInterval)
			goto CALL_CONTRACT
		}
		return nil, fmt.Errorf("fail to get Parachain' status, err: %v", err)
	}

	status := &chain.BMCLinkStatus{
		BlockIntervalSrc: int(bs.BlockIntervalSrc.Int64()),
		BlockIntervalDst: int(bs.BlockIntervalDst.Int64()),
		TxSeq:            bs.TxSeq.Int64(),
		RxSeq:            bs.RxSeq.Int64(),
		Verifier: struct {
			Height     int64
			Offset     int64
			LastHeight int64
		}{
			Height:     bs.Verifier.HeightMTA.Int64(),
			Offset:     bs.Verifier.OffsetMTA.Int64(),
			LastHeight: bs.Verifier.LastHeight.Int64(),
		},
		RotateHeight:   bs.RotateHeight.Int64(),
		RotateTerm:     int(bs.RotateTerm.Int64()),
		DelayLimit:     int(bs.DelayLimit.Int64()),
		MaxAggregation: int(bs.MaxAggregation.Int64()),
		RxHeight:       bs.RxHeight.Int64(),
		RxHeightSrc:    bs.RxHeightSrc.Int64(),
		CurrentHeight:  bs.CurrentHeight.Int64(),
		BMRIndex:       int(bs.RelayIdx.Int64()),
		BMRs: make([]struct {
			Address      string
			BlockCount   int64
			MessageCount int64
		}, len(bs.Relays)),
	}
	for i, bmr := range bs.Relays {
		status.BMRs[i].Address = string(bmr.Addr.Hex())
		status.BMRs[i].BlockCount = bmr.BlockCount.Int64()
		status.BMRs[i].MessageCount = bmr.MsgCount.Int64()
	}

	return status, nil
}

func (s *Sender) MonitorLoop(height int64, cb chain.MonitorCallback, scb func()) error {
	s.log.Debugf("MonitorLoop from height: %v", height)

	if err := s.c.MonitorBlock(uint64(height), false, func(v *BlockNotification) error {
		cb(int64(v.Height))
		return nil
	}); err != nil {
		return fmt.Errorf("MonitorLoop parachain, got err: %v", err)
	}
	return nil
}

func (s *Sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}

func (s *Sender) FinalizeLatency() int {
	//on-the-next
	return 1
}

func NewSender(src, dst chain.BtpAddress, w Wallet, endpoint string, opt map[string]interface{}, l log.Logger) chain.Sender {
	s := &Sender{
		src: src,
		dst: dst,
		w:   w,
		log: l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	s.c = NewClient(endpoint, dst.ContractAddress(), l)

	return s
}

func (s *Sender) isOverSizeLimit(size int) bool {
	return txSizeLimit < float64(size)
}

func (s *Sender) isOverBlocksLimit(blockupdates int) bool {
	return blockupdates >= MaxBlockUpdatesPerSegment
}

func (s *Sender) parseTransactionError(from EvmAddress, tx *EvmTransaction, blockNumber *big.Int) error {
	_, txerr := s.c.CallContract(EvmCallMsg{
		From:     from,
		To:       tx.To(),
		Gas:      tx.Gas(),
		GasPrice: tx.GasPrice(),
		Value:    tx.Value(),
		Data:     tx.Data(),
	}, blockNumber)
	if txerr == nil {
		return errors.New("parseTransactionError: empty")
	}

	if dataerr, ok := txerr.(EvmDataError); ok {
		rawerr, derr := decodeEvmError(dataerr)
		if derr != nil {
			s.log.Error(derr)
			return txerr
		}

		return mapError(rawerr)
	}

	return txerr
}

func decodeEvmError(dataerr EvmDataError) (string, error) {
	i := dataerr.ErrorData()
	if i == nil {
		return "", errors.New("decodeEvmError: ErrorData is empty")
	}

	s := dataerr.ErrorData().(string)
	if len(s) < 136 {
		return "", fmt.Errorf("decodeEvmError: unknow error")
	}
	s = s[136:]
	s = strings.TrimRight(s, "0")
	d, err := hex.DecodeString(s)
	if err != nil {
		return "", fmt.Errorf("decodeEvmError: %v", err.Error())
	}

	return strings.Split(string(d), ":")[0], nil
}

func mapError(s string) error {
	for code, name := range chain.BMCRevertCodeNames {
		if name == s {
			return chain.NewRevertError(int(code))
		}
	}

	for code, name := range chain.BMVRevertCodeNames {
		if name == s {
			return chain.NewRevertError(int(code))
		}
	}
	return errors.New(s)
}
