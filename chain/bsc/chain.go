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

package bsc

import (
	"encoding/hex"
	"fmt"
	"sync"
	"time"
	"unsafe"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
)

const (
	DefaultDBDir  = "db"
	DefaultDBType = db.GoLevelDBBackend
	// DefaultBufferScaleOfBlockProof Base64 in:out = 6:8
	DefaultBufferScaleOfBlockProof  = 0.5
	DefaultBufferNumberOfBlockProof = 100
	DefaultBufferInterval           = 5 * time.Second
	DefaultReconnectDelay           = time.Second
)

type SimpleChain struct {
	s chain.Sender
	r chain.Receiver

	src chain.BtpAddress
	dst chain.BtpAddress

	bs  *chain.BMCLinkStatus //getstatus(dst.src)
	l   log.Logger
	cfg *chain.Config

	ssMtx  sync.RWMutex
	ss     []*chain.Segment
	rm     *chain.RelayMessage
	rmSize int
}

func (s *SimpleChain) _hasWait(rm *chain.RelayMessage) bool {
	for _, segment := range rm.Segments {
		if segment != nil && segment.GetResultParam != nil && segment.TransactionResult == nil {
			return true
		}
	}
	return false
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

func (s *SimpleChain) addSegment() error {
	s.ssMtx.Lock()
	defer s.ssMtx.Unlock()

	rm := &RelayMessage{
		Receipts: make([][]byte, 0),
	}
	var (
		b   []byte
		err error
	)
	numOfEvents := 0
	for _, rp := range s.rm.ReceiptProofs {
		if len(rp.Events) == 0 {
			continue
		}
		numOfEvents += len(rp.Events)
		if b, err = codec.RLP.MarshalToBytes(rp.Events); err != nil {
			return err
		}
		r := &Receipt{
			Index:  int64(rp.Index),
			Events: b,
			Height: rp.Height,
		}
		if b, err = codec.RLP.MarshalToBytes(r); err != nil {
			return err
		}
		rm.Receipts = append(rm.Receipts, b)
	}
	if b, err = codec.RLP.MarshalToBytes(rm); err != nil {
		return err
	}
	lrp := s.rm.ReceiptProofs[len(s.rm.ReceiptProofs)-1]
	le := lrp.Events[len(lrp.Events)-1]
	seg := &chain.Segment{
		TransactionParam: b,
		Height:           lrp.Height,
		EventSequence:    le.Sequence,
		NumberOfEvent:    numOfEvents,
	}
	s.ss = append(s.ss, seg)
	lrp.Events = lrp.Events[:0]
	s.rm.ReceiptProofs[0] = lrp
	s.rm.ReceiptProofs = s.rm.ReceiptProofs[:1]
	s.rmSize = 0
	return nil
}

func (s *SimpleChain) _relay() {
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

func (s *SimpleChain) isOverLimit(size int) bool {
	return s.s.TxSizeLimit() < size
}

func (s *SimpleChain) RemoveSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) {
	r := 0
	for i, s := range ss {
		if s.EventSequence.Int64() <= bs.RxSeq.Int64() {
			r = i + 1
		}
	}
	s.removeSegment(r)
}

func (s *SimpleChain) removeSegment(offset int) {
	s.ssMtx.Lock()
	defer s.ssMtx.Unlock()

	if offset < 1 {
		return
	}
	s.l.Debugf("removeSegment ss:%d removeRelayMessage %d ~ %d",
		len(s.ss),
		s.ss[0].EventSequence,
		s.ss[offset-1].EventSequence)
	s.ss = s.ss[offset:]
}

func (s *SimpleChain) UpdateSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) error {
	vs := &VerifierStatus_v2{}
	_, err := codec.RLP.UnmarshalFromBytes(bs.Verifier.Extra, vs)
	if err != nil {
		return err
	}

	if !(bs.RxSeq.Int64() < ss[0].Height) {
		for i := 0; i < len(ss); i++ {
			if (bs.RxSeq.Int64() <= ss[i].Height) || (ss[i].Height > bs.Verifier.Height) {
				ss[i].GetResultParam = nil
			}
		}
	} else {
		s.l.Panicf("No relay messages collected.")
	}

	return nil
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
				s.UpdateSegment(s.bs, s.ss)
				segment.GetResultParam = nil
			case BMVAlreadyVerified:
				s.RemoveSegment(s.bs, s.ss)
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

func (s *SimpleChain) OnBlockOfDst(height int64) error {
	s.l.Tracef("OnBlockOfDst height:%d", height)
	//h, seq := s.bs.Verifier.Height, s.bs.RxSeq
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	if s.bs.Verifier.Height != s.bs.Verifier.Height || s.bs.RxSeq != s.bs.RxSeq {
		s.l.Debugf("OnBlockOfDst h:%d seq:%d monitorHeight:%d",
			s.bs.Verifier.Height, s.bs.RxSeq, s.bs.CurrentHeight)
		r := 0
		for i, seg := range s.ss {
			if seg.EventSequence.Int64() <= s.bs.RxSeq.Int64() {
				r = i + 1
			}
		}
		s.removeSegment(r)
	}
	return nil
}

func (s *SimpleChain) OnBlockOfSrc(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) error {
	s.l.Debugf("OnBlockOfSrc rps:%d", len(rps))
	var err error
	for _, rp := range rps {
		trp := &chain.ReceiptProof{
			Index:  rp.Index,
			Events: make([]*chain.Event, 0),
			Height: rp.Height,
		}
		s.rm.ReceiptProofs = append(s.rm.ReceiptProofs, trp)

		for _, e := range rp.Events {
			size := sizeOfEvent(e)
			if s.isOverLimit(s.rmSize+size) && s.rmSize > 0 {
				if err = s.addSegment(); err != nil {
					return err
				}
			}
			trp.Events = append(trp.Events, e)
			s.rmSize += size
		}

		//last event
		if s.isOverLimit(s.rmSize) {
			if err = s.addSegment(); err != nil {
				return err
			}
		}

		//remove last receipt if empty
		if len(trp.Events) == 0 {
			s.rm.ReceiptProofs = s.rm.ReceiptProofs[:len(s.rm.ReceiptProofs)-1]
		}
	}

	if !s.cfg.MaxSizeTx {
		//immediately relay
		if s.rmSize > 0 {
			if err = s.addSegment(); err != nil {
				return err
			}
		}
	}
	s._relay()
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

func (s *SimpleChain) Serve(sender chain.Sender) error {
	s.s = sender
	s.r = NewReceiver(s.src, s.dst, s.cfg.Src.Endpoint, s.cfg.Src.Options, s.l)

	if err := s.Monitoring(); err != nil {
		return err
	}

	return nil
}

func (s *SimpleChain) Monitoring() error {
	if err := s.RefreshStatus(); err != nil {
		return err
	}
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
			s.bs.Verifier.Height,
			s.bs.RxSeq,
			s.OnBlockOfSrc,
			func() {
				s.l.Debugf("Source %s, height:%d, seq:%d",
					s.src, s.bs.Verifier.Height, s.bs.RxSeq)
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

func sizeOfEvent(rp *chain.Event) int {
	return int(unsafe.Sizeof(rp))
}

func NewChain(cfg *chain.Config, l log.Logger) *SimpleChain {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		l:   l.WithFields(log.Fields{log.FieldKeyChain:
		//fmt.Sprintf("%s->%s", cfg.Src.Address.NetworkAddress(), cfg.Dst.Address.NetworkAddress())}),
		fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		//rms: make([]*chain.RelayMessage, 0),
		rm: &chain.RelayMessage{
			BlockUpdates:  make([]*chain.BlockUpdate, 0),
			ReceiptProofs: make([]*chain.ReceiptProof, 0),
		},
		ss: make([]*chain.Segment, 0),
	}
	return s
}
