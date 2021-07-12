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

package btp

import (
	"math"
	"sync"
	"sync/atomic"
	"time"

	"github.com/gammazero/workerpool"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/chain/pra"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
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
	DefaultMaxWorkers               = 100
)

// BTP is the core to manage relaying messages
type BTP struct {
	cfg              *Config
	wallet           wallet.Wallet
	dir              string
	log              log.Logger
	sender           chain.Sender
	receiver         chain.Receiver
	bmcLinkStatus    *chain.BMCLinkStatus
	heightOfDst      int64
	relaySignal      chan bool
	bmcSrcBtpAddress chain.BtpAddress
	bmcDstBtpAddress chain.BtpAddress
	store            *mta.ExtAccumulator
	rms              []*chain.RelayMessage
	rmsMutex         *sync.RWMutex
	rmSeq            uint64
	lastBlockUpdate  *chain.BlockUpdate
	wp               *workerpool.WorkerPool // Use worker pool to avoid too many open files error
}

// New creates a new BTP object
func New(cfg *Config, w wallet.Wallet, l log.Logger) (*BTP, error) {

	var sender chain.Sender
	var receiver chain.Receiver

	switch cfg.Dst.Address.BlockChain() {
	case "icon":
		sender = icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Dst.Options, l)
	case "pra":
		sender = pra.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, nil, l)
	default:
		return nil, errors.New("Chain not supported yet")
	}

	switch cfg.Src.Address.BlockChain() {
	case "icon":
		receiver = icon.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, nil, l)
	case "pra":
		receiver = pra.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Dst.Options, l)
	default:
		return nil, errors.New("Chain not supported yet")
	}

	return &BTP{
		cfg:              cfg,
		wallet:           w,
		dir:              cfg.BaseDir,
		log:              l,
		sender:           sender,
		receiver:         receiver,
		relaySignal:      make(chan bool, 2),
		bmcSrcBtpAddress: cfg.Src.Address,
		bmcDstBtpAddress: cfg.Dst.Address,
		rms:              []*chain.RelayMessage{},
		rmsMutex:         &sync.RWMutex{},
		wp:               workerpool.New(DefaultMaxWorkers),
	}, nil
}

func (b *BTP) init() error {
	if err := b.prepareDatabase(b.cfg.Offset); err != nil {
		return err
	}

	if err := b.refreshStatus(); err != nil {
		return err
	}
	atomic.StoreInt64(&b.heightOfDst, b.bmcLinkStatus.CurrentHeight)
	b.relayLoop()
	b.newRelayMessage()
	return nil
}

// Serve starts the BTP
func (b *BTP) Serve() error {
	log.Info("Starting BTP...")
	if err := b.init(); err != nil {
		return err
	}

	errCh := make(chan error)
	go func() {
		err := b.sender.MonitorLoop(
			b.bmcLinkStatus.CurrentHeight,
			b.OnBlockOfDst,
			func() {
				b.log.Debugf("Connect MonitorLoop")
				errCh <- nil
			})
		select {
		case errCh <- err:
		default:
		}
	}()
	go func() {
		h := b.receiveHeight()
		b.log.Debugf("start receiveloop from heigh: %v", h)

		err := b.receiver.ReceiveLoop(
			h,
			b.bmcLinkStatus.RxSeq,
			b.OnBlockOfSrc,
			func() {
				b.log.Debug("Connect ReceiveLoop")
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

// OnBlockOfDst callback when got a new update from the BMC source
func (b *BTP) OnBlockOfSrc(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
	b.log.Tracef("OnBlockOfSrc bu.Height:%d", bu.Height)
	b.updateMTA(bu)
	b.addRelayMessage(bu, rps)
	b.relaySignal <- true
}

// OnBlockOfDst callback when got a new update from the BMC destination
func (b *BTP) OnBlockOfDst(height int64) error {
	b.log.Debugf("OnBlockOfDst height:%d", height)
	atomic.StoreInt64(&b.heightOfDst, height)

	h, seq := b.bmcLinkStatus.Verifier.Height, b.bmcLinkStatus.RxSeq
	if err := b.refreshStatus(); err != nil {
		return err
	}

	if h != b.bmcLinkStatus.Verifier.Height || seq != b.bmcLinkStatus.RxSeq {
		if err := b.updateRelayMessages(b.bmcLinkStatus.Verifier.Height, b.bmcLinkStatus.RxSeq); err != nil {
			return err
		}
		b.relaySignal <- true
	}
	return nil
}

// refreshStatus update current status of BMCLink
func (b *BTP) refreshStatus() error {
	bmcStatus, err := b.sender.GetStatus()
	if err != nil {
		return err
	}
	b.bmcLinkStatus = bmcStatus
	return nil
}

// relayLoop listen a signal to relay messages
func (b *BTP) relayLoop() {
	go func() {
		b.log.Debugln("start relayLoop")
		for {
			_, ok := <-b.relaySignal
			if !ok {
				break
			}
			b.relay()
		}
		b.log.Debugln("stop relayLoop")
	}()
}

// relay relays messages are in the buffered rms
func (b *BTP) relay() {
	b.rmsMutex.Lock()
	defer b.rmsMutex.Unlock()

	if err := b.refreshStatus(); err != nil {
		b.log.Panicf("fail to refresh status err:%+v", err)
	}

	for _, rm := range b.rms {
		hasWait := rm.HasWait()
		skippable := b.skippable(rm)
		relayable := b.relayble(rm)

		b.log.Debugf("Relay rms:%v has_wait:%v skippable:%v relayable:%v", len(b.rms), hasWait, skippable, relayable)

		if hasWait || (!skippable && !relayable) {
			break
		} else {
			if len(rm.Segments) == 0 {
				if segments, err := b.sender.Segment(rm, b.bmcLinkStatus.Verifier.Height); err != nil {
					b.log.Panicf("fail to segment err:%+v", err)
				} else {
					rm.Segments = segments
				}
			}

			b.logRelaying("before relay", rm, nil, -1)
			reSegment := true
			for j, segment := range rm.Segments {
				if segment == nil {
					continue
				}
				reSegment = false

				if segment.GetResultParam == nil {
					segment.TransactionResult = nil
					if resultParam, err := b.sender.Relay(segment); err != nil {
						b.log.Panicf("fail to Relay err:%+v", err)
					} else {
						segment.GetResultParam = resultParam
					}

					b.logRelaying("after relay", rm, segment, j)
					b.updateResult(rm, segment)

				}
			}

			if reSegment {
				rm.Segments = rm.Segments[:0]
			}
		}
	}

}

// addRelayMessage adds messages to the buffered rms
func (b *BTP) addRelayMessage(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
	b.log.Debugf("addRelayMessage bu.Height:%v b.bmcLinkStatus.Verifier.Height:%v", bu.Height, b.bmcLinkStatus.Verifier.Height)

	if b.lastBlockUpdate != nil {
		//TODO consider remained bu when reconnect
		if b.lastBlockUpdate.Height+1 != bu.Height {
			b.log.Panicf("invalid bu")
		}
	}
	b.lastBlockUpdate = bu

	rm := b.rms[len(b.rms)-1]
	if len(rm.Segments) > 0 {
		rm = b.newRelayMessage()
	}

	if len(rps) > 0 {
		rm.BlockUpdates = append(rm.BlockUpdates, bu)

		rm.ReceiptProofs = rps
		rm.HeightOfDst = b.HeightOfDst()
		if b.bmcLinkStatus.BlockIntervalDst > 0 {
			scale := float64(b.bmcLinkStatus.BlockIntervalSrc) / float64(b.bmcLinkStatus.BlockIntervalDst)
			guessHeightOfDst := b.bmcLinkStatus.RxHeight + int64(math.Ceil(float64(bu.Height-b.bmcLinkStatus.RxHeightSrc)/scale)) - 1
			if guessHeightOfDst < rm.HeightOfDst {
				rm.HeightOfDst = guessHeightOfDst
			}
		}
		b.log.Debugf("addRelayMessage rms:%d bu:%d rps:%d HeightOfDst:%d", len(b.rms), bu.Height, len(rps), rm.HeightOfDst)
		rm = b.newRelayMessage()
	} else {
		if bu.Height <= b.bmcLinkStatus.Verifier.Height {
			return
		}
		rm.BlockUpdates = append(rm.BlockUpdates, bu)
		b.log.Debugf("addRelayMessage rms:%d bu:%d ~ %d", len(b.rms), rm.BlockUpdates[0].Height, bu.Height)
	}
}

// updateRelayMessage updates messages in the buffered rms
func (b *BTP) updateRelayMessages(verifierHeight int64, rxSeq int64) (err error) {
	b.rmsMutex.Lock()
	defer b.rmsMutex.Unlock()

	rrm := 0
	for i, rm := range b.rms {
		if len(rm.ReceiptProofs) > 0 {
			b.updateProofs(rm, rxSeq)
		}

		if rm.BlockProof != nil {
			if len(rm.ReceiptProofs) > 0 {
				if rm.BlockProof, err = b.newBlockProof(rm.BlockProof.BlockWitness.Height, rm.BlockProof.Header); err != nil {
					return
				}
			} else {
				rrm = i + 1
			}
		}

		if len(rm.BlockUpdates) > 0 {
			rbu := verifierHeight - rm.BlockUpdates[0].Height + 1
			if rbu < 1 {
				break
			}

			if rbu >= int64(len(rm.BlockUpdates)) {
				if len(rm.ReceiptProofs) > 0 {
					lbu := rm.BlockUpdates[len(rm.BlockUpdates)-1]
					if rm.BlockProof, err = b.newBlockProof(lbu.Height, lbu.Header); err != nil {
						return
					}
					rm.BlockUpdates = rm.BlockUpdates[:0]
				} else {
					rrm = i + 1
				}
			} else {
				b.log.Debugf("updateRelayMessage rm:%d removeBlockUpdates %d ~ %d",
					rm.Seq,
					rm.BlockUpdates[0].Height,
					rm.BlockUpdates[rbu-1].Height)
				rm.BlockUpdates = rm.BlockUpdates[rbu:]
			}
		}
	}

	if rrm > 0 {
		b.log.Debugf("updateRelayMessage rms:%d removeRelayMessage %d ~ %d",
			len(b.rms),
			b.rms[0].Seq,
			b.rms[rrm-1].Seq)

		b.rms = b.rms[rrm:]
		if len(b.rms) == 0 {
			b.newRelayMessage()
		}
	}
	return
}

// updateMTA updates Merkle Tree Accumulator
func (b *BTP) updateMTA(bu *chain.BlockUpdate) {
	next := b.store.Height() + 1
	if next < bu.Height {
		b.log.Fatalf("found missing block next:%d bu:%d", next, bu.Height)
		return
	}
	if next == bu.Height {
		b.store.AddHash(bu.BlockHash)
		if err := b.store.Flush(); err != nil {
			//TODO MTA Flush error handling
			b.log.Fatalf("fail to MTA Flush err:%+v", err)
		}
	}
}

// updateResult updates TransactionResult of the segment
func (b *BTP) updateResult(rm *chain.RelayMessage, segment *chain.Segment) (err error) {
	b.wp.Submit(func() {

		segment.TransactionResult, err = b.sender.GetResult(segment.GetResultParam)
		if err != nil {
			if ec, ok := errors.CoderOf(err); ok {
				b.log.Debugf("fail to GetResult GetResultParam:%v ErrorCoder:%+v", segment.GetResultParam, ec)
				switch ec.ErrorCode() {
				case chain.BMVRevertInvalidSequence, chain.BMVRevertInvalidBlockUpdateLower:
					for i := 0; i < len(rm.Segments); i++ {
						if rm.Segments[i] == segment {
							rm.Segments[i] = nil
							break
						}
					}
				case chain.BMVRevertInvalidBlockWitnessOld:
					rm.BlockProof, err = b.newBlockProof(rm.BlockProof.BlockWitness.Height, rm.BlockProof.Header)
					b.sender.UpdateSegment(rm.BlockProof, segment)
					segment.GetResultParam = nil
				case chain.BMVRevertInvalidSequenceHigher, chain.BMVRevertInvalidBlockUpdateHigher, chain.BMVRevertInvalidBlockProofHigher:
					segment.GetResultParam = nil
				case chain.BMCRevertUnauthorized:
					segment.GetResultParam = nil
				default:
					b.log.Panicf("fail to GetResult GetResultParam:%v ErrorCoder:%+v", segment.GetResultParam, ec)
				}
			} else {
				b.log.Panicf("fail to GetResult GetResultParam:%v err:%+v", segment.GetResultParam, err)
			}
		}
	})
	return nil
}

func (b *BTP) updateProofs(rm *chain.RelayMessage, rxSeq int64) {
	rrp := 0
	for j, rp := range rm.ReceiptProofs {
		// only update on have Events or EventProofs
		if len(rp.Events) == 0 || len(rp.EventProofs) == 0 {
			continue
		}
		revt := rxSeq - rp.Events[0].Sequence + 1
		if revt < 1 {
			break
		}

		if revt >= int64(len(rp.Events)) {
			rrp = j + 1
		} else {
			rp.Events = rp.Events[revt:]
			if len(rp.EventProofs) > 0 {
				rp.EventProofs = rp.EventProofs[revt:]
			}
		}
	}

	if rrp > 0 {
		rm.ReceiptProofs = rm.ReceiptProofs[rrp:]
	}
}
