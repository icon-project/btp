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

package iconbridge

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math/big"

	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/bridge/module"
	"github.com/icon-project/btp/cmd/bridge/module/iconbridge/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/intconv"
	"github.com/icon-project/btp/common/mpt"

	"github.com/icon-project/btp/common/log"
)

type Receiver struct {
	c   *client.Client
	src module.BtpAddress
	dst module.BtpAddress
	l   log.Logger
	opt struct {
	}
	req *client.BTPRequest
	bh  *client.BTPBlockHeader
}

func (r *Receiver) getBTPLinkNetworkId() (networkId int64, err error) {
	p := &client.CallParam{
		ToAddress: client.Address(r.src.Account()),
		DataType:  "call",
		Data: client.CallData{
			Method: "getBTPLinkNetworkId",
			Params: client.BMCStatusParams{
				Target: r.dst.String(),
			},
		},
	}
	var ret client.HexInt
	if err = r.c.Call(p, &ret); err != nil {
		return
	}
	return ret.Value()
}

func (r *Receiver) getBTPLinkOffset() (offset int64, err error) {
	p := &client.CallParam{
		ToAddress: client.Address(r.src.Account()),
		DataType:  "call",
		Data: client.CallData{
			Method: "getBTPLinkOffset",
			Params: client.BMCStatusParams{
				Target: r.dst.String(),
			},
		},
	}
	var ret client.HexInt
	if err = r.c.Call(p, &ret); err != nil {
		return
	}
	return ret.Value()
}

func (r *Receiver) monitorBTPBlock(req *client.BTPRequest, seq int64,
	cb module.ReceiveCallback,
	scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	offset, err := r.getBTPLinkOffset()
	if err != nil {
		return err
	}
	//BMC.seq starts with 1 and BTPBlock.FirstMessageSN starts with 0
	offset += 1
	return r.c.MonitorBTP(req, func(conn *websocket.Conn, v *client.BTPNotification) error {
		b, err := v.Header.Value()
		if err != nil {
			return err
		}
		bh := &client.BTPBlockHeader{}
		if _, err = codec.RLP.UnmarshalFromBytes(b, bh); err != nil {
			return err
		}
		if bh.MessageCount == 0 {
			return nil
		}
		sn := offset + bh.UpdateNumber>>1
		rps := make([]*module.ReceiptProof, 0)
		evts := make([]*module.Event, 0)
		p := &client.BTPBlockParam{
			Height:    client.NewHexInt(bh.MainHeight),
			NetworkId: req.NetworkID,
		}
		msgs, err := r.c.GetBTPMessage(p)
		if err != nil {
			return err
		}
		for _, msg := range msgs {
			if sn > seq {
				evt, err := messageToEvent(r.dst.String(), msg, sn)
				if err != nil {
					return err
				}
				evts = append(evts, evt)
			}
			sn++
		}
		if len(evts) == 0 {
			return nil
		}
		rp := &module.ReceiptProof{
			Index:  0,
			Events: evts,
			Height: bh.MainHeight,
		}
		rps = append(rps, rp)

		return cb(rps)
	}, scb, errCb)
}

func messageToEvent(next, msg string, seq int64) (*module.Event, error) {
	b, err := base64.StdEncoding.DecodeString(msg)
	if err != nil {
		return nil, err
	}
	evt := &module.Event{
		Next:     next,
		Sequence: seq,
		Message:  b,
	}
	return evt, nil
}

const (
	EventSignature      = "Message(str,int,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

func (r *Receiver) monitorEvent(req *client.BlockRequest, seq int64,
	cb module.ReceiveCallback,
	scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	return r.c.MonitorBlock(req,
		func(conn *websocket.Conn, v *client.BlockNotification) error {
			if len(v.Indexes) > 0 {
				rps := make([]*module.ReceiptProof, 0)
				h, _ := v.Height.Value()
				l := v.Indexes[0]
			RpLoop:
				for i, index := range l {
					p := &client.ProofEventsParam{BlockHash: v.Hash, Index: index, Events: v.Events[0][i]}
					proofs, err := r.c.GetProofForEvents(p)
					if err != nil {
						return mapError(err)
					}
					evts := make([]*module.Event, 0)
				EpLoop:
					for j := 0; j < len(p.Events); j++ {
						var evt *module.Event
						if evt, err = proofToEvent(proofs[j+1]); err != nil {
							return err
						}
						if evt.Sequence < seq {
							continue EpLoop
						}
						evts = append(evts, evt)
					}
					if len(evts) == 0 {
						continue RpLoop
					}
					idx, _ := index.Value()
					rp := &module.ReceiptProof{
						Index:  idx,
						Events: evts,
						Height: h,
					}
					rps = append(rps, rp)
				}
				return cb(rps)
			}
			return nil
		}, scb, errCb)
}

func proofToEvent(proof [][]byte) (*module.Event, error) {
	el, err := proofToEventLog(proof)
	if err != nil {
		return nil, err
	}
	var i big.Int
	intconv.BigIntSetBytes(&i, el.Indexed[EventIndexSequence])
	evt := &module.Event{
		Next:     string(el.Indexed[EventIndexNext]),
		Sequence: i.Int64(),
		Message:  el.Data[0],
	}
	return evt, nil
}

func proofToEventLog(proof [][]byte) (*client.EventLog, error) {
	mp, err := mpt.NewMptProof(proof)
	if err != nil {
		return nil, err
	}
	el := &client.EventLog{}
	if _, err := codec.RLP.UnmarshalFromBytes(mp.Leaf().Data, el); err != nil {
		return nil, fmt.Errorf("fail to parse EventLog on leaf err:%+v", err)
	}
	return el, nil
}

func (r *Receiver) ReceiveLoop(height, seq int64,
	cb module.ReceiveCallback, scb func()) error {
	if height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	onErr := func(conn *websocket.Conn, err error) {
		r.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
		_ = conn.Close()
	}
	if networkId, err := r.getBTPLinkNetworkId(); err == nil && networkId > 0 {
		req := &client.BTPRequest{
			Height:    client.HexInt(intconv.FormatInt(height)),
			NetworkID: client.HexInt(intconv.FormatInt(networkId)),
		}
		onConn := func(conn *websocket.Conn) {
			r.l.Debugf("ReceiveLoop monitorBTPBlock height:%d seq:%d networkId:%d connected %s",
				height, seq, networkId, conn.LocalAddr().String())
			if scb != nil {
				scb()
			}
		}
		return r.monitorBTPBlock(req, seq, cb, onConn, onErr)
	} else {
		s := r.dst.String()
		ef := &client.EventFilter{
			Addr:      client.Address(r.src.Account()),
			Signature: EventSignature,
			Indexed:   []*string{&s},
		}
		req := &client.BlockRequest{
			Height:       client.NewHexInt(height),
			EventFilters: []*client.EventFilter{ef},
		}
		onConn := func(conn *websocket.Conn) {
			r.l.Debugf("ReceiveLoop monitorEvent height:%d seq:%d connected %s",
				height, seq, conn.LocalAddr().String())
			if scb != nil {
				scb()
			}
		}
		return r.monitorEvent(req, seq, cb, onConn, onErr)
	}
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
