/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icon

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math"
	"math/big"
	"net/url"
	"strconv"
	"time"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	DefaultGetRelayResultInterval = time.Second
	DefaultRelayReSendInterval    = time.Second
	DefaultStepLimit              = 0x9502f900 //maxStepLimit(invoke), refer https://www.icondev.io/docs/step-estimation
)

var (
	txSizeLimit = int(math.Ceil(txMaxDataSize / (1 + txOverheadScale)))
)

type sender struct {
	c   *Client
	src base.BtpAddress
	dst base.BtpAddress
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
	cb                 base.ReceiveCallback
}

func (s *sender) newTransactionParam(prev string, rm *RelayMessage) (*TransactionParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}
	rmp := &BMCRelayMethodParams{
		Prev:     prev,
		Messages: base64.URLEncoding.EncodeToString(b),
	}
	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.Account()),
		NetworkID:   HexInt(s.dst.NetworkID()),
		StepLimit:   NewHexInt(s.opt.StepLimit),
		DataType:    "call",
		Data: &CallData{
			Method: BMCRelayMethod,
			Params: rmp,
		},
	}
	return p, nil
}

func (s *sender) Segment(rm *base.RelayMessage, height int64) ([]*base.Segment, error) {
	segments := make([]*base.Segment, 0)
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
			segment := &base.Segment{
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

	if len(rm.ReceiptProofs) > 0 && rm.BlockProof == nil {
		s.l.Panicf("rm.BlockProof is nil, rm:%d h:%d", rm.Seq, rm.HeightOfSrc)
	}
	var bp []byte
	if bp, err = codec.RLP.MarshalToBytes(rm.BlockProof); err != nil {
		return nil, err
	}
	if s.isOverLimit(len(bp)) {
		return nil, fmt.Errorf("invalid BlockProof size")
	}

	var b []byte
	for _, rp := range rm.ReceiptProofs {
		if s.isOverLimit(len(rp.Proof)) {
			return nil, fmt.Errorf("invalid ReceiptProof.Proof size")
		}
		if len(msg.BlockUpdates) == 0 {
			size += len(bp)
			msg.BlockProof = bp
			msg.height = rm.HeightOfSrc
		}
		size += len(rp.Proof)
		trp := &ReceiptProof{
			Index:       rp.Index,
			Proof:       rp.Proof,
			EventProofs: make([]*base.EventProof, 0),
		}
		for j, ep := range rp.EventProofs {
			//FIXME
			//if s.isOverLimit(len(ep.Proof)) {
			//	return nil, fmt.Errorf("invalid EventProof.Proof size")
			//}
			size += len(ep.Proof)
			if s.isOverLimit(size) {
				//FIXME
				//if j == 0 && len(msg.BlockUpdates) == 0 {
				//	return nil, fmt.Errorf("BlockProof + ReceiptProof + EventProof > limit")
				//}
				//
				segment := &base.Segment{
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
					height: rm.HeightOfSrc,
				}
				size = len(ep.Proof)
				size += len(rp.Proof)
				size += len(bp)

				trp = &ReceiptProof{
					Index:       rp.Index,
					Proof:       rp.Proof,
					EventProofs: make([]*base.EventProof, 0),
				}
			}
			trp.EventProofs = append(trp.EventProofs, ep)
			msg.eventSequence = rp.Events[j].Sequence
			msg.numberOfEvent += 1
		}

		if b, err = codec.RLP.MarshalToBytes(trp); err != nil {
			return nil, err
		}
		msg.ReceiptProofs = append(msg.ReceiptProofs, b)
	}
	//
	segment := &base.Segment{
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

func (s *sender) UpdateSegment(bp *base.BlockProof, segment *base.Segment) error {
	p := segment.TransactionParam.(*TransactionParam)
	rmp := p.Data.(*CallData).Params.(*BMCRelayMethodParams)
	msg := &RelayMessage{}
	b, err := base64.URLEncoding.DecodeString(rmp.Messages)
	if _, err = codec.RLP.UnmarshalFromBytes(b, msg); err != nil {
		return err
	}
	if msg.BlockProof, err = codec.RLP.MarshalToBytes(bp); err != nil {
		return err
	}
	segment.TransactionParam, err = s.newTransactionParam(rmp.Prev, msg)
	return err
}

func (s *sender) sendFragment(rmp *BMCRelayMethodParams, idx int) (base.GetResultParam, error) {
	msgLen := txSizeLimit
	if len(rmp.Messages) < msgLen {
		msgLen = len(rmp.Messages)
	}
	fmp := &BMCFragmentMethodParams{
		Prev: rmp.Prev, Messages: rmp.Messages[:msgLen], Index: NewHexInt(int64(idx))}
	rmp.Messages = rmp.Messages[msgLen:]
	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.Account()),
		NetworkID:   HexInt(s.dst.NetworkID()),
		StepLimit:   NewHexInt(s.opt.StepLimit),
		DataType:    "call",
		Data: &CallData{
			Method: BMCFragmentMethod,
			Params: fmp,
		},
	}
	return s.sendTransaction(p)
}

func (s *sender) Relay(segment *base.Segment) (base.GetResultParam, error) {
	p := segment.TransactionParam.(*TransactionParam)
	rmp := p.Data.(*CallData).Params.(*BMCRelayMethodParams)

	idx := len(rmp.Messages) / txSizeLimit
	if idx == 0 {
		return s.sendTransaction(p)
	} else {
		ret, err := s.sendFragment(rmp, idx*-1)
		if err != nil {
			return nil, err
		}
		for idx--; idx >= 0; idx-- {
			if ret, err = s.sendFragment(rmp, idx); err != nil {
				return ret, err
			}
		}
		return ret, err
	}
}

func (s *sender) sendTransaction(p *TransactionParam) (base.GetResultParam, error) {
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

func (s *sender) GetResult(p base.GetResultParam) (base.TransactionResult, error) {
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
		return nil, fmt.Errorf("fail to cast *TransactionHashParam %T", p)
	}
}

func (s *sender) GetStatus() (*base.BMCLinkStatus, error) {
	p := &CallParam{
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.Account()),
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
	ls := &base.BMCLinkStatus{}
	if ls.TxSeq, err = bs.TxSeq.BigInt(); err != nil {
		return nil, err
	}
	if ls.RxSeq, err = bs.RxSeq.BigInt(); err != nil {
		return nil, err
	}
	if ls.Verifier.Height, err = bs.Verifier.Height.Value(); err != nil {
		return nil, err
	}
	if ls.Verifier.Offset, err = bs.Verifier.Offset.Value(); err != nil {
		return nil, err
	}
	if ls.Verifier.LastHeight, err = bs.Verifier.LastHeight.Value(); err != nil {
		return nil, err
	}
	ls.BMRs = make([]struct {
		Address      string
		BlockCount   int64
		MessageCount *big.Int
	}, len(bs.BMRs))
	for i, bmr := range bs.BMRs {
		ls.BMRs[i].Address = string(bmr.Address)
		if ls.BMRs[i].BlockCount, err = bmr.BlockCount.Value(); err != nil {
			return nil, err
		}
		if ls.BMRs[i].MessageCount, err = bmr.MessageCount.BigInt(); err != nil {
			return nil, err
		}
	}
	if ls.BMRIndex, err = bs.BMRIndex.Int(); err != nil {
		return nil, err
	}
	if ls.RotateHeight, err = bs.RotateHeight.Value(); err != nil {
		return nil, err
	}
	if ls.RotateTerm, err = bs.RotateTerm.Int(); err != nil {
		return nil, err
	}
	if ls.DelayLimit, err = bs.DelayLimit.Int(); err != nil {
		return nil, err
	}
	if ls.MaxAggregation, err = bs.MaxAggregation.Int(); err != nil {
		return nil, err
	}
	if ls.CurrentHeight, err = bs.CurrentHeight.Value(); err != nil {
		return nil, err
	}
	if ls.RxHeight, err = bs.RxHeight.Value(); err != nil {
		return nil, err
	}
	if ls.RxHeightSrc, err = bs.RxHeightSrc.Value(); err != nil {
		return nil, err
	}
	return ls, nil
}

func (s *sender) isOverLimit(size int) bool {
	return txSizeLimit < size
}

func (s *sender) MonitorLoop(height int64, cb base.MonitorCallback, scb func()) error {
	br := &BlockRequest{
		Height: NewHexInt(height),
	}
	return s.c.MonitorBlock(br,
		func(conn *websocket.Conn, v *BlockNotification) error {
			if h, err := v.Height.Value(); err != nil {
				return err
			} else {
				return cb(h)
			}
		},
		func(conn *websocket.Conn) {
			s.l.Debugf("MonitorLoop connected %s", conn.LocalAddr().String())
			if scb != nil {
				scb()
			}
		},
		func(conn *websocket.Conn, err error) {
			s.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
			_ = conn.Close()
		})
}

func (s *sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}
func (s *sender) FinalizeLatency() int {
	//on-the-next
	return 1
}

func NewSender(src, dst base.BtpAddress, w Wallet, endpoint string, opt map[string]interface{}, l log.Logger) base.Sender {
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
	if s.opt.StepLimit <= 0 {
		s.opt.StepLimit = DefaultStepLimit
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
				return base.ErrSendFailByOverflow
			case JsonrpcErrorCodeSystem:
				if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
					//TODO return JsonRPC Error
					switch subEc {
					case ExpiredTransactionError:
						return base.ErrSendFailByExpired
					case FutureTransactionError:
						return base.ErrSendFailByFuture
					case TransactionPoolOverflowError:
						return base.ErrSendFailByOverflow
					}
				}
			case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
				return base.ErrGetResultFailByPending
			}
		case *common.HttpError:
			fmt.Printf("*common.HttpError:%+v", re)
			return base.ErrConnectFail
		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				//fmt.Printf("*url.Error:%+v", re)
				return base.ErrConnectFail
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
			err = base.NewRevertError(int(fc - ResultStatusFailureCodeRevert))
		}
	}
	return err
}
