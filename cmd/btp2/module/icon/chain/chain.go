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
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
	"github.com/icon-project/btp/common/wallet"
	"math/big"
	"path/filepath"
	"sync"
	"sync/atomic"
	"time"
	"unsafe"
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
	if sh == 0 {
		s.srcHeight = r.ch.srcHeight
	} else {
		s.srcHeight = sh
	}

	if dh == 0 {
		s.dstHeight = r.ch.dstHeight
	} else {
		s.dstHeight = dh
	}

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
	if s.cfg.TxPoolFlag {
		rmSize = len(s.rms) - 1
	} else {
		rmSize = len(s.rms)
	}

	for i := 0; i < rmSize; i++ {
		s._log("before relay", s.rms[i], nil, -1)
		b, err := codec.RLP.MarshalToBytes(s.rms[i])
		if err != nil {
			return err
		}
		segment := &module.Segment{
			TransactionParam: b,
			Height:           s.rms[i].Height(),
		}

		if segment.GetResultParam == nil {
			segment.TransactionResult = nil
			if segment.GetResultParam, err = s.s.Relay(segment); err != nil {
				s.l.Panicf("fail to Relay err:%+v", err)
			}
			s._log("after relay", s.rms[i], segment, -1)
			go s.result(i, segment)
		}
	}
	s.rms = append(s.rms[:0], s.rms[0+rmSize:]...)
	return nil
}

func (s *SimpleChain) segment() error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	bdsLen := len(s.bds) - 1
	if len(s.rms) == 0 || s.isOverLimit(s.rms[len(s.rms)-1].Size()) {
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
		if s.isOverLimit(s.rms[len(s.rms)-1].Size() + int(unsafe.Sizeof(bu))) {
			s.rms = append(s.rms, icon.NewRelayMessage())
		}
		tpm, err := icon.NewTypePrefixedMessage(bu)
		if err != nil {
			return err
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
		b, err := codec.RLP.MarshalToBytes(p)
		//if s.isOverLimit(s.rms[len(s.rms)-1].Size() + s.bds[bdIndex].Mt.ProofLength(s.bds[bdIndex].PartialOffset, endIndex)) {
		if s.isOverLimit(s.rms[len(s.rms)-1].Size() + len(b)) {
			tpm, err := icon.NewTypePrefixedMessage(*p)
			if err != nil {
				return err
			}
			partialOffset = endIndex

			s.rms[len(s.rms)-1].SetHeight(heightOfSrc)
			s.rms[len(s.rms)-1].SetMessageSeq(endIndex)
			s.rms[len(s.rms)-1].AppendMessage(tpm)
			s.rms = append(s.rms, icon.NewRelayMessage())
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
			switch ec.ErrorCode() {
			case module.BMVRevertInvalidSequence, module.BMVRevertInvalidBlockUpdateLower:
				for i := 0; i < len(s.bds); i++ {
					if s.bds[i].HeightOfSrc == s.rms[index].Height() {
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

func (s *SimpleChain) addRelayMessage(bu *icon.BTPBlockUpdate, bh *icon.BTPBlockHeader) error {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()

	if len(bu.BTPBlockProof) == 0 {
		p, err := s.r.GetBTPProof(bh.MainHeight, s.cfg.Nid)
		if err != nil {
			return err
		}
		bu.BTPBlockProof = p
	}
	var mt *mbt.MerkleBinaryTree
	if bh.MessageCount > 0 {
		m, err := s.r.GetBTPMessage(bh.MainHeight, s.cfg.Nid)
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
	s.ci.Update(bh.MainHeight, s.ci.ch.dstHeight)
	return nil
}

func (s *SimpleChain) updateRelayMessage(h int64, seq *big.Int) {
	s.rmsMtx.Lock()
	defer s.rmsMtx.Unlock()
	s.l.Debugf("updateRelayMessage h:%d seq:%d monitorHeight:%d", h, seq, s.monitorHeight())
	bdIndex := 0

	for i, bd := range s.bds {
		if bd.HeightOfSrc == h {
			bdIndex = i
		}
	}

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

func (s *SimpleChain) OnBlockOfSrc(bu *icon.BTPBlockUpdate) error {
	bh := &icon.BTPBlockHeader{}
	_, err := codec.RLP.UnmarshalFromBytes(bu.BTPBlockHeader, bh)
	if err != nil {
		return err
	}

	s.l.Tracef("OnBlockOfSrc height:%d, bu.Height:%d", s.ci.srcHeight(), bh.MainHeight)
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
	return nil
}

func (s *SimpleChain) receiveHeight() (int64, error) {
	if s.bs.Verifier.Height == s.ci.StartHeight {
		return s.ci.ch.srcHeight, nil
	} else {
		if len(s.rms) == 0 {
			s.rms = append(s.rms, icon.NewRelayMessage())
		}
		h, err := s.r.GetBTPBlockHeader(s.bs.Verifier.Height, s.cfg.Nid)
		if err != nil {
			return 0, err
		}
		bh := &icon.BTPBlockHeader{}
		_, err = codec.RLP.UnmarshalFromBytes(h, bh)
		if err != nil {
			return 0, err
		}
		index := (s.bs.RxSeq.Int64() - s.bs.Verifier.Sequence_offset) - s.bs.Verifier.First_message_sn
		if index < bh.MessageCount {
			var mt *mbt.MerkleBinaryTree
			m, err := s.r.GetBTPMessage(bh.MainHeight, s.cfg.Nid)
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
	if err := s.prepareDatabase(); err != nil {
		return err
	}

	//TODO Pre rotation settings
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
		s.ci.srcHeight(), s.dst, s.bs.Verifier.Height, s.bs.RxSeq, s.bs.Verifier.LastHeight, h)

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
			s.cfg.Nid,
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

func NewChain(cfg *module.Config, w wallet.Wallet, l log.Logger) *SimpleChain {
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
