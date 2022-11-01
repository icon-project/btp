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

package evmbridge

import (
	"encoding/json"
	"math/big"
	"sort"

	"github.com/ethereum/go-ethereum"
	"github.com/icon-project/btp/cmd/bridge/module/evmbridge/client"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/icon-project/btp/cmd/bridge/module"
	"github.com/icon-project/btp/common/log"
)

const (
	EventSignature      = "Message(string,uint256,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type Receiver struct {
	c   *client.Client
	src module.BtpAddress
	dst module.BtpAddress
	l   log.Logger
	opt struct {
	}
}

func logToEvent(el *types.Log) (*module.Event, error) {
	bm, err := client.UnpackEventLog(el.Data)
	if err != nil {
		return nil, err
	}
	return &module.Event{
		Next:     bm.Next,
		Sequence: bm.Seq.Int64(),
		Message:  bm.Msg,
	}, nil
}

func (r *Receiver) ReceiveLoop(height, seq int64,
	cb module.ReceiveCallback, scb func()) error {
	fq := &ethereum.FilterQuery{
		Addresses: []common.Address{common.HexToAddress(r.src.ContractAddress())},
		Topics: [][]common.Hash{
			{crypto.Keccak256Hash([]byte(EventSignature))},
			//{crypto.Keccak256Hash([]byte(r.dst.String()))}, //if 'next' is indexed
		},
	}
	r.l.Debugf("ReceiveLoop height:%d seq:%d filterQuery[Address:%s,Topic:%s]",
		height, seq, fq.Addresses[0].String(), fq.Topics[0][0].Hex())
	br := &client.BlockRequest{
		Height:      big.NewInt(height),
		FilterQuery: fq,
	}
	started := false
	return r.c.MonitorBlock(br,
		func(v *client.BlockNotification) error {
			if !started {
				started = true
				scb()
			}
			if len(v.Logs) > 0 {
				rpsMap := make(map[uint]*module.ReceiptProof)
			EpLoop:
				for _, el := range v.Logs {
					evt, err := logToEvent(&el)
					r.l.Debugf("event[seq:%d next:%s] seq:%d dst:%s",
						evt.Sequence, evt.Next, seq, r.dst.String())
					if err != nil {
						return err
					}
					if evt.Sequence <= seq {
						continue EpLoop
					}
					//below statement is unnecessary if 'next' is indexed
					if evt.Next != r.dst.String() {
						continue EpLoop
					}
					rp, ok := rpsMap[el.TxIndex]
					if !ok {
						rp = &module.ReceiptProof{
							Index:  int64(el.TxIndex),
							Events: make([]*module.Event, 0),
							Height: int64(el.BlockNumber),
						}
						rpsMap[el.TxIndex] = rp
					}
					rp.Events = append(rp.Events, evt)
				}
				if len(rpsMap) > 0 {
					rps := make([]*module.ReceiptProof, 0)
					for _, rp := range rpsMap {
						rps = append(rps, rp)
					}
					sort.Slice(rps, func(i int, j int) bool {
						return rps[i].Index < rps[j].Index
					})
					return cb(rps)
				}
			}
			return nil
		},
	)
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}

func NewReceiver(src, dst module.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) module.Receiver {
	r := &Receiver{
		src: src,
		dst: dst,
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &r.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	r.c = client.NewClient(endpoint, l)
	return r
}
