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
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"math/big"
	"sort"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common/hexutil"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethdb/memorydb"
	"github.com/ethereum/go-ethereum/light"
	"github.com/ethereum/go-ethereum/rlp"
	"github.com/ethereum/go-ethereum/trie"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/cmd/bridge/module/evmbridge/client"

	//"github.com/icon-project/btp/cmd/bridge/module"
	//"github.com/icon-project/btp/cmd/bridge/module/evmbridge/client"
	"github.com/icon-project/btp/common/codec"

	"github.com/icon-project/btp/common/log"
)

const (
	EPOCH               = 200
	EventSignature      = "Message(string,uint256,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type receiver struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	log log.Logger
	opt struct {
	}

	evtReq             *BlockRequest
	isFoundOffsetBySeq bool
}

func logToEvent(el *types.Log) (*chain.Event, error) {
	bm, err := client.UnpackEventLog(el.Data)
	if err != nil {
		return nil, err
	}
	return &chain.Event{
		Next:     chain.BtpAddress(bm.Next),
		Sequence: bm.Seq,
		Message:  bm.Msg,
	}, nil
}

func (r *receiver) newBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	var err error

	bu := &chain.BlockUpdate{
		BlockHash: v.Hash.Bytes(),
		Height:    v.Height.Int64(),
	}

	header := MakeHeader(v.Header)
	bu.Header, err = codec.RLP.MarshalToBytes(*header)
	if err != nil {
		return nil, err
	}

	encodedHeader, _ := rlp.EncodeToBytes(v.Header)
	if !bytes.Equal(v.Header.Hash().Bytes(), crypto.Keccak256(encodedHeader)) {
		return nil, fmt.Errorf("mismatch block hash with BlockNotification")
	}

	update := &BlockUpdate{}
	update.BlockHeader, _ = codec.RLP.MarshalToBytes(*header)
	//TODO get validators
	//update.Validators = header.Extra format marshal
	buf := new(bytes.Buffer)
	encodeSigHeader(buf, v.Header)
	update.EvmHeader = buf.Bytes()

	bu.Proof, err = codec.RLP.MarshalToBytes(update)
	if err != nil {
		return nil, err
	}

	return bu, nil
}

func encodeSigHeader(w io.Writer, header *types.Header) {
	err := rlp.Encode(w, []interface{}{
		big.NewInt(97),
		header.ParentHash,
		header.UncleHash,
		header.Coinbase,
		header.Root,
		header.TxHash,
		header.ReceiptHash,
		header.Bloom,
		header.Difficulty,
		header.Number,
		header.GasLimit,
		header.GasUsed,
		header.Time,
		header.Extra[:len(header.Extra)-65], // Yes, this will panic if extra is too short
		header.MixDigest,
		header.Nonce,
	})

	if err != nil {
		//panic("can't encode: " + err.Error())
	}
}

func (r *receiver) newReceiptProofs(v *BlockNotification, seq *big.Int) ([]*chain.ReceiptProof, error) {
	rpsMap := make(map[uint]*chain.ReceiptProof)
	rps := make([]*chain.ReceiptProof, 0)
EpLoop:
	for _, el := range v.Logs {
		evt, err := logToEvent(&el)
		r.log.Debugf("event[seq:%d next:%s] seq:%d dst:%s",
			evt.Sequence, evt.Next, seq, r.dst.String())
		if err != nil {
			return nil, err
		}
		if evt.Sequence.Int64() <= seq.Int64() {
			continue EpLoop
		}
		//below statement is unnecessary if 'next' is indexed
		if evt.Next.String() != r.dst.String() {
			continue EpLoop
		}
		rp, ok := rpsMap[el.TxIndex]
		if !ok {
			rp = &chain.ReceiptProof{
				Index:  int(el.TxIndex),
				Events: make([]*chain.Event, 0),
				Height: int64(el.BlockNumber),
			}
			rpsMap[el.TxIndex] = rp
		}
		rp.Events = append(rp.Events, evt)
	}
	if len(rpsMap) > 0 {
		for _, rp := range rpsMap {
			rps = append(rps, rp)
		}
		sort.Slice(rps, func(i int, j int) bool {
			return rps[i].Index < rps[j].Index
		})
	}
	return rps, nil
}

func trieFromReceipts(receipts []*types.Receipt) (*trie.Trie, error) {
	tr, _ := trie.New(common.Hash{}, trie.NewDatabase(memorydb.New()))

	for i, r := range receipts {
		path, err := rlp.EncodeToBytes(uint(i))

		if err != nil {
			return nil, err
		}

		rawReceipt, err := rlp.EncodeToBytes(r)
		if err != nil {
			return nil, err
		}

		tr.Update(path, rawReceipt)
	}

	_, err := tr.Commit(nil)
	if err != nil {
		return nil, err
	}

	return tr, nil
}

func receiptProof(receiptTrie *trie.Trie, key []byte) ([][]byte, error) {
	proofSet := light.NewNodeSet()
	err := receiptTrie.Prove(key, 0, proofSet)
	if err != nil {
		return nil, err
	}
	proofs := make([][]byte, 0)
	for _, node := range proofSet.NodeList() {
		fmt.Println(hexutil.Encode(node))
		proofs = append(proofs, node)
	}
	return proofs, nil
}

func (r *receiver) ReceiveLoop(height int64, seq *big.Int, cb chain.ReceiveCallback, scb func()) error {
	var err error
	fq := &ethereum.FilterQuery{
		Addresses: []common.Address{common.HexToAddress(r.src.ContractAddress())},
		Topics: [][]common.Hash{
			{crypto.Keccak256Hash([]byte(EventSignature))},
			//{crypto.Keccak256Hash([]byte(r.dst.String()))}, //if 'next' is indexed
		},
	}
	r.log.Debugf("ReceiveLoop height:%d seq:%d filterQuery[Address:%s,Topic:%s]",
		height, seq, fq.Addresses[0].String(), fq.Topics[0][0].Hex())
	br := &BlockRequest{
		Height:      big.NewInt(height),
		FilterQuery: fq,
	}
	started := false
	return r.c.MonitorBlock(br,
		func(v *BlockNotification) error {
			if !started {
				started = true
				scb()
			}

			var bu *chain.BlockUpdate
			var rps []*chain.ReceiptProof

			if bu, err = r.newBlockUpdate(v); err != nil {
				return err
			}
			if rps, err = r.newReceiptProofs(v, seq); err != nil {
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

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) chain.Receiver {
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

func (r *receiver) GetBlockUpdate(height int64) (*chain.BlockUpdate, error) {
	var bu *chain.BlockUpdate
	v := &BlockNotification{Height: big.NewInt(height)}
	bu, err := r.newBlockUpdate(v)
	return bu, err
}
