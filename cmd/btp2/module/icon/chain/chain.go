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

package chain

import (
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
	"math/big"
	"sync"
	"sync/atomic"
)

type chainInfo struct {
	StartHeight     int64
	NetworkTypeName string
}

type SimpleChain struct {
	s  module.Sender
	r  *icon.Receiver
	ci *chainInfo

	src module.BtpAddress
	dst module.BtpAddress

	bs  *module.BMCLinkStatus //getstatus(dst.src)
	l   log.Logger
	cfg *module.Config

	bds []*icon.BTPBlockData
	rms []*icon.BTPRelayMessage

	rmsMtx      sync.RWMutex
	rmSeq       uint64
	heightOfDst int64

	bmrIndex       int
	relayble       bool
	relaybleIndex  int
	relaybleHeight int64
}

func (s *SimpleChain) _log(prefix string, rm *icon.BTPRelayMessage, segment *module.Segment, segmentIdx int) {
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
		s.rms = append(s.rms, icon.NewRelayMessage())
	}

	for i := 0; i < rmSize; i++ {
		s._log("before relay", s.rms[i], nil, -1)
		b, err := codec.RLP.MarshalToBytes(s.rms[i])
		if err != nil {
			return err
		}

		if s.rms[i].Segments() == nil {
			s.rms[i].SetSegments(b, s.rms[i].Height())
		}

		if s.rms[i].Segments().GetResultParam == nil {
			s.rms[i].Segments().TransactionResult = nil
			fmt.Print(hex.EncodeToString(b), "\n\n")
			fmt.Print(base64.URLEncoding.EncodeToString(b), "\n\n")
			if s.rms[i].Segments().GetResultParam, err = s.s.Relay(s.rms[i].Segments()); err != nil {
				s.l.Panicf("fail to Relay err:%+v", err)
			}
			s._log("after relay", s.rms[i], s.rms[i].Segments(), -1)
			go s.result(i, s.rms[i].Segments())
		}
	}
	return nil
}

func (s *SimpleChain) segment() error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	bdsLen := len(s.bds) - 1
	if len(s.rms) == 0 {
		s.rms = append(s.rms, icon.NewRelayMessage())
	}

	//blockUpdate
	s.BlockUpdateSegment(s.bds[bdsLen].Bu, s.bds[bdsLen].HeightOfSrc)
	//messageProof
	if s.bds[bdsLen].Mt != nil {
		s.MessageSegment(s.bds[bdsLen].Mt, s.bds[bdsLen].PartialOffset, s.bds[bdsLen].HeightOfSrc)
	}
	return nil
}

func (s *SimpleChain) BlockUpdateSegment(bu *icon.BTPBlockUpdate, heightOfSrc int64) error {
	//blockUpdate
	//skipped first btp block
	if heightOfSrc != s.ci.StartHeight {
		tpm, err := icon.NewTypePrefixedMessage(bu)
		b, err := codec.RLP.MarshalToBytes(tpm)
		if err != nil {
			return err
		}
		rmsSize, err := s.rms[len(s.rms)-1].Size()
		if err != nil {
			return err
		}
		if s.isOverLimit(rmsSize + len(b)) {
			s.rms = append(s.rms, icon.NewRelayMessage())
		}
		s.rms[len(s.rms)-1].SetHeight(heightOfSrc)
		s.rms[len(s.rms)-1].AppendMessage(tpm)
	}
	return nil
}

func (s *SimpleChain) MessageSegment(mt *mbt.MerkleBinaryTree, partialOffset int, heightOfSrc int64) error {
	var endIndex int
	for endIndex = partialOffset + 1; endIndex < mt.Len(); endIndex++ {
		//TODO refactoring
		p, err := mt.Proof(partialOffset+1, endIndex)
		if err != nil {
			return err
		}
		tpm, err := icon.NewTypePrefixedMessage(*p)
		b, err := codec.RLP.MarshalToBytes(tpm)
		//if s.isOverLimit(s.rms[len(s.rms)-1].Size() + s.bds[bdIndex].Mt.ProofLength(s.bds[bdIndex].PartialOffset, endIndex)) {
		rmsSize, err := s.rms[len(s.rms)-1].Size()
		if err != nil {
			return err
		}
		if s.isOverLimit(rmsSize + len(b)) {
			s.rms = append(s.rms, icon.NewRelayMessage())
			if err != nil {
				return err
			}
			partialOffset = endIndex

			s.rms[len(s.rms)-1].SetHeight(heightOfSrc)
			s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
			s.rms[len(s.rms)-1].AppendMessage(tpm)
		}
	}
	if partialOffset != mt.Len() {
		p, err := mt.Proof(partialOffset+1, endIndex)
		if err != nil {
			return err
		}

		tpm, err := icon.NewTypePrefixedMessage(*p)
		if err != nil {
			return err
		}

		s.rms[len(s.rms)-1].SetHeight(heightOfSrc)
		s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
		s.rms[len(s.rms)-1].AppendMessage(tpm)
		partialOffset = mt.Len()
	}
	return nil
}

func (s *SimpleChain) result(index int, segment *module.Segment) {
	var err error
	segment.TransactionResult, err = s.s.GetResult(segment.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				segment.GetResultParam, ec)
			switch ec.ErrorCode() { //TODO Add BMV invalid code
			case module.BMVRevertInvalidSequence, module.BMVRevertInvalidBlockUpdateLower:
				segment.GetResultParam = nil
			case module.BMVRevertInvalidBlockWitnessOld:
				segment.GetResultParam = nil
			case module.BMVRevertInvalidSequenceHigher, module.BMVRevertInvalidBlockUpdateHigher, module.BMVRevertInvalidBlockProofHigher:
				segment.GetResultParam = nil
			case module.BMCRevertUnauthorized:
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

func (s *SimpleChain) addRelayMessage(bu *icon.BTPBlockUpdate, bh *icon.BTPBlockHeader) error {
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

	btpBlock := &icon.BTPBlockData{
		Bu: bu,
		Mt: mt, PartialOffset: 0,
		Seq:         s.rmSeq,
		HeightOfDst: s.monitorHeight(),
		HeightOfSrc: bh.MainHeight}

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
		if bd.HeightOfSrc == h {
			bdIndex = i
		}
	}

	for i, rm := range s.rms {
		if rm.Height() == h {
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

func (s *SimpleChain) OnBlockOfSrc(bu *icon.BTPBlockUpdate) error {
	bh := &icon.BTPBlockHeader{}
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
			s.rms = append(s.rms, icon.NewRelayMessage())
		}
		h, err := s.r.GetBTPBlockHeader(s.bs.Verifier.Height, s.cfg.Src.Nid)
		if err != nil {
			return 0, err
		}
		bh := &icon.BTPBlockHeader{}
		_, err = codec.RLP.UnmarshalFromBytes(h, bh)
		if err != nil {
			return 0, err
		}

		vs := &icon.VerifierStatus{}
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
			s.MessageSegment(mt, int(index), bh.MainHeight)
			s.relay()
		}
		return bh.MainHeight + 1, nil
	}
}

func (s *SimpleChain) monitorHeight() int64 {
	return atomic.LoadInt64(&s.heightOfDst)
}

func (s *SimpleChain) Serve(sender module.Sender) error {
	s.s = sender
	s.r = icon.NewReceiver(s.src, s.dst, s.cfg.Src.Endpoint, s.cfg.Src.Options, s.l)
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
			s.cfg.ProofFlag,
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

func NewChain(cfg *module.Config, l log.Logger) *SimpleChain {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		l:   l.WithFields(log.Fields{log.FieldKeyChain: fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		bds: make([]*icon.BTPBlockData, 0),
		rms: make([]*icon.BTPRelayMessage, 0),
	}

	return s
}
