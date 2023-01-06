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
	"encoding/hex"
	"fmt"
	"sync"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
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

	ss []*chain.Segment

	ssMtx    sync.RWMutex
	rmSeq    uint64
	cs       ChainSegment
	relayble bool
}

//TODO refactoring rename
type ChainSegment interface {
	Segments(bu *BTPBlockUpdate, seq int64, maxSizeTx bool,
		msgs []string, offset int64, ss *[]*chain.Segment) error
	RemoveSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment)
	UpdateSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) error
}

func (s *SimpleChain) _log(prefix string, segment *chain.Segment) {
	if segment == nil {
		s.l.Debugf("%s rm message seq:%, rm height:%",
			prefix,
			segment.EventSequence,
			segment.Height)
	} else {
		s.l.Debugf("%s segment [h:%d,bu:%d,seq:%d,evt:%d,txh:%v]",
			prefix,
			segment.Height,
			segment.NumberOfBlockUpdate,
			segment.EventSequence,
			segment.NumberOfEvent,
			segment.GetResultParam)
	}
}

func (s *SimpleChain) result(segment *chain.Segment) {
	s.ssMtx.Lock()
	defer s.ssMtx.Unlock()
	var err error
	segment.TransactionResult, err = s.s.GetResult(segment.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				segment.GetResultParam, ec)
			s.RefreshStatus()
			switch ec.ErrorCode() {
			case BMVUnknown:
				s.l.Panicf("BMVUnknown Revert :%v ErrorCoder:%+v",
					segment.GetResultParam, ec)
			case BMVNotVerifiable:
				s.cs.UpdateSegment(s.bs, s.ss)
			case BMVAlreadyVerified:
				s.cs.RemoveSegment(s.bs, s.ss)
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

func (s *SimpleChain) relay() {
	s.ssMtx.RLock()
	defer s.ssMtx.RUnlock()

	var err error
	for _, seg := range s.ss {
		s._log("before relay", seg)
		s.l.Debugln("TransactionParam:" + hex.EncodeToString(seg.TransactionParam.([]byte)))
		if seg.GetResultParam == nil {
			seg.TransactionResult = nil
			if seg.GetResultParam, err = s.s.Relay(seg); err != nil {
				s.l.Panicf("fail to Relay err:%+v", err)
			}
			s._log("after relay", seg)
			go s.result(seg)
		}
	}
}

func (s *SimpleChain) OnBlockOfDst(height int64) error {
	s.l.Tracef("OnBlockOfDst height:%d", height)
	//h, seq := s.bs.Verifier.Height, s.bs.RxSeq
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	if s.bs.Verifier.Height != s.bs.Verifier.Height || s.bs.RxSeq != s.bs.RxSeq {
		s.cs.RemoveSegment(s.bs, s.ss)
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
		var msgs []string
		var offset int64
		if len(bu.BTPBlockProof) == 0 {
			p, err := s.r.GetBTPProof(bh.MainHeight, s.cfg.Src.Nid)
			if err != nil {
				return err
			}
			bu.BTPBlockProof = p
		}

		if bh.MessageCount > 0 {
			msgs, err = s.r.GetBTPMessage(bh.MainHeight, s.cfg.Src.Nid)
			if err != nil {
				return err
			}
		}

		if s.cfg.Src.BridgeMode {
			o, err := s.r.getBTPLinkOffset()
			if err != nil {
				return err
			}
			offset = o
		}

		s.cs.Segments(bu, s.bs.RxSeq.Int64(), s.cfg.MaxSizeTx, msgs, offset, &s.ss)
		s.relay()
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

func (s *SimpleChain) receiveHeight() (int64, error) {
	if s.bs.Verifier.Height == s.ci.StartHeight {
		return s.bs.Verifier.Height, nil
	} else {
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
			m, err := s.r.GetBTPMessage(bh.MainHeight, s.cfg.Src.Nid)

			offset, err := s.r.getBTPLinkOffset()
			if err != nil {
				return 0, err
			}

			s.cs.Segments(&BTPBlockUpdate{BTPBlockHeader: h},
				s.bs.RxSeq.Int64(), s.cfg.MaxSizeTx, m[:index], offset, &s.ss)
			//s.MessageSegment(bd)
			//s.relay()
		}
		return bh.MainHeight + 1, nil
	}
}

func (s *SimpleChain) Serve(sender chain.Sender) error {
	s.s = sender
	s.r = NewReceiver(s.src, s.dst, s.cfg.Src.Endpoint, s.cfg.Src.Options, s.l)
	s.ci = &chainInfo{}
	//TODO Pre rotation settings
	s.relayble = true

	if s.cfg.Src.BridgeMode {
		s.cs = NewBridge(s.l, s.dst, s.s.TxSizeLimit(), s.ssMtx)
	} else {
		s.cs = NewBTP(s.l, s.dst, s.s.TxSizeLimit(), s.ssMtx)
	}

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
	if err := s.RefreshStatus(); err != nil {
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
		ss:  make([]*chain.Segment, 0),
	}

	return s
}
