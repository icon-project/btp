/*
 * Copyright 2022 ICON Foundation
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

package main

import (
	"fmt"
	"sync"
	"unsafe"

	"github.com/icon-project/btp/cmd/bridge/module"
	"github.com/icon-project/btp/cmd/bridge/module/evmbridge"
	"github.com/icon-project/btp/cmd/bridge/module/iconbridge"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
)

type bridge struct {
	s module.Sender
	r module.Receiver
	w module.Wallet

	src module.BtpAddress
	dst module.BtpAddress

	bs  *module.BMCLinkStatus //getstatus(dst.src)
	l   log.Logger
	cfg *module.Config

	ssMtx  sync.RWMutex
	ss     []*module.Segment
	rm     *module.RelayMessage
	rmSize int
}

func (c *bridge) _log(prefix string, segment *module.Segment) {
	c.l.Debugf("%s height:%d,seq:%d,evt:%d,txh:%v]",
		prefix,
		segment.Height,
		segment.EventSequence,
		segment.NumberOfEvent,
		segment.GetResultParam)
}

func (c *bridge) relay() {
	c.ssMtx.RLock()
	defer c.ssMtx.RUnlock()

	var err error
	for _, s := range c.ss {
		c._log("before relay", s)
		//if wait for result
		if s.GetResultParam != nil && s.TransactionResult == nil {
			return
		}
		if s.GetResultParam == nil {
			s.TransactionResult = nil
			if s.GetResultParam, err = c.s.Relay(s); err != nil {
				c.l.Panicf("fail to Relay err:%+v", err)
			}
			c._log("after relay", s)
			go c.result(s)
		}
	}
}

func (c *bridge) result(s *module.Segment) {
	var err error
	s.TransactionResult, err = c.s.GetResult(s.GetResultParam)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			c.l.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
				s.GetResultParam, ec)
			switch ec.ErrorCode() {
			default:
				c.l.Panicf("fail to GetResult GetResultParam:%v ErrorCoder:%+v",
					s.GetResultParam, ec)
			}
		} else {
			c.l.Panicf("fail to GetResult GetResultParam:%v err:%+v",
				s.GetResultParam, err)
		}
	}
}

func (c *bridge) isOverLimit(size int) bool {
	return c.s.TxSizeLimit() < size
}

func (c *bridge) OnBlockOfDst(bs *module.BMCLinkStatus) error {
	c.l.Tracef("OnBlockOfDst height:%d", bs.CurrentHeight)
	if bs.Verifier.Height != c.bs.Verifier.Height || bs.RxSeq != c.bs.RxSeq {
		c.l.Debugf("OnBlockOfDst h:%d seq:%d monitorHeight:%d",
			c.bs.Verifier.Height, c.bs.RxSeq, bs.CurrentHeight)
		r := 0
		for i, s := range c.ss {
			if s.EventSequence <= bs.RxSeq {
				r = i + 1
			}
		}
		c.removeSegment(r)
	}
	c.bs = bs
	return nil
}

func (c *bridge) removeSegment(offset int) {
	c.ssMtx.Lock()
	defer c.ssMtx.Unlock()

	if offset < 1 {
		return
	}
	c.l.Debugf("removeSegment ss:%d removeRelayMessage %d ~ %d",
		len(c.ss),
		c.ss[0].EventSequence,
		c.ss[offset-1].EventSequence)
	c.ss = c.ss[offset:]
}

type Receipt struct {
	Index  uint64
	Events []byte
	Height uint64
}

func (c *bridge) addSegment() error {
	c.ssMtx.Lock()
	defer c.ssMtx.Unlock()

	rm := &struct {
		Receipts [][]byte
	}{
		Receipts: make([][]byte, 0),
	}
	var (
		b   []byte
		err error
	)
	numOfEvents := 0
	for _, rp := range c.rm.ReceiptProofs {
		if len(rp.Events) == 0 {
			continue
		}
		numOfEvents += len(rp.Events)
		if b, err = codec.RLP.MarshalToBytes(rp.Events); err != nil {
			return err
		}
		r := &struct {
			Index  int64
			Events []byte
			Height int64
		}{
			Index:  rp.Index,
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
	lrp := c.rm.ReceiptProofs[len(c.rm.ReceiptProofs)-1]
	le := lrp.Events[len(lrp.Events)-1]
	s := &module.Segment{
		TransactionParam: b,
		//
		From:          c.src,
		Height:        lrp.Height,
		EventSequence: le.Sequence,
		NumberOfEvent: numOfEvents,
	}
	c.ss = append(c.ss, s)
	lrp.Events = lrp.Events[:0]
	c.rm.ReceiptProofs[0] = lrp
	c.rm.ReceiptProofs = c.rm.ReceiptProofs[:1]
	c.rmSize = 0
	return nil
}

func sizeOfEvent(rp *module.Event) int {
	return int(unsafe.Sizeof(rp))
}

func (c *bridge) OnBlockOfSrc(rps []*module.ReceiptProof) error {
	c.l.Debugf("OnBlockOfSrc rps:%d", len(rps))
	var err error
	for _, rp := range rps {
		trp := &module.ReceiptProof{
			Index:  rp.Index,
			Events: make([]*module.Event, 0),
			Height: rp.Height,
		}
		c.rm.ReceiptProofs = append(c.rm.ReceiptProofs, trp)

		for _, e := range rp.Events {
			size := sizeOfEvent(e)
			if c.isOverLimit(c.rmSize+size) && c.rmSize > 0 {
				if err = c.addSegment(); err != nil {
					return err
				}
			}
			trp.Events = append(trp.Events, e)
			c.rmSize += size
		}

		//last event
		if c.isOverLimit(c.rmSize) {
			if err = c.addSegment(); err != nil {
				return err
			}
		}

		//remove last receipt if empty
		if len(trp.Events) == 0 {
			c.rm.ReceiptProofs = c.rm.ReceiptProofs[:len(c.rm.ReceiptProofs)-1]
		}
	}

	if !c.cfg.MaxSizeTx {
		//immediately relay
		if c.rmSize > 0 {
			if err = c.addSegment(); err != nil {
				return err
			}
		}
	}
	c.relay()
	return nil
}

func (c *bridge) ReceiveLoop(height, seq int64, errCh chan<- error) {
	err := c.r.ReceiveLoop(
		height,
		seq,
		c.OnBlockOfSrc,
		func() {
			c.l.Debugf("Source %s, height:%d, seq:%d",
				c.src, height, seq)
		})
	select {
	case errCh <- err:
	default:
	}
}

func (c *bridge) Serve() error {
	errCh := make(chan error)
	var once sync.Once
	go func() {
		err := c.s.MonitorLoop(func(bs *module.BMCLinkStatus) error {
			once.Do(func() {
				c.bs = bs
				c.l.Debugf("Destination %s, height:%d",
					c.dst, c.bs.CurrentHeight)
				go c.ReceiveLoop(c.bs.Verifier.Height, c.bs.RxSeq, errCh)
			})
			return c.OnBlockOfDst(bs)
		})
		select {
		case errCh <- err:
		default:
		}
	}()

	for {
		select {
		case err := <-errCh:
			return err
		}
	}
}

func NewBridge(cfg *module.Config, ks, pw []byte, l log.Logger) (*bridge, error) {
	c := &bridge{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		cfg: cfg,
		rm: &module.RelayMessage{
			ReceiptProofs: make([]*module.ReceiptProof, 0),
		},
		ss: make([]*module.Segment, 0),
	}

	var err error
	if c.w, err = newWallet(cfg, ks, pw); err != nil {
		return nil, err
	}
	c.l = l.WithFields(log.Fields{
		log.FieldKeyWallet: c.w.Address()[2:],
		log.FieldKeyChain:  fmt.Sprintf("%s", cfg.Dst.Address.NetworkID()),
	})
	if c.r, err = newReceiver(cfg, c.l); err != nil {
		return nil, err
	}
	if c.s, err = newSender(cfg, c.w, c.l); err != nil {
		return nil, err
	}
	return c, nil
}

const (
	chainNameIcon = "icon"
	chainNameBsc  = "bsc"
)

func newWallet(cfg *module.Config, ks, pw []byte) (w module.Wallet, err error) {
	switch cfg.Dst.Address.BlockChain() {
	case chainNameIcon:
		return iconbridge.NewWallet(ks, pw)
	case chainNameBsc:
		return evmbridge.NewWallet(ks, pw)
	default:
		err = errors.Errorf("not supported wallet %s", cfg.Dst.Address.BlockChain())
	}
	return
}

func newReceiver(cfg *module.Config, l log.Logger) (r module.Receiver, err error) {
	switch cfg.Src.Address.BlockChain() {
	case chainNameIcon:
		r = iconbridge.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Src.Options, l)
	case chainNameBsc:
		r = evmbridge.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Src.Options, l)
	default:
		err = errors.Errorf("not supported receiver %s", cfg.Src.Address.BlockChain())
	}
	return
}

func newSender(cfg *module.Config, w module.Wallet, l log.Logger) (s module.Sender, err error) {
	switch cfg.Dst.Address.BlockChain() {
	case chainNameIcon:
		s = iconbridge.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Src.Options, l)
	case chainNameBsc:
		s = evmbridge.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, nil, l)
	default:
		err = errors.Errorf("not supported sender %s", cfg.Dst.Address.BlockChain())
	}
	return
}
