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

package bsc

import (
	"encoding/json"
	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"math/big"
)

type receiver struct {
	c   *Client
	src module.BtpAddress
	dst module.BtpAddress
	log log.Logger
	opt struct {
	}
	consensusStates    ConsensusStates
	evtReq             *BlockRequest
	isFoundOffsetBySeq bool
}

func (r *receiver) newBlockUpdate(v *BlockNotification) (*module.BlockUpdate, error) {
	var err error

	bu := &module.BlockUpdate{
		BlockHash: v.Hash.Bytes(),
		Height:    v.Height.Int64(),
	}

	bu.Header, err = codec.RLP.MarshalToBytes(*v.Header)
	if err != nil {
		return nil, err
	}

	/*proof, err := r.c.GetProof(v.Height, HexToAddress(r.src.ContractAddress()))
	if err != nil {
		return nil, err
	}*/

	/*if (v.Height % epoch) == 0 {
	   If (height % epoch)==0, should fetch ValidatorSet from BSCValidatorSet contract.
		r.consensusStates , err = r.c.GetLatestConsensusState()
	}*/

	update := &BlockUpdate{}
	update.BlockHeader, _ = codec.RLP.MarshalToBytes(*v.Header)
	update.Validators = r.consensusStates.NextValidatorSet

	bu.Proof, err = codec.RLP.MarshalToBytes(update)
	if err != nil {
		return nil, err
	}

	return bu, nil
}

func (r *receiver) newReceiptProofs(v *BlockNotification) ([]*module.ReceiptProof, error) {
	rps := make([]*module.ReceiptProof, 0)

	block, err := r.c.GetBlockByHeight(v.Height)
	if err != nil {
		return nil, err
	}

	receipts, err := r.c.GetBlockReceipts(block)
	if err != nil {
		return nil, err
	}

	for _, receipt := range receipts {
		rp := &module.ReceiptProof{}
		rp.Index = int(receipt.TransactionIndex)
		rp.Proof, err = codec.RLP.MarshalToBytes(receipt)
		if err != nil {
			return nil, err
		}
		eventProofs := make([]*module.EventProof, 0)
		for _, eventLog := range receipt.Logs {
			ep := &module.EventProof{}
			ep.Proof, err = codec.RLP.MarshalToBytes(eventLog)
			ep.Index = int(eventLog.Index)
			eventProofs = append(eventProofs, ep)
		}
		rps = append(rps, rp)
	}

	return rps, nil
}

func (r *receiver) ReceiveLoop(height int64, seq int64, cb module.ReceiveCallback, scb func()) error {
	r.log.Debugf("ReceiveLoop connected")
	br := &BlockRequest{
		Height: big.NewInt(height),
	}
	var err error
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}
	r.consensusStates, err = r.c.GetLatestConsensusState()
	if err != nil {
		r.log.Fatalf(err.Error())
	}
	return r.c.MonitorBlock(br,
		func(v *BlockNotification) error {
			var bu *module.BlockUpdate
			var rps []*module.ReceiptProof
			if bu, err = r.newBlockUpdate(v); err != nil {
				return err
			}
			if rps, err = r.newReceiptProofs(v); err != nil {
				return err
			} else if r.isFoundOffsetBySeq {
				cb(bu, rps)
			} else {
				cb(bu, nil)
			}
			return nil
		},
	)
}

func (r *receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}

func NewReceiver(src, dst module.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) module.Receiver {
	r := &receiver{
		src: src,
		dst: dst,
		log: l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &r.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	r.c = NewClient(endpoint, l)
	return r
}

func (r *receiver) GetBlockUpdate(height int64) (*module.BlockUpdate, error) {
	var bu *module.BlockUpdate
	v := &BlockNotification{Height: big.NewInt(height)}
	bu, err := r.newBlockUpdate(v)
	return bu, err
}
