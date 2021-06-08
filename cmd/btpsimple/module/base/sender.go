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

package base

import (
	"encoding/json"
	"fmt"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"time"
	"github.com/reactivex/rxgo/v2"
)

const (
	DefaultGetRelayResultInterval = time.Second
	DefaultRelayReSendInterval    = time.Second
)

type sender struct {
	client      Client
	source      BtpAddress
	destination BtpAddress
	wallet      Wallet
	logger      log.Logger
	options     struct {
		StepLimit int64
	}
}

func (s *sender) MonitorLoop(height int64, cb MonitorCallback, scb func()) error {
	br := s.client.GetBlockRequest(height)

	return s.client.MonitorSenderBlock(br,
		func(observable rxgo.Observable) error {
			result := observable.Observe()
			for item := range result {
				bn := item.V.(*BlockNotification)

				if h, err := s.client.GetBlockNotificationHeight(bn); err != nil {
					return err
				} else {
					return cb(h)
				}
			}
			return nil
		},
		func() {
			scb()
		})
}

func (s *sender) FinalizeLatency() int {
	return 1
}

func (s *sender) GetResult(p GetResultParam) (TransactionResult, error) {
	for {
		txr, err := s.client.GetTransactionResult(&p)
		if err != nil {
			switch err {
			case ErrGetResultFailByPending:
				<-time.After(DefaultGetRelayResultInterval)
				continue
			}
		}
		return txr, err
	}
}

func (s *sender) GetStatus() (*BMCLinkStatus, error) {
	return s.client.GetBMCLinkStatus(s.wallet, s.destination, s.source)
}

func (s *sender) UpdateSegment(bp *BlockProof, segment *Segment) error {
	p, err := s.client.GetTransactionParams(segment)
	if err != nil {
		return err
	}
	msg, prev, err := s.client.GetRelayMethodParams(&p)
	if err != nil {
		return err
	}
	rmsg := &RelayMessageClient{}
	if err := s.client.UnmarshalFromSegment(msg, rmsg); err != nil {
		return err
	}
	if rmsg.BlockProof, err = codec.RLP.MarshalToBytes(bp); err != nil {
		return err
	}
	segment.TransactionParam, err = s.client.BMCRelayMethodTransactionParam(s.wallet, s.destination, s.source, prev, rmsg, s.options.StepLimit)
	return err
}

func (s *sender) Relay(segment *Segment) (GetResultParam, error) {
	p, err := s.client.GetTransactionParams(segment)
	if err != nil {
		return nil, err
	}
	thp := new(TransactionHashParam)
SignLoop:
	for {
		if err := s.client.SignTransaction(s.wallet, &p); err != nil {
			return nil, err
		}
	SendLoop:
		for {
			txh, err := s.client.SendTransaction(&p)
			if txh != nil {
				if err := s.client.AttachHash(thp, txh); err != nil {
					return nil, err
				}
			}
			if err != nil {
				switch err {
				case ErrSendFailByOverflow:
					<-time.After(DefaultRelayReSendInterval)
					continue SendLoop
				case ErrSendDuplicateTransaction:
					s.logger.Debugf("DuplicateTransactionError txh:%v", txh)
					return thp, nil
				case ErrSendFailByExpired:
					continue SignLoop
				}
				return nil, err
			}
			return thp, nil
		}
	}
}

func (s *sender) Segment(rm *RelayMessage, height int64) ([]*Segment, error) {
	segments := make([]*Segment, 0)
	var err error
	rmsg := &RelayMessageClient{
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
		if s.client.IsTransactionOverLimit(buSize) {
			return nil, fmt.Errorf("invalid BlockUpdate.Proof size")
		}
		size += buSize
		if s.client.IsTransactionOverLimit(size) {
			segment := &Segment{
				Height:              rmsg.height,
				NumberOfBlockUpdate: rmsg.numberOfBlockUpdate,
			}
			if segment.TransactionParam, err = s.client.BMCRelayMethodTransactionParam(s.wallet, s.destination, s.source, rm.From.String(), rmsg, s.options.StepLimit); err != nil {
				return nil, err
			}
			segments = append(segments, segment)
			rmsg = &RelayMessageClient{
				BlockUpdates:  make([][]byte, 0),
				ReceiptProofs: make([][]byte, 0),
			}
			size = buSize
		}
		rmsg.BlockUpdates = append(rmsg.BlockUpdates, bu.Proof)
		rmsg.height = bu.Height
		rmsg.numberOfBlockUpdate += 1
	}

	var bp []byte
	if bp, err = codec.RLP.MarshalToBytes(rm.BlockProof); err != nil {
		return nil, err
	}
	if s.client.IsTransactionOverLimit(len(bp)) {
		return nil, fmt.Errorf("invalid BlockProof size")
	}

	var b []byte
	for _, rp := range rm.ReceiptProofs {
		if s.client.IsTransactionOverLimit(len(rp.Proof)) {
			return nil, fmt.Errorf("invalid ReceiptProof.Proof size")
		}
		if len(rmsg.BlockUpdates) == 0 {
			size += len(bp)
			rmsg.BlockProof = bp
			rmsg.height = rm.BlockProof.BlockWitness.Height
		}
		size += len(rp.Proof)
		trp := &ReceiptProof{
			Index:       rp.Index,
			Proof:       rp.Proof,
			EventProofs: make([]*EventProof, 0),
		}
		for j, ep := range rp.EventProofs {
			if s.client.IsTransactionOverLimit(len(ep.Proof)) {
				return nil, fmt.Errorf("invalid EventProof.Proof size")
			}
			size += len(ep.Proof)
			if s.client.IsTransactionOverLimit(size) {
				if j == 0 && len(rmsg.BlockUpdates) == 0 {
					return nil, fmt.Errorf("BlockProof + ReceiptProof + EventProof > limit")
				}
				//
				segment := &Segment{
					Height:              rmsg.height,
					NumberOfBlockUpdate: rmsg.numberOfBlockUpdate,
					EventSequence:       rmsg.eventSequence,
					NumberOfEvent:       rmsg.numberOfEvent,
				}
				if segment.TransactionParam, err = s.client.BMCRelayMethodTransactionParam(s.wallet, s.destination, s.source, rm.From.String(), rmsg, s.options.StepLimit); err != nil {
					return nil, err
				}
				segments = append(segments, segment)

				rmsg = &RelayMessageClient{
					BlockUpdates:  make([][]byte, 0),
					ReceiptProofs: make([][]byte, 0),
					BlockProof:    bp,
				}
				size = len(ep.Proof)
				size += len(rp.Proof)
				size += len(bp)

				trp = &ReceiptProof{
					Index:       rp.Index,
					Proof:       rp.Proof,
					EventProofs: make([]*EventProof, 0),
				}
			}
			trp.EventProofs = append(trp.EventProofs, ep)
			rmsg.eventSequence = rp.Events[j].Sequence
			rmsg.numberOfEvent += 1
		}

		if b, err = codec.RLP.MarshalToBytes(trp); err != nil {
			return nil, err
		}
		rmsg.ReceiptProofs = append(rmsg.ReceiptProofs, b)
	}
	//
	segment := &Segment{
		Height:              rmsg.height,
		NumberOfBlockUpdate: rmsg.numberOfBlockUpdate,
		EventSequence:       rmsg.eventSequence,
		NumberOfEvent:       rmsg.numberOfEvent,
	}
	if segment.TransactionParam, err = s.client.BMCRelayMethodTransactionParam(s.wallet, s.destination, s.source, rm.From.String(), rmsg, s.options.StepLimit); err != nil {
		return nil, err
	}
	segments = append(segments, segment)
	return segments, nil
}

func (s *sender) StopMonitorLoop() {
	s.client.CloseAllMonitor()
}

func NewSender(source, destination BtpAddress, wallet Wallet, endpoint string, options map[string]interface{}, logger log.Logger, client Client) Sender {
	sender := &sender{
		client:      client,
		source:      source,
		destination: destination,
		wallet:      wallet,
		logger:      logger,
	}
	b, err := json.Marshal(options)
	if err != nil {
		logger.Panicf("fail to marshal opt:%#v err:%+v", options, err)
	}
	if err = json.Unmarshal(b, &sender.options); err != nil {
		logger.Panicf("fail to unmarshal opt:%#v err:%+v", options, err)
	}
	return sender
}
