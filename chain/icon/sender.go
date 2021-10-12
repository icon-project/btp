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
	"net/url"
	"regexp"
	"strconv"
	"time"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                   = 524288 //512 * 1024 // 512kB
	txOverheadScale                 = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                     = txMaxDataSize / (1 + txOverheadScale)
	DefaultGetRelayResultInterval   = time.Second
	DefaultRelayReSendInterval      = time.Second * 3
	MaxDefaultGetRelayResultRetries = int((time.Minute * 5) / (DefaultGetRelayResultInterval)) // Pending or stale transaction timeout is 5 minute
	DefaultStepLimit                = 2500000000                                               // estimated step limit for 10 blockupdates per segment
)

var RetryHTTPError = regexp.MustCompile(`connection reset by peer|EOF`)

type SenderOptions struct {
	StepLimit int64 `json:"stepLimit"`
}

type sender struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	w   Wallet
	l   log.Logger
	opt SenderOptions
}

func (s *sender) newTransactionParam(prev string, rm *RelayMessage) (*TransactionParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}

	rmp := BMCRelayMethodParams{
		Prev:     prev,
		Messages: base64.URLEncoding.EncodeToString(b),
	}

	sl := NewHexInt(s.opt.StepLimit)
	if s.opt.StepLimit == 0 {
		sl = NewHexInt(DefaultStepLimit)
	}

	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.ContractAddress()),
		NetworkID:   HexInt(s.dst.NetworkID()),
		StepLimit:   sl,
		DataType:    "call",
		Data: CallData{
			Method: BMCRelayMethod,
			Params: rmp,
		},
	}

	// s.l.Tracef("newTransactionParam RLPEncodedRelayMessage: %x\n", b)
	// s.l.Tracef("newTransactionParam Base64EncodedRLPEncodedRelayMessage: %s\n", rmp.Messages)
	return p, nil
}

// TODO refactor with newTransactionParam
func (s *sender) newFragmentationsTransactionParam(prev string, msg string, idx int) (*TransactionParam, error) {
	sl := NewHexInt(s.opt.StepLimit)
	if s.opt.StepLimit == 0 {
		sl = NewHexInt(DefaultStepLimit)
	}

	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(s.w.Address()),
		ToAddress:   Address(s.dst.ContractAddress()),
		NetworkID:   HexInt(s.dst.NetworkID()),
		StepLimit:   sl,
		DataType:    "call",
		Data: CallData{
			Method: BMCFragmentMethod,
			Params: BMCFragmentMethodParams{
				Prev:     prev,
				Messages: msg,
				Index:    NewHexInt(int64(idx)),
			},
		},
	}
	return p, nil
}

func (s *sender) iconSegment(rm *chain.RelayMessage, height int64) ([]*chain.Segment, error) {
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
		if s.isOverSizeLimit(buSize) {
			return nil, ErrInvalidBlockUpdateProofSize
		}
		size += buSize
		if s.isOverSizeLimit(size) {
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

	if s.isOverSizeLimit(len(bp)) {
		return nil, fmt.Errorf("invalid BlockProof size")
	}

	for i, rp := range rm.ReceiptProofs {
		if s.isOverSizeLimit(len(rp.Proof)) {
			return nil, ErrInvalidReceiptProofSize
		}
		if len(msg.BlockUpdates) == 0 {
			size += len(bp)
			msg.BlockProof = bp
			msg.height = rm.BlockProof.BlockWitness.Height
			s.l.Tracef("Segment: at %d BlockProof: %x", msg.height, msg.BlockProof)
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

				// TODO: need a confirmation
				// I'm not sure why this EventProofs is missing
				// at here this https://github.com/icon-project/btp/blob/master/cmd/btpsimple/module/icon/sender.go#L162

				if b, err := codec.RLP.MarshalToBytes(trp); err != nil {
					return nil, err
				} else {
					msg.ReceiptProofs = append(msg.ReceiptProofs, b)
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
			msg.ReceiptProofs = append(msg.ReceiptProofs, b)
		}
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
	return segments, nil
}

// Can't import pra package because of cycle import
type parachainBlockUpdateExtra struct {
	ScaleEncodedBlockHeader []byte
	FinalityProofs          [][]byte
}

type parachainBlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	FinalityProof           []byte
}

func (s *sender) Segment(rm *chain.RelayMessage, height int64) ([]*chain.Segment, error) {
	s.l.Tracef("Segments: create Segment for height %d", height)
	switch s.src.BlockChain() {
	case "icon":
		return s.iconSegment(rm, height)
	case "pra":
		return s.praSegment(rm, height)
	default:
		return nil, ErrNotSupportedSrcChain
	}
}

func (s *sender) isOverSizeLimit(size int) bool {
	return txSizeLimit < float64(size)
}

// UpdateSegment updates segment
func (s *sender) UpdateSegment(bp *chain.BlockProof, segment *chain.Segment) error {
	p := segment.TransactionParam.(*TransactionParam)
	cd := p.Data.(CallData)
	rmp := cd.Params.(BMCRelayMethodParams)
	msg := &RelayMessage{}
	b, err := base64.URLEncoding.DecodeString(rmp.Messages)
	if err != nil {
		return err
	}
	if _, err = codec.RLP.UnmarshalFromBytes(b, msg); err != nil {
		return err
	}
	if msg.BlockProof, err = codec.RLP.MarshalToBytes(bp); err != nil {
		return err
	}
	segment.TransactionParam, err = s.newTransactionParam(rmp.Prev, msg)
	return err
}

func (s *sender) signAndSendTransaction(p *TransactionParam) (chain.GetResultParam, error) {
	thp := &TransactionHashParam{}
SignLoop:
	for {
		if err := s.c.SignTransaction(s.w, p); err != nil {
			return nil, err
		}
	SendLoop:
		for {
			// s.l.Tracef("signAndSendTransaction: TransactionParam %+v\n", p)
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

func (s *sender) sendFragmentations(tps []*TransactionParam, idxs []int) (chain.GetResultParam, error) {
	s.l.Tracef("sendFragmentations: start")
	var grp chain.GetResultParam
	var err error

	for i := 0; i < len(tps); i++ {
		grp, err = s.signAndSendTransaction(tps[i])
		s.l.Debugf("sendFragmentations: fragment %d hash %+v", idxs[i], grp)
		if err != nil {
			s.l.Panicf("sendFragmentations: fails result %+v error%+v", grp, err)
		}
		// TODO use (finalizelatency * blockinterval / appropriateratio) * time.Second
		time.Sleep(time.Duration(s.FinalizeLatency()) * time.Second)
	}

	s.l.Tracef("sendFragmentations: end")
	return grp, err
}

func (s *sender) relayByFragments(p *TransactionParam, dataSize int) (chain.GetResultParam, error) {
	callData, ok1 := p.Data.(CallData)
	if !ok1 {
		return nil, fmt.Errorf("casting failure")
	}

	bmcRelayMessageParams, ok2 := callData.Params.(BMCRelayMethodParams)
	if !ok2 {
		return nil, fmt.Errorf("casting failure")
	}

	prev := bmcRelayMessageParams.Prev
	msg := bmcRelayMessageParams.Messages
	tps := make([]*TransactionParam, 0)
	idxs := make([]int, 0)
	// + 1 is safeguard against transaction limit
	nuFragments := int(math.Ceil(float64(dataSize)/float64(txMaxDataSize))) + 1
	fragmentStringSize := len(msg) / nuFragments
	if (len(msg) % nuFragments) != 0 {
		nuFragments += 1
	}

	s.l.Debugf("relayByFragments: fragments: %d size: %d", nuFragments, fragmentStringSize)
	for i := 0; i < len(msg); i += fragmentStringSize {
		end := i + fragmentStringSize

		if end > len(msg) {
			end = len(msg)
		}

		var index int
		if i == 0 {
			index = ^nuFragments + 2
		} else if end >= len(msg) {
			index = 0
		} else {
			index = (nuFragments - 1 - (i / fragmentStringSize))
		}

		tp, err := s.newFragmentationsTransactionParam(prev, msg[i:end], index)
		if err != nil {
			return nil, err
		}

		tps = append(tps, tp)
		idxs = append(idxs, index)
	}
	return s.sendFragmentations(tps, idxs)
}

func (s *sender) Relay(segment *chain.Segment) (chain.GetResultParam, error) {
	p, ok := segment.TransactionParam.(*TransactionParam)
	if !ok {
		return nil, fmt.Errorf("casting failure")
	}

	dataSize := countBytesOfCompactJSON(p.Data)
	if dataSize > txMaxDataSize {
		return s.relayByFragments(p, dataSize)
	} else {
		return s.signAndSendTransaction(p)
	}
}

func (s *sender) GetResult(p chain.GetResultParam) (chain.TransactionResult, error) {
	if txh, ok := p.(*TransactionHashParam); ok {
		tries := 0
		for {
			tries++
			txr, err := s.c.GetTransactionResult(txh)
			if err != nil && tries < MaxDefaultGetRelayResultRetries {
				if RetryHTTPError.MatchString(err.Error()) {
					<-time.After(DefaultGetRelayResultInterval)
					s.l.Tracef("GetResult: retry %d with GetResult %s err:%+v", tries, txh.Hash, err)
					continue
				}

				if je, ok := err.(*jsonrpc.Error); ok {
					switch je.Code {
					case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
						<-time.After(DefaultGetRelayResultInterval)
						s.l.Tracef("GetResult: retry %d with GetResult %s err:%+v", tries, txh.Hash, err)
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

func (s *sender) GetStatus() (*chain.BMCLinkStatus, error) {
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
	ls := &chain.BMCLinkStatus{}
	ls.TxSeq, err = bs.TxSeq.Value()
	ls.RxSeq, err = bs.RxSeq.Value()
	ls.Verifier.Height, err = bs.Verifier.Height.Value()
	ls.Verifier.Offset, err = bs.Verifier.Offset.Value()
	ls.Verifier.LastHeight, err = bs.Verifier.LastHeight.Value()
	ls.BMRs = make([]struct {
		Address      string
		BlockCount   int64
		MessageCount int64
	}, len(bs.BMRs))
	for i, bmr := range bs.BMRs {
		ls.BMRs[i].Address = string(bmr.Address)
		ls.BMRs[i].BlockCount, err = bmr.BlockCount.Value()
		ls.BMRs[i].MessageCount, err = bmr.MessageCount.Value()
	}
	ls.BMRIndex, err = bs.BMRIndex.Int()
	ls.RotateHeight, err = bs.RotateHeight.Value()
	ls.RotateTerm, err = bs.RotateTerm.Int()
	ls.DelayLimit, err = bs.DelayLimit.Int()
	ls.MaxAggregation, err = bs.MaxAggregation.Int()
	ls.CurrentHeight, err = bs.CurrentHeight.Value()
	ls.RxHeight, err = bs.RxHeight.Value()
	ls.RxHeightSrc, err = bs.RxHeightSrc.Value()
	ls.BlockIntervalSrc, err = bs.BlockIntervalSrc.Int()
	ls.BlockIntervalDst, err = bs.BlockIntervalDst.Int()
	return ls, nil
}

func (s *sender) isOverLimit(size int) bool {
	return txSizeLimit < float64(size)
}

func (s *sender) MonitorLoop(height int64, cb chain.MonitorCallback, scb func()) error {
	br := &BlockRequest{
		Height: NewHexInt(height),
	}
	return s.c.MonitorBlock(br,
		func(conn *jsonrpc.RecConn, v *BlockNotification) error {
			if h, err := v.Height.Value(); err != nil {
				return err
			} else {
				return cb(h)
			}
		},
		func(conn *jsonrpc.RecConn) {
			s.l.Debugf("MonitorLoop connected %s", conn.LocalAddr().String())
			if scb != nil {
				scb()
			}
		},
		func(conn *jsonrpc.RecConn, err error) {
			s.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
			conn.CloseAndReconnect()
		})
}

func (s *sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}

// the unit is block
func (s *sender) FinalizeLatency() int {
	//on-the-next
	return 1
}

func NewSender(src, dst chain.BtpAddress, w Wallet, endpoint string, opt map[string]interface{}, l log.Logger) chain.Sender {
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

func mapErrorWithTransactionResult(txr *TransactionResult, err error) error {
	err = mapError(err)
	if err == nil && txr != nil && txr.Status != ResultStatusSuccess {
		fc, _ := txr.Failure.CodeValue.Value()
		if fc < ResultStatusFailureCodeRevert || fc > ResultStatusFailureCodeEnd {
			err = fmt.Errorf("failure with code:%s, message:%s",
				txr.Failure.CodeValue, txr.Failure.MessageValue)
		} else {
			err = chain.NewRevertError(int(fc - ResultStatusFailureCodeRevert))
		}
	}
	return err
}
