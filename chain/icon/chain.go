/*
 * Copyright 2020 ICON Foundation
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
	"fmt"
	"math/big"
	"sync"
	"sync/atomic"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
)

type chainInfo struct {
	StartHeight     int64
	NetworkTypeName string
}

type SimpleChain struct {
	s  chain.Sender
	r  *Receiver
	ci *chainInfo

	src chain.BtpAddress
	dst chain.BtpAddress

	bs  *chain.BMCLinkStatus
	l   log.Logger
	cfg *chain.Config

	bds []*BTPBlockData
	rms []*BTPRelayMessage

	rmsMtx      sync.RWMutex
	rmSeq       uint64
	heightOfDst int64

	relayble bool
}

func (s *SimpleChain) _log(prefix string, rm *BTPRelayMessage, segment *chain.Segment, segmentIdx int) {
	if segment == nil {
		s.l.Debugf("%s rm message seq:%, rm height:%, relayLen:%d",
			prefix,
			rm.MessageSeq(),
			rm.Height(),
			len(rm.Messages))
	} else {
		s.l.Debugf("%s rm:%d [i:%d,h:%d,bu:%d,seq:%d,evt:%d,txh:%v]",
			prefix,
			rm.Height(),
			segmentIdx,
			segment.Height,
			segment.NumberOfBlockUpdate,
			segment.EventSequence,
			segment.NumberOfEvent,
			segment.GetResultParam)
	}
}

func (s *SimpleChain) relay() error {
	s.rmsMtx.RLock()
	defer s.rmsMtx.RUnlock()
	var rmSize int
	if s.cfg.MaxSizeTx {
		rmSize = len(s.rms) - 1
	} else {
		rmSize = len(s.rms)
		s.rms = append(s.rms, NewRelayMessage())
	}

	for i := 0; i < rmSize; i++ {
		s._log("before relay", s.rms[i], nil, -1)
		b, err := codec.RLP.MarshalToBytes(s.rms[i])
		if err != nil {
			return err
		}

		if s.rms[i].Segments() == nil {
			s.rms[i].SetSegments(b, s.rms[i].Height(), int64(s.rms[i].MessageSeq()))
		}

		if s.rms[i].Segments().GetResultParam == nil {
			s.rms[i].Segments().TransactionResult = nil
			if s.rms[i].Segments().GetResultParam, err = s.s.Relay(s.rms[i].Segments()); err != nil {
				s.l.Panicf("fail to Relay err:%+v", err)
			}
			s._log("after relay", s.rms[i], s.rms[i].Segments(), -1)
			go s.result(s.rms[i].Segments())
		}
	}
	return nil
}

func (s *SimpleChain) segment() error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	bdsLen := len(s.bds) - 1
	if len(s.rms) == 0 {
		s.rms = append(s.rms, NewRelayMessage())
	}

	//blockUpdate
	s.BlockUpdateSegment(s.bds[bdsLen].Bu, s.bds[bdsLen].Height)
	//messageProof
	if s.bds[bdsLen].Mt != nil {
		s.MessageSegment(s.bds[bdsLen])
	}
	return nil
}

func (s *SimpleChain) BlockUpdateSegment(bu *BTPBlockUpdate, heightOfSrc int64) error {
	//blockUpdate
	//skipped first btp block
	if heightOfSrc != s.ci.StartHeight {
		tpm, err := NewTypePrefixedMessage(bu)
		b, err := codec.RLP.MarshalToBytes(tpm)
		if err != nil {
			return err
		}
		rmsSize, err := s.rms[len(s.rms)-1].Size()
		if err != nil {
			return err
		}
		if s.isOverLimit(rmsSize + len(b)) {
			s.rms = append(s.rms, NewRelayMessage())
		}
		s.rms[len(s.rms)-1].SetHeight(heightOfSrc)
		s.rms[len(s.rms)-1].AppendMessage(tpm)
	}
	return nil
}

func (s *SimpleChain) MessageSegment(bd *BTPBlockData) error {
	var endIndex int
	for endIndex = bd.PartialOffset + 1; endIndex < bd.Mt.Len(); endIndex++ {
		//TODO refactoring
		p, err := bd.Mt.Proof(bd.PartialOffset+1, endIndex)
		if err != nil {
			return err
		}
		tpm, err := NewTypePrefixedMessage(*p)
		b, err := codec.RLP.MarshalToBytes(tpm)
		//if s.isOverLimit(s.rms[len(s.rms)-1].Size() + s.bds[bdIndex].Mt.ProofLength(s.bds[bdIndex].PartialOffset, endIndex)) {
		rmsSize, err := s.rms[len(s.rms)-1].Size()
		if err != nil {
			return err
		}
		if s.isOverLimit(rmsSize + len(b)) {
			s.rms = append(s.rms, NewRelayMessage())
			if err != nil {
				return err
			}
			bd.PartialOffset = endIndex

			s.rms[len(s.rms)-1].SetHeight(bd.Height)
			s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
			s.rms[len(s.rms)-1].AppendMessage(tpm)
		}
	}
	if bd.PartialOffset != bd.Mt.Len() {
		p, err := bd.Mt.Proof(bd.PartialOffset+1, endIndex)
		if err != nil {
			return err
		}

		tpm, err := NewTypePrefixedMessage(*p)
		if err != nil {
			return err
		}

		s.rms[len(s.rms)-1].SetHeight(bd.Height)
		s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
		s.rms[len(s.rms)-1].AppendMessage(tpm)
		bd.PartialOffset = bd.Mt.Len()
	}
	return nil
}

func (s *SimpleChain) updateSegments(h int64, seq *big.Int) error {
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	vs := &VerifierStatus{}
	_, err := codec.RLP.UnmarshalFromBytes(s.bs.Verifier.Extra, vs)
	if err != nil {
		return err
	}

	if !(s.bs.RxSeq.Int64() < s.rms[0].Height()) {
		for i := 0; i < len(s.rms); i++ {
			if (s.bs.RxSeq.Int64() <= s.rms[i].Height()) || (s.rms[i].Height() > h) {
				s.rms[i].Segments().GetResultParam = nil
			}
		}
	} else {
		s.l.Panicf("No relay messages collected.")
	}

	return nil
}

func (s *SimpleChain) result(segment *chain.Segment) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	var err error
	segment.TransactionResult, err = s.s.GetResult(segment.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				segment.GetResultParam, ec)
			switch ec.ErrorCode() {
			case BMVUnknown:
				s.l.Panicf("BMVUnknown Revert :%v ErrorCoder:%+v",
					segment.GetResultParam, ec)
			case BMVNotVerifiable:
				s.updateSegments(segment.Height, segment.EventSequence)
				segment.GetResultParam = nil
			case BMVAlreadyVerified:
				s.updateRelayMessage(segment.Height, segment.EventSequence)
			case BMCRevertUnauthorized:
				segment.GetResultParam = nil
			default:
				s.l.Panicf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
					segment.GetResultParam, ec)
			}
		} else {
			s.l.Panicf("fail to GetResult GetResultParam:%v err:%+v",
				segment.GetResultParam, err)
		}
	}
}

func (s *SimpleChain) isOverLimit(size int) bool {
	return s.s.TxSizeLimit() < size
}

func (s *SimpleChain) addRelayMessage(bu *BTPBlockUpdate, bh *BTPBlockHeader) error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()

	if len(bu.BTPBlockProof) == 0 {
		p, err := s.r.GetBTPProof(bh.MainHeight, s.cfg.Src.Nid)
		if err != nil {
			return err
		}
		bu.BTPBlockProof = p
	}
	var mt *mbt.MerkleBinaryTree

	if bh.MessageCount > 0 {
		m, err := s.r.GetBTPMessage(bh.MainHeight, s.cfg.Src.Nid)
		if err != nil {
			return err
		}

		if mt, err = mbt.NewMerkleBinaryTree(mbt.HashFuncByUID(s.ci.NetworkTypeName), m); err != nil {
			return err
		}
	}

	btpBlock := &BTPBlockData{
		Bu:         bu,
		MessageCnt: bh.MessageCount,
		Mt:         mt, PartialOffset: 0,
		Height: bh.MainHeight}

	s.bds = append(s.bds, btpBlock)
	return nil
}

func (s *SimpleChain) updateRelayMessage(h int64, seq *big.Int) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	s.l.Debugf("updateRelayMessage h:%d seq:%d monitorHeight:%d", h, seq, s.monitorHeight())
	bdIndex := 0
	rmIndex := 0

	for i, bd := range s.bds {
		if (bd.Height == h || big.NewInt(bd.MessageCnt) == seq) || bd.Height < h {
			bdIndex = i
		}
	}

	for i, rm := range s.rms {
		if (rm.Height() == h || big.NewInt(int64(rm.MessageSeq())) == seq) || rm.Height() < h {
			rmIndex = i
		}
	}

	s.bds = s.bds[bdIndex:]
	s.rms = s.rms[rmIndex:]
}

func (s *SimpleChain) OnBlockOfDst(height int64) error {
	s.l.Tracef("OnBlockOfDst height:%d", height)
	atomic.StoreInt64(&s.heightOfDst, height)
	h, seq := s.bs.Verifier.Height, s.bs.RxSeq
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	if h != s.bs.Verifier.Height || seq != s.bs.RxSeq {
		h, seq = s.bs.Verifier.Height, s.bs.RxSeq
		s.updateRelayMessage(h, seq)
	}
	return nil
}

func (s *SimpleChain) OnBlockOfSrc(bu *BTPBlockUpdate) error {
	bh := &BTPBlockHeader{}
	_, err := codec.RLP.UnmarshalFromBytes(bu.BTPBlockHeader, bh)
	if err != nil {
		return err
	}

	s.l.Tracef("OnBlockOfSrc height:%d, bu.Height:%d", s.ci.StartHeight, bh.MainHeight)
	if s.ci.StartHeight == 0 {
		s.SetChainInfo()
	}

	if s.relayble && bh.MainHeight != s.ci.StartHeight {
		if err := s.addRelayMessage(bu, bh); err != nil {
			return err
		}

		if err := s.segment(); err != nil {
			return err
		}

		if err := s.relay(); err != nil {
			return err
		}
	}
	return nil
}

func (s *SimpleChain) RefreshStatus() error {
	bmcStatus, err := s.s.GetStatus()
	if err != nil {
		return err
	}
	s.bs = bmcStatus
	return nil
}

func (s *SimpleChain) init() error {
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	atomic.StoreInt64(&s.heightOfDst, s.bs.CurrentHeight)
	return nil
}

func (s *SimpleChain) receiveHeight() (int64, error) {
	if s.bs.Verifier.Height == s.ci.StartHeight {
		return s.bs.Verifier.Height, nil
	} else {
		if len(s.rms) == 0 {
			s.rms = append(s.rms, NewRelayMessage())
		}
		h, err := s.r.GetBTPBlockHeader(s.bs.Verifier.Height, s.cfg.Src.Nid)
		if err != nil {
			return 0, err
		}
		bh := &BTPBlockHeader{}
		_, err = codec.RLP.UnmarshalFromBytes(h, bh)
		if err != nil {
			return 0, err
		}

		vs := &VerifierStatus{}
		_, err = codec.RLP.UnmarshalFromBytes(s.bs.Verifier.Extra, vs)
		if err != nil {
			return 0, err
		}

		index := (s.bs.RxSeq.Int64() - vs.SequenceOffset) - vs.FirstMessageSn
		if index < bh.MessageCount {
			var mt *mbt.MerkleBinaryTree
			m, err := s.r.GetBTPMessage(bh.MainHeight, s.cfg.Src.Nid)
			if err != nil {
				return 0, err
			}
			if mt, err = mbt.NewMerkleBinaryTree(mbt.HashFuncByUID(s.ci.NetworkTypeName), m); err != nil {
				return 0, err
			}
			bd := &BTPBlockData{
				Mt:            mt,
				PartialOffset: int(index),
				MessageCnt:    bh.MessageCount,
				Height:        bh.MainHeight,
			}

			s.MessageSegment(bd)
			s.relay()
		}
		return bh.MainHeight + 1, nil
	}
}

func (s *SimpleChain) monitorHeight() int64 {
	return atomic.LoadInt64(&s.heightOfDst)
}

func (s *SimpleChain) Serve(sender chain.Sender) error {
	s.s = sender
	s.r = NewReceiver(s.src, s.dst, s.cfg.Src.Endpoint, s.cfg.Src.Options, s.l)
	s.ci = &chainInfo{}
	//TODO Pre rotation settings
	s.relayble = true

	s.SetChainInfo()

	if err := s.Monitoring(); err != nil {
		return err
	}

	return nil
}
func (s *SimpleChain) SetChainInfo() error {
	ni, err := s.r.GetBTPNetworkInfo(s.cfg.Src.Nid)
	if err != nil {
		return err
	}
	sh, err := ni.StartHeight.Value()
	s.ci.StartHeight = sh + 1
	s.ci.NetworkTypeName = ni.NetworkTypeName
	return nil
}

func (s *SimpleChain) Monitoring() error {
	if err := s.init(); err != nil {
		return err
	}

	h, err := s.receiveHeight()
	if err != nil {
		return err
	}

	s.l.Debugf("_init height:%d, dst(%s, src height:%d, seq:%d, last:%d), receive:%d",
		s.ci.StartHeight, s.dst, s.bs.Verifier.Height, s.bs.RxSeq, s.bs.Verifier.Height, h)

	errCh := make(chan error)
	go func() {
		err := s.s.MonitorLoop(
			s.bs.CurrentHeight,
			s.OnBlockOfDst,
			func() {
				s.l.Debugf("Connect MonitorLoop")
				errCh <- nil
			})
		select {
		case errCh <- err:
		default:
		}
	}()
	go func() {
		err := s.r.ReceiveLoop(
			h,
			s.cfg.Src.Nid,
			false,
			s.OnBlockOfSrc,
			func() {
				s.l.Debugf("Connect ReceiveLoop")
				errCh <- nil
			})
		select {
		case errCh <- err:
		default:
		}
	}()
	for {
		select {
		case err := <-errCh:
			if err != nil {
				return err
			}
		}
	}
}

func NewChain(cfg *chain.Config, l log.Logger) *SimpleChain {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		l:   l.WithFields(log.Fields{log.FieldKeyChain: fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		bds: make([]*BTPBlockData, 0),
		rms: make([]*BTPRelayMessage, 0),
	}

	return s
}
