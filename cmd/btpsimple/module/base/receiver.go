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
	"math/big"
	"github.com/reactivex/rxgo/v2"

	"github.com/icon-project/btp/common/crypto"
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

func (r *receiver) getBlockHeader(height int64) (*BlockHeader, error) {
	var blockHeader BlockHeader
	serializedHeader, err := r.client.GetBlockHeaderByHeight(height, &blockHeader)
	if err != nil {
		return nil, err
	}

	blockHeader.Serialized = serializedHeader
	return &blockHeader, nil
}

func (r *receiver) newBlockUpdate(blockNotification *BlockNotification) (*BlockUpdate, error) {
	height, err := r.client.GetBlockNotificationHeight(blockNotification)
	if err != nil {
		return nil, err
	}

	blockHeader, err := r.getBlockHeader(height)
	if err != nil {
		return nil, err
	}

	blockHash, _ := r.client.GetBlockNotificationHash(blockNotification)
	if !bytes.Equal(blockHash, crypto.SHA3Sum256(blockHeader.Serialized)) {
		return nil, fmt.Errorf("mismatch block hash with BlockNotification")
	}

	blockUpdate := &BlockUpdate{
		BlockHash: blockHash,
		Height:    blockHeader.Height,
		Header:    blockHeader.Serialized,
	}

	blockUpdate.Proof, err = r.client.GetBlockProof(blockHeader)
	if err != nil {
		return nil, err
	}

	r.blockHeader = blockHeader
	return blockUpdate, nil
}

func (r *receiver) newReceiptProofs(blockNotification *BlockNotification) ([]*ReceiptProof, error) {
	receiptProofs, isFound, err := r.client.GetReceiptProofs(blockNotification, r.isFoundOffsetBySequence, r.eventLogFilter)
	r.isFoundOffsetBySequence = isFound

	if err != nil {
		return nil, err
	}

	return receiptProofs, nil
}


func (r *receiver) ReceiveLoop(height int64, sequence *big.Int, receiveCallback ReceiveCallback, scb func()) error {
	destination := r.destination.String()
	r.eventRequest = r.client.GetEventRequest(r.source, destination, height)

	if height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	var err error
	if r.blockHeader, err = r.getBlockHeader(height - 1); err != nil {
		return err
	}

	if sequence.Cmp(big.NewInt(1)) < 0 {
		r.isFoundOffsetBySequence = true
	}

	return r.client.MonitorReceiverBlock(r.eventRequest,
		func(observable rxgo.Observable) error {
			result := observable.Observe()
			var err error
			var bu *BlockUpdate
			var rps []*ReceiptProof

			for item := range result {
				bn := item.V.(*BlockNotification)

				if bu, err = r.newBlockUpdate(bn); err != nil {
					return err
				}

				if rps, err = r.newReceiptProofs(bn); err != nil {
					return err
				} else if r.isFoundOffsetBySequence {
					receiveCallback(bu, rps)
				} else {
					receiveCallback(bu, nil)
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
		logger.Panicf("fail to marshal opt:%#v err:%+v", options, err)
	}

	if err = json.Unmarshal(byteData, &receiver.options); err != nil {
		logger.Panicf("fail to unmarshal opt:%#v err:%+v", options, err)
	}

	return receiver
}

func (r *receiver) StopReceiveLoop() {
	r.client.CloseAllMonitor()
}
