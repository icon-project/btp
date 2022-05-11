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
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/reactivex/rxgo/v2"

	"github.com/icon-project/btp/common/log"
)

type receiver struct {
	client      Client
	source      BtpAddress
	destination BtpAddress
	logger      log.Logger
	options     struct {
	}

	eventLogFilter          EventLogFilter
	eventRequest            *BlockRequest
	blockHeader             *BlockHeader
	isFoundOffsetBySequence bool
}

func (r *receiver) newBlockUpdate(blockNotification BlockNotification) (*BlockUpdate, error) {
	height := blockNotification.Height()

	blockHeader, err := r.client.GetBlockHeader(height)
	if err != nil {
		return nil, err
	}

	blockHash, err := r.client.ComputeBlockHash(blockHeader.Serilaized)
	if err != nil {
		return nil, err
	}

	if !bytes.Equal(blockNotification.Hash(), blockHash) {
		return nil, fmt.Errorf("mismatch block hash with BlockNotification")
	}

	blockUpdate := &BlockUpdate{
		BlockHash: blockHash,
		Height:    blockHeader.Height,
		Header:    blockHeader.Serilaized,
	}

	blockUpdate.Proof, err = r.client.GetBlockProof(&blockNotification)
	if err != nil {
		return nil, err
	}

	r.blockHeader = blockHeader
	return blockUpdate, nil
}

func (r *receiver) ReceiveLoop(height int64, sequence int64, receiveCallback ReceiveCallback, scb func()) error {

	r.eventRequest = r.client.GetEventRequest(r.source, r.destination, height)

	if height < 1 {
		return fmt.Errorf("cannot catch up from zero height") //TODO : Refactor errors to error code
	}

	var err error
	if r.blockHeader, err = r.client.GetBlockHeader(height - 1); err != nil {
		return err
	}

	if sequence < 1 {
		r.isFoundOffsetBySequence = true
	}

	return r.client.MonitorReceiverBlock(r.eventRequest,
		func(observable rxgo.Observable) error {
			result := observable.Observe()
			var err error
			var blockUpdate *BlockUpdate
			var receiptProofs []*ReceiptProof

			for item := range result {
				if err := item.E; err != nil {
					return err
				}
				blockNotification, _ := item.V.(BlockNotification)

				if blockUpdate, err = r.newBlockUpdate(blockNotification); err != nil {
					return err
				}
				
				if receiptProofs, err = r.client.GetReceiptProofs(r.eventRequest); err != nil {
					return err
				} else if r.isFoundOffsetBySequence {
					receiveCallback(blockUpdate, receiptProofs)
				} else {
					receiveCallback(blockUpdate, nil)
				}
			}

			return nil
		},
		func() {
			scb()
		})
}

func NewReceiver(source, destination BtpAddress, endpoint string, options map[string]interface{}, logger log.Logger, client Client) Receiver {
	receiver := &receiver{
		client:      client,
		source:      source,
		destination: destination,
		logger:      logger,
	}

	byteData, err := json.Marshal(options)

	if err != nil {
		logger.Panicf("fail to marshal options :%#v err:%+v", options, err)
	}

	if err = json.Unmarshal(byteData, &receiver.options); err != nil {
		logger.Panicf("fail to unmarshal options :%#v err:%+v", options, err)
	}

	return receiver
}

func (r *receiver) StopReceiveLoop() {
	r.client.CloseAllMonitor()
}

func (r *receiver) GetBlockUpdate(height int64) (*BlockUpdate, error) {
	// var bu *BlockUpdate
	// v := &BlockNotification{Height: big.NewInt(height)}
	// bu, err := r.newBlockUpdate(v)
	return nil, nil
}

