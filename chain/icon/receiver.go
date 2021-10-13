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

package icon

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mpt"
)

const (
	EventSignature      = "Message(str,int,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type receiver struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	l   log.Logger
	opt struct {
	}

	evtLogRawFilter struct {
		addr      []byte
		signature []byte
		next      []byte
		seq       []byte
	}
	evtReq             *BlockRequest
	bh                 *BlockHeader
	isFoundOffsetBySeq bool
}

func (r *receiver) getBlockHeader(height HexInt) (*BlockHeader, error) {
	p := &BlockHeightParam{Height: height}
	b, err := r.c.GetBlockHeaderByHeight(p)
	if err != nil {
		return nil, mapError(err)
	}
	var bh BlockHeader
	_, err = codec.RLP.UnmarshalFromBytes(b, &bh)
	if err != nil {
		return nil, err
	}
	bh.serialized = b
	return &bh, nil
}

func (r *receiver) newBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	bh, err := r.getBlockHeader(v.Height)
	if err != nil {
		return nil, err
	}
	blkHash, _ := v.Hash.Value()
	if !bytes.Equal(blkHash, crypto.SHA3Sum256(bh.serialized)) {
		return nil, fmt.Errorf("mismatch block hash with BlockNotification")
	}

	var update BlockUpdate
	update.BlockHeader = bh.serialized
	vb, vbErr := r.c.GetVotesByHeight(&BlockHeightParam{Height: v.Height})
	if vbErr != nil {
		return nil, mapError(vbErr)
	}
	update.Votes = vb

	if r.bh == nil || !bytes.Equal(bh.NextValidatorsHash, r.bh.NextValidatorsHash) {
		dp := &DataHashParam{Hash: NewHexBytes(bh.NextValidatorsHash)}
		nvb, err := r.c.GetDataByHash(dp)
		if err != nil {
			return nil, mapError(err)
		}
		update.Validators = nvb
	}

	bu := &chain.BlockUpdate{
		BlockHash: blkHash,
		Height:    bh.Height,
		Header:    bh.serialized,
	}
	bu.Proof, err = codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}
	r.bh = bh
	return bu, nil
}

func (r *receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	nextEp := 0
	rps := make([]*chain.ReceiptProof, 0)
	if len(v.Indexes) > 0 {
		l := v.Indexes[0]
	RpLoop:
		for i, index := range l {
			p := &ProofEventsParam{BlockHash: v.Hash, Index: index, Events: v.Events[0][i]}
			proofs, err := r.c.GetProofForEvents(p)
			if err != nil {
				return nil, mapError(err)
			}
			if !r.isFoundOffsetBySeq {
			EpLoop:
				for j := 0; j < len(p.Events); j++ {
					if el, err := toEventLog(proofs[j+1]); err != nil {
						return nil, err
					} else if bytes.Equal(el.Addr, r.evtLogRawFilter.addr) &&
						bytes.Equal(el.Indexed[EventIndexSignature], r.evtLogRawFilter.signature) &&
						bytes.Equal(el.Indexed[EventIndexNext], r.evtLogRawFilter.next) &&
						bytes.Equal(el.Indexed[EventIndexSequence], r.evtLogRawFilter.seq) {
						r.isFoundOffsetBySeq = true
						r.l.Debugf("onCatchUp found offset sequence %d at %d event on block %s", r.evtLogRawFilter.seq, j, v.Height)
						if (j + 1) < len(p.Events) {
							nextEp = j + 1
							break EpLoop
						}
					}
				}
				if nextEp == 0 {
					continue RpLoop
				}
			}
			idx, _ := index.Value()
			h, _ := v.Height.Value()
			rp := &chain.ReceiptProof{
				Height:      h,
				Index:       int(idx),
				EventProofs: make([]*chain.EventProof, 0),
			}
			if rp.Proof, err = codec.RLP.MarshalToBytes(proofs[0]); err != nil {
				return nil, err
			}
			for k := nextEp; k < len(p.Events); k++ {
				eIdx, _ := p.Events[k].Value()
				ep := &chain.EventProof{
					Index: int(eIdx),
				}
				if ep.Proof, err = codec.RLP.MarshalToBytes(proofs[k+1]); err != nil {
					return nil, err
				}
				var evt *chain.Event
				if evt, err = r.toEvent(proofs[k+1]); err != nil {
					return nil, err
				}
				rp.Events = append(rp.Events, evt)
				rp.EventProofs = append(rp.EventProofs, ep)
			}
			rps = append(rps, rp)
			nextEp = 0
		}
	}
	return rps, nil
}

func (r *receiver) toEvent(proof [][]byte) (*chain.Event, error) {
	el, err := toEventLog(proof)
	if err != nil {
		return nil, err
	}
	if bytes.Equal(el.Addr, r.evtLogRawFilter.addr) &&
		bytes.Equal(el.Indexed[EventIndexSignature], r.evtLogRawFilter.signature) &&
		bytes.Equal(el.Indexed[EventIndexNext], r.evtLogRawFilter.next) {
		var i common.HexInt
		i.SetBytes(el.Indexed[EventIndexSequence])
		evt := &chain.Event{
			Next:     chain.BtpAddress(el.Indexed[EventIndexNext]),
			Sequence: i.Int64(),
			Message:  el.Data[0],
		}
		return evt, nil
	}
	return nil, fmt.Errorf("invalid event")
}

func toEventLog(proof [][]byte) (*EventLog, error) {
	mp, err := mpt.NewMptProof(proof)
	if err != nil {
		return nil, err
	}
	el := &EventLog{}
	if _, err := codec.RLP.UnmarshalFromBytes(mp.Leaf().Data, el); err != nil {
		return nil, fmt.Errorf("fail to parse EventLog on leaf err:%+v", err)
	}
	return el, nil
}

func (r *receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	s := r.dst.String()
	ef := &EventFilter{
		Addr:      Address(r.src.ContractAddress()),
		Signature: EventSignature,
		Indexed:   []*string{&s},
	}
	r.evtReq = &BlockRequest{
		Height:       NewHexInt(height),
		EventFilters: []*EventFilter{ef},
	}

	if height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}
	var err error
	if r.bh, err = r.getBlockHeader(NewHexInt(height - 1)); err != nil {
		return err
	}
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}
	if r.evtLogRawFilter.addr, err = ef.Addr.Value(); err != nil {
		r.l.Panicf("ef.Addr.Value() err:%+v", err)
	}
	r.evtLogRawFilter.signature = []byte(EventSignature)
	r.evtLogRawFilter.next = []byte(s)
	r.evtLogRawFilter.seq = common.NewHexInt(seq).Bytes()
	return r.c.MonitorBlock(r.evtReq,
		func(conn *jsonrpc.RecConn, v *BlockNotification) error {
			var err error
			var bu *chain.BlockUpdate
			var rps []*chain.ReceiptProof
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
		func(conn *jsonrpc.RecConn) {
			r.l.Debugf("ReceiveLoop connected %s", conn.Id)
			if scb != nil {
				scb()
			}
		},
		func(conn *jsonrpc.RecConn, err error) {
			r.l.Debugf("onError %s err:%+v", conn.Id, err)
			conn.CloseAndReconnect()
		})
}

func (r *receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) chain.Receiver {
	r := &receiver{
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
	r.c = NewClient(endpoint, l)
	return r
}
