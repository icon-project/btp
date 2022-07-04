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
	"fmt"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	"github.com/icon-project/btp/common/mbt"
	"math"
	"math/big"
	"path/filepath"
	"sync"
	"sync/atomic"
	"time"
	"unsafe"

	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

const (
	DefaultDBDir  = "db"
	DefaultDBType = db.GoLevelDBBackend
	//Base64 in:out = 6:8
	DefaultBufferScaleOfBlockProof  = 0.5
	DefaultBufferNumberOfBlockProof = 100
	DefaultBufferInterval           = 5 * time.Second
	DefaultReconnectDelay           = time.Second
	DefaultRelayReSendInterval      = time.Second
)

type chainInfo struct {
	Src             module.BtpAddress
	Bucket          db.Bucket
	StartHeight     int64
	NetworkTypeName string
	ch              *chainHeight
}

type chainHeight struct {
	srcHeight int64
	dstHeight int64
}

func (r *chainInfo) srcHeight() int64 {
	return r.ch.srcHeight
}

func (r *chainInfo) dstHeight() int64 {
	return r.ch.dstHeight
}

func (r *chainInfo) Recover() error {
	b, err := r.Bucket.Get([]byte(r.Src.String()))
	if err != nil {
		return err
	}

	if len(b) == 0 {
		r.ch.srcHeight = 0
		r.ch.dstHeight = 0
	}
	var ch chainHeight
	if _, err = codec.RLP.UnmarshalFromBytes(b, &ch); err != nil {
		return err
	}
	r.ch.dstHeight = ch.dstHeight
	r.ch.srcHeight = ch.srcHeight

	return nil
}

func (r *chainInfo) Update(sh int64, dh int64) error {
	var s chainHeight
	s.srcHeight = sh
	s.dstHeight = dh

	if bs, err := codec.RLP.MarshalToBytes(&s); err != nil {
		return err
	} else {
		return r.Bucket.Set([]byte(r.Src.String()), bs)
	}
}

func newRelayHeight(sh int64, dh int64, bk db.Bucket) *chainInfo {
	return &chainInfo{
		ch: &chainHeight{
			dstHeight: dh,
			srcHeight: sh,
		},
		Bucket: bk,
	}
}

type SimpleChain struct {
	s  module.Sender
	r  *icon.Receiver
	ci *chainInfo
	w  wallet.Wallet

	src module.BtpAddress
	dst module.BtpAddress

	bs  *module.BMCLinkStatus //getstatus(dst.src)
	l   log.Logger
	cfg *Config

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

func (s *SimpleChain) _relayble(rm *module.RelayMessage) bool {
	return s.relayble && (s._overMaxAggregation(rm) || s._lastRelaybleHeight())
}
func (s *SimpleChain) _overMaxAggregation(rm *module.RelayMessage) bool {
	return len(rm.BlockUpdates) >= s.bs.MaxAggregation
}
func (s *SimpleChain) _lastRelaybleHeight() bool {
	return s.relaybleHeight == (s.monitorHeight() + 1)
}

func (s *SimpleChain) _log(prefix string, rm *module.RelayMessage, segment *module.Segment, segmentIdx int) {
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

func (s *SimpleChain) relay() error {
	s.rmsMtx.RLock()
	defer s.rmsMtx.RUnlock()
	//TODO txlimit check
	for i := 0; i < len(s.rms); i++ {
		//s._log("before relay", rm, nil, -1)
		b, err := codec.RLP.MarshalToBytes(s.rms[i])
		if err != nil {
			return err
		}
		segment := &module.Segment{
			TransactionParam: b,
		}

		if segment.GetResultParam == nil {
			segment.TransactionResult = nil
			if segment.GetResultParam, err = s.s.Relay(segment); err != nil {
				s.l.Panicf("fail to Relay err:%+v", err)
			}
			//s._log("after relay", rm, segment, j)
			go s.result(i, segment)
		}
	}
	return nil
}

func (s *SimpleChain) segment() error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	for bdIndex := len(s.bds) - 1; bdIndex > -1; bdIndex-- {
		if len(s.rms) == 0 || s.isOverLimit(s.rms[len(s.rms)-1].Size()) {
			s.rms = append(s.rms, icon.NewRelayMessage())
		}

		//blockUpdate
		//skipped first btp block
		if s.bds[bdIndex].Bu.MainHeight != s.ci.StartHeight {
			if s.isOverLimit(s.rms[len(s.rms)-1].Size() + int(unsafe.Sizeof(s.bds[bdIndex].Bu))) {
				s.rms = append(s.rms, icon.NewRelayMessage())
			}
			tpm, err := icon.NewTypePrefixedMessage(s.bds[bdIndex].Bu)
			if err != nil {
				return err
			}

			s.rms[len(s.rms)-1].SetHeight(s.bds[bdIndex].Bu.MainHeight)
			s.rms[len(s.rms)-1].AppendMessage(tpm)
		}

		//messageProof
		var endIndex int
		for endIndex = s.bds[bdIndex].PartialOffset + 1; endIndex < s.bds[bdIndex].Mt.Len(); endIndex++ {
			//TODO refactoring
			p, err := s.bds[bdIndex].Mt.Proof(s.bds[bdIndex].PartialOffset+1, endIndex)
			if err != nil {
				return err
			}
			b, err := codec.RLP.MarshalToBytes(p)
			//TODO refactoring
			//if s.isOverLimit(s.rms[len(s.rms)-1].Size() + s.bds[bdIndex].Mt.ProofLength(s.bds[bdIndex].PartialOffset, endIndex)) {
			if s.isOverLimit(s.rms[len(s.rms)-1].Size() + len(b)) {
				tpm, err := icon.NewTypePrefixedMessage(*p)
				if err != nil {
					return err
				}
				s.bds[bdIndex].PartialOffset = endIndex

				s.rms[len(s.rms)-1].SetHeight(s.bds[bdIndex].Bu.MainHeight)
				s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
				s.rms[len(s.rms)-1].AppendMessage(tpm)
				s.rms = append(s.rms, icon.NewRelayMessage())
			}
		}

		if s.bds[bdIndex].PartialOffset != s.bds[bdIndex].Mt.Len() {
			s.setRelayMessageForMessageProof(bdIndex, s.bds[bdIndex].Mt.Len())
			s.bds[bdIndex].PartialOffset = s.bds[bdIndex].Mt.Len()
		}

	}
	return nil
}

func (s *SimpleChain) setRelayMessageForMessageProof(bdIndex int, endIndex int) error {
	p, err := s.bds[bdIndex].Mt.Proof(s.bds[bdIndex].PartialOffset+1, endIndex)
	if err != nil {
		return err
	}

	tpm, err := icon.NewTypePrefixedMessage(*p)
	if err != nil {
		return err
	}

	s.rms[len(s.rms)-1].SetHeight(s.bds[bdIndex].Bu.MainHeight)
	s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
	s.rms[len(s.rms)-1].AppendMessage(tpm)
	return nil
}

func (s *SimpleChain) result(index int, segment *module.Segment) {
	var err error
	segment.TransactionResult, err = s.s.GetResult(segment.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				segment.GetResultParam, ec)
			switch ec.ErrorCode() {
			case module.BMVRevertInvalidSequence, module.BMVRevertInvalidBlockUpdateLower:
				for i := 0; i < len(s.bds); i++ {
					if s.bds[i].Bu.MainHeight == s.rms[index].Height() {
						if s.bds[i].PartialOffset == s.rms[index].MessageSeq() {
							s.bds = s.bds[i:]
						} else {
							s.bds[i].PartialOffset = s.rms[index].MessageSeq()
						}
						break
					}
				}
				s.rms = s.rms[index:]
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

func (s *SimpleChain) addRelayMessage(bu *icon.BTPBlockHeader) error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()

	if bu.Proof == nil {
		//TODO refactoring base64 decoding
		bu.Proof, _ = s.r.GetBTPProof(bu, s.cfg.Nid) // TODO error refactoring
	}
	var mt *mbt.MerkleBinaryTree
	if bu.MessageCount > 0 {
		m, err := s.r.GetBTPMessage(bu, s.cfg.Nid)
		if err != nil {
			return err
		}
		//TODO refactoring base64 decoding
		if mt, err = mbt.NewMerkleBinaryTree(mbt.HashFuncByUID(s.ci.NetworkTypeName), m); err != nil {
			return err
		}
	}

	btpBlock := &icon.BTPBlockData{
		Bu: bu,
		Mt: mt, PartialOffset: 0,
		Seq:         s.rmSeq,
		HeightOfDst: s.monitorHeight(),
		HeightOfSrc: bu.MainHeight}

	//TODO refactoring
	if s.bs.BlockIntervalDst > 0 {
		scale := float64(s.bs.BlockIntervalSrc) / float64(s.bs.BlockIntervalDst)
		guessHeightOfDst := s.bs.RxHeight + int64(math.Ceil(float64(bu.MainHeight-s.bs.RxHeightSrc)/scale)) - 1
		if guessHeightOfDst < btpBlock.HeightOfDst {
			btpBlock.HeightOfDst = guessHeightOfDst
		}
	}

	s.bds = append(s.bds, btpBlock)
	s.ci.Update(btpBlock.Bu.MainHeight, s.ci.ch.dstHeight)
	return nil
}

func bigIntSubstract(x *big.Int, y *big.Int) int64 {
	var ret big.Int
	ret.Sub(x, y)
	if !ret.IsInt64() {
		panic("overflow")
	}
	return ret.Int64()
}

func (s *SimpleChain) updateRelayMessage(h int64, seq *big.Int) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	s.l.Debugf("updateRelayMessage h:%d seq:%d monitorHeight:%d", h, seq, s.monitorHeight())
	rmIndex := 0
	bdIndex := 0
	for i, rm := range s.rms {
		if rm.Height() == h && rm.MessageSeq() == int(seq.Int64()) {
			rmIndex = i
		}
	}

	for i, bd := range s.bds {
		if bd.Bu.MainHeight == h {
			bdIndex = i
		}
	}

	s.rms = s.rms[rmIndex:]
	s.bds = s.bds[bdIndex:]
	s.ci.Update(s.ci.ch.srcHeight, h)
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

func (s *SimpleChain) OnBlockOfSrc(bu *icon.BTPBlockHeader) error {
	s.l.Tracef("OnBlockOfSrc height:%d, bu.Height:%d", s.ci.srcHeight(), bu.MainHeight)
	if s.ci.StartHeight == 0 {
		s.SetChainInfo()
	}

	if s.relayble {
		s.addRelayMessage(bu)
		s.segment()
		s.relay()
	}
	return nil
}

func (s *SimpleChain) prepareDatabase() error {
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
	if bk, err = database.GetBucket("relayHeight"); err != nil {
		return err
	}
	k := []byte("relayHeight")
	s.ci = newRelayHeight(0, 0, bk)
	if bk.Has(k) {
		//offset will be ignore
		if err = s.ci.Recover(); err != nil {
			return errors.Wrapf(err, "fail to acc.Recover cause:%v", err)
		}
		s.l.Debugf("recover relayHeight srcHeight:%d, dstHeight:%d", s.ci.srcHeight(), s.ci.dstHeight())
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
	s.l.Debugf("_init height:%d, dst(%s, src height:%d, seq:%d, last:%d), receive:%d",
		s.ci.srcHeight(), s.dst, s.bs.Verifier.Height, s.bs.RxSeq, s.bs.Verifier.LastHeight, s.receiveHeight())
	return nil
}

func (s *SimpleChain) receiveHeight() int64 {
	//min(max(s.acc.Height(), s.bs.Verifier.Offset), s.bs.Verifier.LastHeight)
	//TODO refactoring

	//max := s.ci.dstHeight()
	//if max < s.bs.Verifier.Offset {
	//	max = s.bs.Verifier.Offset
	//}
	//max += 1
	//min := s.bs.Verifier.LastHeight
	//if max < min {
	//	min = max
	//}
	//return min

	return s.ci.ch.srcHeight
}

func (s *SimpleChain) monitorHeight() int64 {
	return atomic.LoadInt64(&s.heightOfDst)
}

func (s *SimpleChain) Serve(sender module.Sender) error {
	s.s = sender
	s.r = icon.NewReceiver(s.src, s.dst, s.cfg.Src.Endpoint, s.cfg.Src.Options, s.l)
	if err := s.prepareDatabase(); err != nil {
		return err
	}

	//TODO for test
	s.relayble = true

	s.SetChainInfo()
	if s.ci.ch.srcHeight < 1 {
		s.ci.ch.srcHeight = s.ci.StartHeight
	}

	if err := s.Monitoring(); err != nil {
		return err
	}

	return nil
}
func (s *SimpleChain) SetChainInfo() error {
	ni, err := s.r.GetBTPNetworkInfo(s.cfg.Nid)
	if err != nil {
		return err
	}
	sh, err := ni.StartHeight.Value()
	s.ci.StartHeight = sh
	s.ci.NetworkTypeName = ni.NetworkTypeName
	return nil
}

func (s *SimpleChain) Monitoring() error {
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
			s.cfg.Nid,
			s.cfg.proofFlag,
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

func NewSimpleChain(cfg *Config, w wallet.Wallet, l log.Logger) *SimpleChain {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		w:   w,
		l:   l.WithFields(log.Fields{log.FieldKeyChain: fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		bds: make([]*icon.BTPBlockData, 0),
		rms: make([]*icon.BTPRelayMessage, 0),
	}

	return s
}
