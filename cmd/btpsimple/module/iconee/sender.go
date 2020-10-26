package iconee

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"time"

	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize       = 524288 //512 * 1024 // 512kB
	DefaultGetRelayResultInterval   = time.Second
	DefaultRelayReSendInterval      = time.Second
)

type sender struct {
	c   *Client
	src module.BtpAddress
	dst module.BtpAddress
	w   Wallet
	l   log.Logger
	opt struct {
		StepLimit int64
	}

	evtLogRawFilter struct {
		addr      []byte
		signature []byte
		next      []byte
		seq       []byte
	}
	evtReq             *BlockRequest
	bh                 *BlockHeader
	isFoundOffsetBySeq bool
	cb                 module.ReceiveCallback
}

func (s *sender) Relay(rm *module.RelayMessage) (module.GetResultParam, error) {
	msg := RelayMessage{
		BlockUpdates:  make([][]byte, len(rm.BlockUpdates)),
		ReceiptProofs: make([][]byte, len(rm.ReceiptProofs)),
	}
	for i, bu := range rm.BlockUpdates {
		msg.BlockUpdates[i] = bu.Proof
	}
	var err error
	if rm.BlockProof != nil {
		if msg.BlockProof, err = codec.RLP.MarshalToBytes(rm.BlockProof); err != nil {
			return nil, err
		}
	}
	for i, rp := range rm.ReceiptProofs {
		if msg.ReceiptProofs[i], err = codec.RLP.MarshalToBytes(rp); err != nil {
			return nil, err
		}
	}
	b, err := codec.RLP.MarshalToBytes(msg)
	if err != nil {
		return nil, err
	}
	s.l.Tracef("RelayMessage size %d", len(b))
	rmp := BMCRelayMethodParams{
		Prev:     rm.From.String(),
		Messages: base64.URLEncoding.EncodeToString(b),
	}
	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.ContractAddress()),
		NetworkID:   HexInt(s.dst.NetworkID()),
		StepLimit:   NewHexInt(s.opt.StepLimit),
		DataType:    "call",
		Data: CallData{
			Method: BMCRelayMethod,
			Params: rmp,
		},
	}
	thp := &TransactionHashParam{}
SignLoop:
	for {
		if err := s.c.SignTransaction(s.w, p); err != nil {
			return nil, err
		}
	SendLoop:
		for {
			txh, err := s.c.SendTransaction(p)
			if txh != nil {
				thp.Hash = *txh
			}
			if err != nil {
				if je, ok := err.(*jsonrpc.Error); ok {
					switch je.Code {
					case JsonrpcErrorCodeTxPoolOverflow:
						<-time.After(DefaultRelayReSendInterval)
						continue SendLoop
					case JsonrpcErrorCodeSystem:
						if subEc, err := strconv.ParseInt(je.Message[1:5], 0, 32); err == nil {
							switch subEc {
							case DuplicateTransactionError:
								s.l.Debugf("DuplicateTransactionError txh:%v", txh)
								return thp, nil
							case ExpiredTransactionError:
								continue SignLoop
							}
						}
					}
				}
				return nil, mapError(err)
			}
			return thp, nil
		}
	}
}

func (s *sender) GetResult(p module.GetResultParam) (module.TransactionResult, error) {
	if txh, ok := p.(*TransactionHashParam); ok {
		for {
			txr, err := s.c.GetTransactionResult(txh)
			if err != nil {
				if je, ok := err.(*jsonrpc.Error); ok {
					switch je.Code {
					case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
						<-time.After(DefaultGetRelayResultInterval)
						continue
					}
				}
			}
			return txr, mapErrorWithTransactionResult(txr, err)
		}
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (s *sender) GetStatus() (*module.BMCLinkStatus, error) {
	p := &CallParam{
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.ContractAddress()),
		DataType:    "call",
		Data: CallData{
			Method: BMCGetStatusMethod,
			Params: BMCStatusParams{
				Target: s.src.String(),
			},
		},
	}
	bs := &BMCStatus{}
	err := mapError(s.c.Call(p, bs))
	if err != nil {
		return nil, err
	}
	ls := &module.BMCLinkStatus{}
	ls.TxSeq, err = bs.TxSeq.Value()
	ls.RxSeq, err = bs.RxSeq.Value()
	ls.Verifier.Height, err = bs.Verifier.Height.Value()
	ls.Verifier.Offset, err = bs.Verifier.Offset.Value()
	ls.Verifier.LastHeight, err = bs.Verifier.LastHeight.Value()
	return ls, nil
}

func (s *sender) LimitOfTransactionSize() int {
	return txMaxDataSize
}

func NewSender(src, dst module.BtpAddress, w Wallet, endpoint string, opt map[string]interface{}, l log.Logger) module.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w,
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	s.c = NewClient(endpoint, l)
	return s
}


func mapError(err error) error {
	if err != nil {
		switch re := err.(type) {
		case *jsonrpc.Error:
			//fmt.Printf("jrResp.Error:%+v", re)
			switch re.Code {
			case JsonrpcErrorCodeTxPoolOverflow:
				return module.ErrSendFailByOverflow
			case JsonrpcErrorCodeSystem:
				if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
					//TODO return JsonRPC Error
					switch subEc {
					case ExpiredTransactionError:
						return module.ErrSendFailByExpired
					case FutureTransactionError:
						return module.ErrSendFailByFuture
					case TransactionPoolOverflowError:
						return module.ErrSendFailByOverflow
					}
				}
			case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
				return module.ErrGetResultFailByPending
			}
		case *common.HttpError:
			return module.ErrConnectFail
		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				//fmt.Printf("*url.Error:%+v", re)
				return module.ErrConnectFail
			}
		}
	}
	return err
}

func mapErrorWithTransactionResult(txr *TransactionResult, err error) error {
	err = mapError(err)
	if err == nil && txr != nil && txr.Status != ResultStatusSuccess {
		fc, _ := txr.Failure.CodeValue.Value()
		if fc < ResultStatusFailureCodeRevert || fc > ResultStatusFailureCodeEnd {
			err = fmt.Errorf("failure with code:%s, message:%s",
				txr.Failure.CodeValue, txr.Failure.MessageValue)
		} else {
			err = module.NewRevertError(int(fc - ResultStatusFailureCodeRevert))
		}
	}
	return err
}
