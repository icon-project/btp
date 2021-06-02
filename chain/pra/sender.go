package pra

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"time"

	"github.com/ethereum/go-ethereum/core/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                   = txMaxDataSize / (1 + txOverheadScale)
	DefaultGetRelayResultInterval = time.Second
	DefaultRelayReSendInterval    = time.Second
	gasLimit                      = 6000000
)

type Sender struct {
	c   *Client
	w   Wallet
	src chain.BtpAddress
	dst chain.BtpAddress
	log log.Logger

	opt struct {
		StepLimit int64
	}

	evtLogRawFilter struct {
		addr      []byte
		signature []byte
		next      []byte
		seq       []byte
	}
	isFoundOffsetBySeq bool
	cb                 chain.ReceiveCallback
}

func (s *Sender) newTransactionParam(prev string, rm *RelayMessage) (*HandleRelayMessageParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}

	return &HandleRelayMessageParam{
		Prev: prev,
		Msg:  base64.URLEncoding.EncodeToString(b),
	}, nil
}

// Segment split the give RelayMessage into small segments
func (s *Sender) Segment(rm *chain.RelayMessage, height int64) ([]*chain.Segment, error) {
	segments := make([]*chain.Segment, 0)
	var err error
	msg := &RelayMessage{
		BlockUpdates:  make([][]byte, 0),
		ReceiptProofs: make([][]byte, 0),
	}
	size := 0
	//TODO rm.BlockUpdates[len(rm.BlockUpdates)-1].Height <= s.bmcStatus.Verifier.Height
	//	using only rm.BlockProof
	for _, bu := range rm.BlockUpdates {
		if bu.Height <= height {
			continue
		}
		buSize := len(bu.Proof)
		if s.isOverLimit(buSize) {
			return nil, fmt.Errorf("invalid BlockUpdate.Proof size")
		}
		size += buSize
		if s.isOverLimit(size) {
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
		msg.BlockUpdates = append(msg.BlockUpdates, bu.Proof)
		msg.height = bu.Height
		msg.numberOfBlockUpdate += 1
	}

	var bp []byte
	if bp, err = codec.RLP.MarshalToBytes(rm.BlockProof); err != nil {
		return nil, err
	}
	if s.isOverLimit(len(bp)) {
		return nil, fmt.Errorf("invalid BlockProof size")
	}

	for _, rp := range rm.ReceiptProofs {
		if s.isOverLimit(len(rp.Proof)) {
			return nil, fmt.Errorf("invalid ReceiptProof.Proof size")
		}
		if len(msg.BlockUpdates) == 0 {
			size += len(bp)
			msg.BlockProof = bp
			msg.height = rm.BlockProof.BlockWitness.Height
		}
		size += len(rp.Proof)

		for j, ep := range rp.EventProofs {
			if s.isOverLimit(len(ep.Proof)) {
				return nil, fmt.Errorf("invalid EventProof.Proof size")
			}
			size += len(ep.Proof)
			if s.isOverLimit(size) {
				if j == 0 && len(msg.BlockUpdates) == 0 {
					return nil, fmt.Errorf("BlockProof + ReceiptProof + EventProof > limit")
				}
				//
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
					ReceiptProofs: make([][]byte, 0),
					BlockProof:    bp,
				}
				size = len(ep.Proof)
				size += len(rp.Proof)
				size += len(bp)

			}
			msg.eventSequence = rp.Events[j].Sequence
			msg.numberOfEvent += 1
		}

	}
	//
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
	return segments, nil
}

// UpdateSegment updates segment
func (s *Sender) UpdateSegment(bp *chain.BlockProof, segment *chain.Segment) error {
	p := segment.TransactionParam.(*HandleRelayMessageParam)
	msg := &RelayMessage{}
	b, err := base64.URLEncoding.DecodeString(p.Msg)
	if _, err = codec.RLP.UnmarshalFromBytes(b, msg); err != nil {
		return err
	}
	if msg.BlockProof, err = codec.RLP.MarshalToBytes(bp); err != nil {
		return err
	}
	segment.TransactionParam, err = s.newTransactionParam(p.Prev, msg)
	return nil
}

func (s *Sender) Relay(segment *chain.Segment) (chain.GetResultParam, error) {
	p, ok := segment.TransactionParam.(*HandleRelayMessageParam)
	if !ok {
		return nil, fmt.Errorf("casting failure")
	}

	tries := 0
SEND_TX:
	tries++
	opts := s.c.newTransactOpts(s.w)
	opts.GasLimit = gasLimit

	txh, err := s.c.bmc.HandleRelayMessage(opts, p.Prev, p.Msg)
	if err != nil {
		if tries < 3 {
			s.log.Debug("retry sending transaction")
			<-time.After(time.Second * 3)
			goto SEND_TX
		}
		return nil, mapError(err)
	}

	s.log.Debugf("Got new relayed TX %s", txh.Hash().Hex())
	return txh, nil
}

// GetResult gets the TransactionReceipt
func (s *Sender) GetResult(p chain.GetResultParam) (chain.TransactionResult, error) {
	//TODO: map right Error with the result getting from the transaction receipt

	if txh, ok := p.(*types.Transaction); ok {
		for {
			s.log.Debugf("Get receipt %s", txh.Hash().Hex())
			txr, err := s.c.GetTransactionReceipt(txh.Hash())
			if err != nil {
				<-time.After(DefaultGetRelayResultInterval)
				continue
			}

			s.log.Error("Got receipt %s %v", txr.TxHash.String())
			return txr, err
		}
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (s *Sender) GetStatus() (*chain.BMCLinkStatus, error) {

	bs, err := s.c.bmc.GetStatus(nil, s.src.String())
	if err != nil {
		return nil, err
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

	status.Verifier.LastHeight = 1 // TODO: Change this when using with real contract
	return status, nil
}

func (s *Sender) MonitorLoop(height int64, cb chain.MonitorCallback, scb func()) error {
	return s.c.MonitorEvmBlock(cb)
}

func (s *Sender) StopMonitorLoop() {
	return
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

func mapError(err error) error {
	if err != nil {
		switch re := err.(type) {
		case *jsonrpc.Error:
			//fmt.Printf("jrResp.Error:%+v", re)
			switch re.Code {
			case JsonrpcErrorCodeTxPoolOverflow:
				return chain.ErrSendFailByOverflow
			case JsonrpcErrorCodeSystem:
				if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
					//TODO return JsonRPC Error
					switch subEc {
					case ExpiredTransactionError:
						return chain.ErrSendFailByExpired
					case FutureTransactionError:
						return chain.ErrSendFailByFuture
					case TransactionPoolOverflowError:
						return chain.ErrSendFailByOverflow
					}
				}
			case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
				return chain.ErrGetResultFailByPending
			}
		case *common.HttpError:
			fmt.Printf("*common.HttpError:%+v", re)
			return chain.ErrConnectFail
		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				//fmt.Printf("*url.Error:%+v", re)
				return chain.ErrConnectFail
			}
		}
	}
	return err
}

func (s *Sender) isOverLimit(size int) bool {
	return txSizeLimit < float64(size)
}
