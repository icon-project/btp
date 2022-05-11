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
	"math"
	"path/filepath"
	"sync"
	"sync/atomic"
	"time"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
	"github.com/icon-project/btp/common/wallet"
	_ "github.com/icon-project/btp/cmd/btpsimple/module/registry"
)

const (
	DefaultDBDir  = "db"
	DefaultDBType = db.GoLevelDBBackend
	// DefaultBufferScaleOfBlockProof Base64 in:out = 6:8
	DefaultBufferScaleOfBlockProof  = 0.5
	DefaultBufferNumberOfBlockProof = 100
	DefaultBufferInterval           = 5 * time.Second
	DefaultReconnectDelay           = time.Second
	DefaultRelayReSendInterval      = time.Second
)

type SimpleChain struct {
	s       base.Sender
	r       base.Receiver
	w       wallet.Wallet
	src     base.BtpAddress
	acc     *mta.ExtAccumulator
	dst     base.BtpAddress
	bs      *base.BMCLinkStatus //getstatus(dst.src)
	relayCh chan *base.RelayMessage
	l       log.Logger
	cfg     *Config

	rms             []*base.RelayMessage
	rmsMtx          sync.RWMutex
	rmSeq           uint64
	heightOfDst     int64
	lastBlockUpdate *base.BlockUpdate

	bmrIndex       int
	relayble       bool
	relaybleIndex  int
	relaybleHeight int64
}

func (s *SimpleChain) _relayble(rm *base.RelayMessage) bool {
	return s.relayble && (s._overMaxAggregation(rm) || s._lastRelaybleHeight())
}
func (s *SimpleChain) _overMaxAggregation(rm *base.RelayMessage) bool {
	return len(rm.BlockUpdates) >= s.bs.MaxAggregation
}
func (s *SimpleChain) _lastRelaybleHeight() bool {
	return s.relaybleHeight == (s.monitorHeight() + 1)
}
func (s *SimpleChain) _hasWait(rm *base.RelayMessage) bool {
	for _, segment := range rm.Segments {
		if segment != nil && segment.GetResultParam != nil && segment.TransactionResult == nil {
			return true
		}
	}
	return false
}

func (s *SimpleChain) _log(prefix string, rm *base.RelayMessage, segment *base.Segment, segmentIdx int) {
	if segment == nil {
		s.l.Debugf("%s rm:%d bu:%d ~ %d rps:%d",
			prefix,
			rm.Seq,
			rm.BlockUpdates[0].Height,
			rm.BlockUpdates[len(rm.BlockUpdates)-1].Height,
			len(rm.ReceiptProofs))
	} else {
		s.l.Debugf("%s rm:%d [i:%d,h:%d,bu:%d,seq:%d,evt:%d,txh:%v]",
			prefix,
			rm.Seq,
			segmentIdx,
			segment.Height,
			segment.NumberOfBlockUpdate,
			segment.EventSequence,
			segment.NumberOfEvent,
			segment.GetResultParam)
	}
}

func (s *SimpleChain) _relay() {
	s.rmsMtx.RLock()
	defer s.rmsMtx.RUnlock()
	var err error
	for _, rm := range s.rms {
		if (len(rm.BlockUpdates) == 0 && len(rm.ReceiptProofs) == 0) ||
			s._hasWait(rm) || (!s._skippable(rm) && !s._relayble(rm)) {
			break
		} else {
			if len(rm.Segments) == 0 {
				if rm.Segments, err = s.s.Segment(rm, s.bs.Verifier.Height); err != nil {
					s.l.Panicf("fail to segment err:%+v", err)
				}
			}
			//s._log("before relay", rm, nil, -1)
			reSegment := true
			for j, segment := range rm.Segments {
				if segment == nil {
					continue
				}
				reSegment = false

				if segment.GetResultParam == nil {
					segment.TransactionResult = nil
					if segment.GetResultParam, err = s.s.Relay(segment); err != nil {
						s.l.Panicf("fail to Relay err:%+v", err)
					}
					s._log("after relay", rm, segment, j)
					go s.result(rm, segment)
				}
			}
			if reSegment {
				rm.Segments = rm.Segments[:0]
			}
		}
	}
}

func (s *SimpleChain) result(rm *base.RelayMessage, segment *base.Segment) {
	var err error
	segment.TransactionResult, err = s.s.GetResult(segment.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				segment.GetResultParam, ec)
			switch ec.ErrorCode() {
			case base.BMVRevertInvalidSequence, base.BMVRevertInvalidBlockUpdateLower:
				for i := 0; i < len(rm.Segments); i++ {
					if rm.Segments[i] == segment {
						rm.Segments[i] = nil
						break
					}
				}
			case base.BMVRevertInvalidBlockWitnessOld:
				rm.BlockProof, err = s.newBlockProof(rm.BlockProof.BlockWitness.Height, rm.BlockProof.Header)
				s.s.UpdateSegment(rm.BlockProof, segment)
				segment.GetResultParam = nil
			case base.BMVRevertInvalidSequenceHigher, base.BMVRevertInvalidBlockUpdateHigher, base.BMVRevertInvalidBlockProofHigher:
				segment.GetResultParam = nil
			case base.BMCRevertUnauthorized:
				segment.GetResultParam = nil
			default:
				s.l.Panicf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
					segment.GetResultParam, ec)
			}
		} else {
			//TODO: commented temporarily to keep the relayer running
			//s.l.Panicf("fail to GetResult GetResultParam:%v err:%+v",
			//	segment.GetResultParam, err)
			s.l.Debugf("fail to GetResult GetResultParam:%v err:%+v", segment.GetResultParam, err)
		}
	}
}

func (s *SimpleChain) _rm() *base.RelayMessage {
	rm := &base.RelayMessage{
		From:         s.src,
		BlockUpdates: make([]*base.BlockUpdate, 0),
		Seq:          s.rmSeq,
	}
	s.rms = append(s.rms, rm)
	s.rmSeq += 1
	return rm
}

func (s *SimpleChain) addRelayMessage(bu *base.BlockUpdate, rps []*base.ReceiptProof) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()

	if s.lastBlockUpdate != nil {
		//TODO consider remained bu when reconnect
		if s.lastBlockUpdate.Height+1 != bu.Height {
			s.l.Panicf("invalid bu")
		}
	}
	s.lastBlockUpdate = bu
	rm := s.rms[len(s.rms)-1]
	if len(rm.Segments) > 0 {
		rm = s._rm()
	}
	if len(rps) > 0 {
		rm.BlockUpdates = append(rm.BlockUpdates, bu)
		rm.ReceiptProofs = rps
		rm.HeightOfDst = s.monitorHeight()
		if s.bs.BlockIntervalDst > 0 {
			scale := float64(s.bs.BlockIntervalSrc) / float64(s.bs.BlockIntervalDst)
			guessHeightOfDst := s.bs.RxHeight + int64(math.Ceil(float64(bu.Height-s.bs.RxHeightSrc)/scale)) - 1
			if guessHeightOfDst < rm.HeightOfDst {
				rm.HeightOfDst = guessHeightOfDst
			}
		}
		s.l.Debugf("addRelayMessage rms:%d bu:%d rps:%d HeightOfDst:%d", len(s.rms), bu.Height, len(rps), rm.HeightOfDst)
		rm = s._rm()
	} else {
		if bu.Height <= s.bs.Verifier.Height {
			return
		}
		rm.BlockUpdates = append(rm.BlockUpdates, bu)
		s.l.Debugf("addRelayMessage rms:%d bu:%d ~ %d", len(s.rms), rm.BlockUpdates[0].Height, bu.Height)
	}
}

func (s *SimpleChain) updateRelayMessage(h int64, seq int64) (err error) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()

	s.l.Debugf("updateRelayMessage h:%d seq:%d monitorHeight:%d", h, seq, s.monitorHeight())

	rrm := 0
rmLoop:
	for i, rm := range s.rms {
		if len(rm.ReceiptProofs) > 0 {
			rrp := 0
		rpLoop:
			for j, rp := range rm.ReceiptProofs {
				revt := seq - rp.Events[0].Sequence + 1
				if revt < 1 {
					break rpLoop
				}
				if revt >= int64(len(rp.Events)) {
					rrp = j + 1
				} else {
					s.l.Debugf("updateRelayMessage rm:%d bu:%d rp:%d removeEventProofs %d ~ %d",
						rm.Seq,
						rm.BlockUpdates[len(rm.BlockUpdates)-1].Height,
						rp.Index,
						rp.Events[0].Sequence,
						rp.Events[revt-1].Sequence)
					rp.Events = rp.Events[revt:]
					if len(rp.EventProofs) > 0 {
						rp.EventProofs = rp.EventProofs[revt:]
					}
				}
			}
			if rrp > 0 {
				s.l.Debugf("updateRelayMessage rm:%d bu:%d removeReceiptProofs %d ~ %d",
					rm.Seq,
					rm.BlockUpdates[len(rm.BlockUpdates)-1].Height,
					rm.ReceiptProofs[0].Index,
					rm.ReceiptProofs[rrp-1].Index)
				rm.ReceiptProofs = rm.ReceiptProofs[rrp:]
			}
		}
		if rm.BlockProof != nil {
			if len(rm.ReceiptProofs) > 0 {
				if rm.BlockProof, err = s.newBlockProof(rm.BlockProof.BlockWitness.Height, rm.BlockProof.Header); err != nil {
					return
				}
			} else {
				rrm = i + 1
			}
		}
		if len(rm.BlockUpdates) > 0 {
			rbu := h - rm.BlockUpdates[0].Height + 1
			if rbu < 1 {
				break rmLoop
			}
			if rbu >= int64(len(rm.BlockUpdates)) {
				if len(rm.ReceiptProofs) > 0 {
					lbu := rm.BlockUpdates[len(rm.BlockUpdates)-1]
					if rm.BlockProof, err = s.newBlockProof(lbu.Height, lbu.Header); err != nil {
						return
					}
					rm.BlockUpdates = rm.BlockUpdates[:0]
				} else {
					rrm = i + 1
				}
			} else {
				s.l.Debugf("updateRelayMessage rm:%d removeBlockUpdates %d ~ %d",
					rm.Seq,
					rm.BlockUpdates[0].Height,
					rm.BlockUpdates[rbu-1].Height)
				rm.BlockUpdates = rm.BlockUpdates[rbu:]
			}
		}
	}
	if rrm > 0 {
		s.l.Debugf("updateRelayMessage rms:%d removeRelayMessage %d ~ %d",
			len(s.rms),
			s.rms[0].Seq,
			s.rms[rrm-1].Seq)
		s.rms = s.rms[rrm:]
		if len(s.rms) == 0 {
			s._rm()
		}
	}
	return nil
}

func (s *SimpleChain) updateMTA(bu *base.BlockUpdate) {
	next := s.acc.Height() + 1
	if next < bu.Height {
		s.l.Panicf("found missing block next:%d bu:%d", next, bu.Height)
	}
	if next == bu.Height {
		s.acc.AddHash(bu.BlockHash)
		if err := s.acc.Flush(); err != nil {
			//TODO MTA Flush error handling
			s.l.Panicf("fail to MTA Flush err:%+v", err)
		}
		//s.l.Debugf("updateMTA %d", bu.Height)
	}
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
		if err := s.updateRelayMessage(h, seq); err != nil {
			return err
		}
		s.relayCh <- nil
	}
	return nil
}

func (s *SimpleChain) OnBlockOfSrc(bu *base.BlockUpdate, rps []*base.ReceiptProof) {
	s.l.Tracef("OnBlockOfSrc height:%d, bu.Height:%d", s.acc.Height(), bu.Height)
	s.updateMTA(bu)
	s.addRelayMessage(bu, rps)
	s.relayCh <- nil
}

func (s *SimpleChain) newBlockProof(height int64, header []byte) (*base.BlockProof, error) {
	//at := s.bs.Verifier.Height
	//w, err := s.acc.WitnessForWithAccLength(height-s.acc.Offset(), at-s.bs.Verifier.Offset)
	at, w, err := s.acc.WitnessForAt(height, s.bs.Verifier.Height, s.bs.Verifier.Offset)
	if err != nil {
		return nil, err
	}

	s.l.Debugf("newBlockProof height:%d, at:%d, w:%d", height, at, len(w))
	bp := &base.BlockProof{
		Header: header,
		BlockWitness: &base.BlockWitness{
			Height:  at,
			Witness: mta.WitnessesToHashes(w),
		},
	}
	dumpBlockProof(s.acc, height, bp)
	return bp, nil
}

func (s *SimpleChain) prepareDatabase(offset int64) error {
	s.l.Debugln("open database", filepath.Join(s.cfg.AbsBaseDir(), s.cfg.Dst.Address.NetworkAddress()))
	database, err := db.Open(s.cfg.AbsBaseDir(), string(DefaultDBType), s.cfg.Dst.Address.NetworkAddress())
	if err != nil {
		return errors.Wrap(err, "fail to open database")
	}
	defer func() {
		if err != nil {
			database.Close()
		}
	}()
	var bk db.Bucket
	if bk, err = database.GetBucket("Accumulator"); err != nil {
		return err
	}
	k := []byte("Accumulator")
	if offset < 0 {
		offset = 0
	}
	s.acc = mta.NewExtAccumulator(k, bk, offset)
	if bk.Has(k) {
		//offset will be ignore
		if err = s.acc.Recover(); err != nil {
			return errors.Wrapf(err, "fail to acc.Recover cause:%v", err)
		}
		s.l.Debugf("recover Accumulator offset:%d, height:%d", s.acc.Offset(), s.acc.Height())

		//TODO [TBD] sync offset
		//if s.acc.Offset() > offset {
		//	hashes := make([][]byte, s.acc.Offset() - offset)
		//	for i := 0; i < len(hashes); i++ {
		//		hashes[i] = getBlockHashByHeight(offset)
		//		offset++
		//	}
		//	s.acc.AddHashesToHead(hashes)
		//} else if s.acc.Offset() < offset {
		//	s.acc.RemoveHashesFromHead(offset-s.acc.Offset())
		//}
	}
	return nil
}

func (s *SimpleChain) _skippable(rm *base.RelayMessage) bool {
	bs := s.bs
	if len(rm.ReceiptProofs) > 0 {
		if bs.RotateTerm > 0 {
			rotate := 0
			relaybleHeightStart := bs.RotateHeight - int64(bs.RotateTerm+1)
			if rm.HeightOfDst > bs.RotateHeight {
				rotate = int(math.Ceil(float64(rm.HeightOfDst-bs.RotateHeight) / float64(bs.RotateTerm)))
				if rotate > 0 {
					relaybleHeightStart += int64(bs.RotateTerm * (rotate - 1))
				}
			}
			skip := int(math.Ceil(float64(s.monitorHeight()+1-rm.HeightOfDst)/float64(bs.DelayLimit))) - 1
			if skip > 0 {
				rotate += skip
				relaybleHeightStart = rm.HeightOfDst + int64(bs.DelayLimit*skip)
			}
			relaybleIndex := bs.BMRIndex
			if rotate > 0 {
				relaybleIndex += rotate
				if relaybleIndex >= len(bs.BMRs) {
					relaybleIndex = relaybleIndex % len(bs.BMRs)
				}
			}
			prevFinalizeHeight := relaybleHeightStart + int64(s.s.FinalizeLatency())
			return (relaybleIndex == s.bmrIndex) && (prevFinalizeHeight <= s.monitorHeight())
		} else {
			return true
		}
	}
	return false
}

func (s *SimpleChain) _rotate() {
	bs := s.bs
	a := s.w.Address()
	if bs.RotateTerm > 0 {
		//update own bmrIndex of BMRs
		bmrIndex := -1
		for i, bmr := range bs.BMRs {
			if bmr.Address == a {
				bmrIndex = i
				break
			}
		}
		s.bmrIndex = bmrIndex

		//predict index and height of rotate on next block
		rotate := int(math.Ceil(float64(s.monitorHeight()+1-bs.RotateHeight) / float64(bs.RotateTerm)))
		relaybleIndex := bs.BMRIndex
		relaybleHeightEnd := bs.RotateHeight
		if rotate > 0 {
			relaybleIndex += rotate
			if relaybleIndex >= len(bs.BMRs) {
				relaybleIndex = relaybleIndex % len(bs.BMRs)
			}
			relaybleHeightEnd += int64(bs.RotateTerm * rotate)
		}
		prevFinalizeHeight := relaybleHeightEnd - int64(s.bs.RotateTerm) + int64(s.s.FinalizeLatency())
		s.relayble = (relaybleIndex == s.bmrIndex) && (prevFinalizeHeight <= s.monitorHeight())
		s.relaybleIndex = relaybleIndex
		s.relaybleHeight = relaybleHeightEnd
		//b7214314876a73397c07}]
		s.l.Debugf("RefreshStatus %d si:%d status[i:%d rh:%d rxh:%d rxhs:%d] relayble[%v i:%d rh:%d r:%d]",
			s.monitorHeight(),
			s.bmrIndex,
			bs.BMRIndex,
			bs.RotateHeight,
			bs.RxHeight,
			bs.RxHeightSrc,
			s.relayble,
			relaybleIndex,
			relaybleHeightEnd,
			rotate)
	}
}

func (s *SimpleChain) RefreshStatus() error {
	bmcStatus, err := s.s.GetStatus()
	if err != nil {
		return err
	}
	s.bs = bmcStatus
	s._rotate()
	return nil
}

func (s *SimpleChain) init() error {
	if err := s.RefreshStatus(); err != nil {
		return err
	}
	atomic.StoreInt64(&s.heightOfDst, s.bs.CurrentHeight)
	if s.relayCh == nil {
		s.relayCh = make(chan *base.RelayMessage, 2)
		go func() {
			s.l.Debugln("start relayLoop")
			defer func() {
				s.l.Debugln("stop relayLoop")
			}()
			for {
				select {
				case _, ok := <-s.relayCh:
					if !ok {
						return
					}
					s._relay()
				}
			}
		}()
	}
	s.l.Debugf("_init height:%d, dst(%s, height:%d, seq:%d, last:%d), receive:%d",
		s.acc.Height(), s.dst, s.bs.Verifier.Height, s.bs.RxSeq, s.bs.Verifier.LastHeight, s.receiveHeight())
	return nil
}

func (s *SimpleChain) receiveHeight() int64 {
	//min(max(s.acc.Height(), s.bs.Verifier.Offset), s.bs.Verifier.LastHeight)
	max := s.acc.Height()
	if max < s.bs.Verifier.Offset {
		max = s.bs.Verifier.Offset
	}
	max += 1
	min := s.bs.Verifier.LastHeight
	if max < min {
		min = max
	}
	return min
}

func (s *SimpleChain) monitorHeight() int64 {
	return atomic.LoadInt64(&s.heightOfDst)
}

func (s *SimpleChain) Serve() error {
	if err := s.init(); err != nil {
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
			s.receiveHeight(),
			s.bs.RxSeq,
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

func NewChain(cfg *Config, w wallet.Wallet, l log.Logger) (*SimpleChain, error) {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		w:   w,
		l:   l.WithFields(log.Fields{log.FieldKeyChain:
		//fmt.Sprintf("%s->%s", cfg.Src.Address.NetworkAddress(), cfg.Dst.Address.NetworkAddress())}),
		fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		rms: make([]*base.RelayMessage, 0),
	}
	s._rm()

	s.s, s.r = newSenderAndReceiver(cfg, w, l)

	if err := s.prepareDatabase(cfg.Offset); err != nil {
		return nil, err
	}
	return s, nil
}

func dumpBlockProof(acc *mta.ExtAccumulator, height int64, bp *base.BlockProof) {
	if n, err := acc.GetNode(height); err != nil {
		fmt.Printf("height:%d, accLen:%d, err:%+v", height, acc.Len(), err)
	} else {
		fmt.Printf("height:%d, accLen:%d, hash:%s\n", height, acc.Len(), hex.EncodeToString(n.Hash()))
	}

	fmt.Print("dumpBlockProof.height:", bp.BlockWitness.Height, ",witness:[")
	for _, w := range bp.BlockWitness.Witness {
		fmt.Print(hex.EncodeToString(w), ",")
	}
	fmt.Println("]")
	b, _ := codec.RLP.MarshalToBytes(bp)
	fmt.Println(base64.URLEncoding.EncodeToString(b))
}
