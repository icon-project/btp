/*
 * Copyright 2023 ICON Foundation
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

package eth2

import (
	"encoding/binary"
	"fmt"
	"math"
	"math/big"
	"strconv"

	api "github.com/attestantio/go-eth2-client/api/v1"
	"github.com/attestantio/go-eth2-client/spec/altair"
	"github.com/attestantio/go-eth2-client/spec/phase0"
	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/rawdb"
	etypes "github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/light"
	"github.com/ethereum/go-ethereum/rlp"
	"github.com/ethereum/go-ethereum/trie"
	ssz "github.com/ferranbt/fastssz"

	"github.com/icon-project/btp/chain/eth2/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
	"github.com/icon-project/btp/common/types"
)

const (
	eventSignature = "Message(string,uint256,bytes)"
)

type receiveStatus struct {
	seq int64 // last sequence number of block
	bu  *blockUpdateData

	mt *mbt.MerkleBinaryTree
}

func (r *receiveStatus) Height() int64 {
	return int64(r.bu.FinalizedHeader.Beacon.Slot)
}

func (r *receiveStatus) Seq() int64 {
	return r.seq
}

func (r *receiveStatus) MerkleBinaryTree() *mbt.MerkleBinaryTree {
	return r.mt
}

func newReceiveStatus(seq int64, bu *blockUpdateData) *receiveStatus {
	return &receiveStatus{
		seq: seq,
		bu:  bu,
	}
}

type receiver struct {
	l   log.Logger
	src types.BtpAddress
	dst types.BtpAddress
	cl  *client.ConsensusLayer
	el  *client.ExecutionLayer
	nid int64
	rsc chan link.ReceiveStatus
	rss []*receiveStatus // with finalized header
	mps []*messageProofData
	fq  *ethereum.FilterQuery
}

func NewReceiver(src, dst types.BtpAddress, clEndpoint, elEndpoint string, l log.Logger) link.Receiver {
	var err error
	r := &receiver{
		src: src,
		dst: dst,
		l:   l,
		rsc: make(chan link.ReceiveStatus),
		rss: make([]*receiveStatus, 0),
		fq: &ethereum.FilterQuery{
			Addresses: []common.Address{common.HexToAddress(src.ContractAddress())},
			Topics: [][]common.Hash{
				{crypto.Keccak256Hash([]byte(eventSignature))},
			},
		},
	}
	r.cl, err = client.NewConsensusLayer(clEndpoint, l)
	if err != nil {
		l.Panicf("failed to connect to %s, %v", clEndpoint, err)
	}
	r.el, err = client.NewExecutionLayer(elEndpoint, l)
	if err != nil {
		l.Panicf("failed to connect to %s, %v", elEndpoint, err)
	}
	return r
}

func (r *receiver) Start(bls *types.BMCLinkStatus) (<-chan link.ReceiveStatus, error) {
	// TODO need initialization?

	//TODO refactoring
	go func() {
		r.Monitoring(bls)
	}()

	return r.rsc, nil
}

func (r *receiver) Stop() {
	close(r.rsc)
}

func (r *receiver) GetStatus() (link.ReceiveStatus, error) {
	return r.rss[len(r.rss)-1], nil
}

func (r *receiver) GetHeightForSeq(seq int64) int64 {
	return r.GetReceiveStatusForSequence(seq).Height()
}

func (r *receiver) GetReceiveStatusForSequence(seq int64) *receiveStatus {
	for _, rs := range r.rss {
		if seq < rs.Seq() {
			return rs
		}
	}
	return nil
}

func (r *receiver) GetMessageProofDataForHeight(height int64) *messageProofData {
	for _, mp := range r.mps {
		if height == mp.Slot {
			return mp
		}
	}
	return nil
}

func (r *receiver) GetMarginForLimit() int64 {
	return 0
}

func (r *receiver) BuildBlockUpdate(bls *types.BMCLinkStatus, limit int64) ([]link.BlockUpdate, error) {
	r.clearData(bls)
	bus := make([]link.BlockUpdate, 0)
	for _, rs := range r.rss {
		bu := NewBlockUpdate(bls, rs.Height(), rs.bu)
		bus = append(bus, bu)
	}
	return bus, nil
}

func (r *receiver) BuildBlockProof(bls *types.BMCLinkStatus, height int64) (link.BlockProof, error) {
	mp := r.GetMessageProofDataForHeight(height)
	if mp == nil {
		return nil, fmt.Errorf("invalid height %d", height)
	}

	// make BlockProof for mp
	path := fmt.Sprintf("[\"blocks\", \"%d\"]", SlotToHistoricalIndex(phase0.Slot(mp.Slot)))
	proof, err := r.cl.GetStateProofWithPath("finalized", path)
	if err != nil {
		return nil, err
	}
	bp, err := treeOffsetProofToSSZProof(proof)
	if err != nil {
		return nil, err
	}
	bpd := &blockProofData{
		Header: mp.Header,
		Proof:  bp,
	}
	return &BlockProof{
		relayMessageItem: relayMessageItem{
			it:      link.TypeBlockProof,
			payload: codec.RLP.MustMarshalToBytes(bpd),
		},
		ph: height,
	}, nil
}

func (r *receiver) BuildMessageProof(bls *types.BMCLinkStatus, limit int64) (link.MessageProof, error) {
	var offset int
	rs := r.GetReceiveStatusForSequence(bls.RxSeq)
	if rs == nil {
		return nil, nil
	}

	//TODO refactoring
	messageCnt := rs.MerkleBinaryTree().Len()
	if messageCnt > 0 {
		for i := offset + 1; i < messageCnt; i++ {
			p, err := rs.MerkleBinaryTree().Proof(offset+1, i)
			if err != nil {
				return nil, err
			}

			if limit < int64(len(codec.RLP.MustMarshalToBytes(p))) {
				mp := NewMessageProof(bls, bls.RxSeq+int64(i), *p)
				return mp, nil
			}
		}
	}

	p, err := rs.MerkleBinaryTree().Proof(1, messageCnt)
	if err != nil {
		return nil, err
	}
	mp := NewMessageProof(bls, bls.RxSeq /*+int64(messageCnt)*/, *p)
	return mp, nil
}

func (r *receiver) BuildRelayMessage(rmis []link.RelayMessageItem) ([]byte, error) {
	bm := &BTPRelayMessage{
		Messages: make([]*TypePrefixedMessage, 0),
	}

	for _, rmi := range rmis {
		tpm, err := NewTypePrefixedMessage(rmi)
		if err != nil {
			return nil, err
		}
		bm.Messages = append(bm.Messages, tpm)
	}

	rb, err := codec.RLP.MarshalToBytes(bm)
	if err != nil {
		return nil, err
	}

	return rb, nil
}

func (r *receiver) clearData(bls *types.BMCLinkStatus) {
	for i, rs := range r.rss {
		if rs.Height() == bls.Verifier.Height && rs.Seq() == bls.RxSeq {
			r.rss = r.rss[i+1:]
			return
		}
	}
	for i, mp := range r.mps {
		if mp.Height() == bls.Verifier.Height && mp.Seq() == bls.RxSeq {
			r.mps = r.mps[i+1:]
		}
	}
}

func (r *receiver) Monitoring(bls *types.BMCLinkStatus) error {
	if bls.Verifier.Height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	eth2Topics := []string{client.TopicLCOptimisticUpdate, client.TopicLCFinalityUpdate}
	for {
		if err := r.cl.Events(eth2Topics, func(event *api.Event) {
			if event.Topic == client.TopicLCOptimisticUpdate {
				update := event.Data.(*altair.LightClientOptimisticUpdate)
				mp, err := r.makeMessageProofData(
					bls,
					int64(update.AttestedHeader.Beacon.Slot),
					update.AttestedHeader.Beacon,
				)
				r.l.Debugf("failed to make messageProofData. %+v", err)
				r.mps = append(r.mps, mp)
			} else if event.Topic == client.TopicLCFinalityUpdate {
				bu, err := r.handleFinalityUpdate(bls, event.Data.(*altair.LightClientFinalityUpdate))
				if err != nil {
					r.l.Debugf("failed to make blockUpdateData. %+v", err)
				}
				lrs := r.rss[len(r.rss)-1]
				rs := newReceiveStatus(lrs.Seq(), bu)
				r.rss = append(r.rss, rs)
				r.rsc <- rs
			}
		}); err != nil {
			r.l.Debugf("onError %+v", err)
		}
	}
	return nil
}

func (r *receiver) handleFinalityUpdate(
	bls *types.BMCLinkStatus,
	update *altair.LightClientFinalityUpdate,
) (*blockUpdateData, error) {
	var nsc *altair.SyncCommittee
	var nscBranch [][]byte

	slot := update.FinalizedHeader.Beacon.Slot
	if IsSyncCommitteeEdge(slot) {
		lcUpdate, err := r.cl.LightClientUpdates(SyncCommitteePeriodAtSlot(update.FinalizedHeader.Beacon.Slot), 1)
		if err != nil {
			return nil, err
		}
		if len(lcUpdate) != 1 {
			return nil, fmt.Errorf("invalid light client updates length")
		}
		nsc = lcUpdate[0].NextSyncCommittee
		copy(nscBranch, lcUpdate[0].NextSyncCommitteeBranch)
	}

	bu := &blockUpdateData{
		AttestedHeader:          update.AttestedHeader,
		FinalizedHeader:         update.FinalizedHeader,
		FinalizedHeaderBranch:   update.FinalityBranch,
		SyncAggregate:           update.SyncAggregate,
		SignatureSlot:           update.SignatureSlot,
		NextSyncCommittee:       nsc,
		NextSyncCommitteeBranch: nscBranch,
	}
	return bu, nil
}

var eventSignatureTopic = crypto.Keccak256([]byte(eventSignature))

func (r *receiver) makeMessageProofData(
	bls *types.BMCLinkStatus,
	slot int64,
	header *phase0.BeaconBlockHeader,
) (mp *messageProofData, err error) {
	beaconBlock, err := r.cl.BeaconBlock(strconv.FormatInt(slot, 10))
	if err != nil {
		return
	}
	elBlockNum := big.NewInt(int64(beaconBlock.Bellatrix.Message.Body.ExecutionPayload.BlockNumber))
	elBlock, err := r.el.BlockByNumber(elBlockNum)
	if err != nil {
		return
	}

	logs, err := r.getEventLogs(elBlock, bls.RxSeq)
	if err != nil {
		return
	}
	if len(logs) == 0 {
		return
	}

	// make receiptProofs for receipts
	receipts, err := r.getReceipts(elBlock)
	if err != nil {
		return
	}

	receiptProofs, err := makeReceiptProofs(receipts, logs)
	if err != nil {
		return
	}

	// make receiptsRootProof
	rrProof, err := r.cl.GetReceiptsRootProof(slot)
	if err != nil {
		return
	}
	receiptsRootProof, err := treeOffsetProofToSSZProof(rrProof)
	if err != nil {
		return
	}

	bms, _ := client.UnpackEventLog(logs[0].Data)
	bme, _ := client.UnpackEventLog(logs[len(logs)-1].Data)

	mp = &messageProofData{
		Slot:              slot,
		ReceiptsRootProof: receiptsRootProof,
		ReceiptProofs:     receiptProofs,
		Header:            header,
		StartSeq:          bms.Seq.Int64(),
		EndSeq:            bme.Seq.Int64(),
	}

	return
}

func (r *receiver) getEventLogs(block *etypes.Block, startSeq int64) ([]etypes.Log, error) {
	if !block.Bloom().Test(eventSignatureTopic) {
		return nil, nil
	}

	fq := *r.fq
	bHash := block.Hash()
	fq.BlockHash = &bHash
	logs, err := r.el.FilterLogs(fq)
	for err != nil {
		return nil, err
	}

	seq := startSeq
	for _, l := range logs {
		bm, err := client.UnpackEventLog(l.Data)
		if err != nil {
			return nil, err
		}
		if seq != bm.Seq.Int64() {
			return nil, fmt.Errorf("sequence number of BTP message is not continuous")
		}
		r.l.Debugf("BTPMessage[seq:%d next:%s] seq:%d dst:%s", bm.Seq, bm.Next, startSeq, r.dst.String())
		seq += 1
	}
	return logs, nil
}

func (r *receiver) getReceipts(block *etypes.Block) ([]*etypes.Receipt, error) {
	size := len(block.Transactions())
	receipts := make([]*etypes.Receipt, size, size)
	for i, tx := range block.Transactions() {
		receipt, err := r.el.TransactionReceipt(tx.Hash())
		if err != nil {
			return nil, err
		}
		receipts[i] = receipt
	}
	return receipts, nil
}

func treeOffsetProofToSSZProof(data []byte) (*ssz.Proof, error) {
	proofType := int(data[0])
	if proofType != 1 {
		return nil, fmt.Errorf("invalid proof type. %d", proofType)
	}
	dataOffset := 1
	// leaf count
	leafCount := int(binary.LittleEndian.Uint16(data[dataOffset : dataOffset+2]))
	if len(data) < (leafCount-1)*2+leafCount*32 {
		return nil, fmt.Errorf("unable to deserialize tree offset proof: not enough bytes. %+v", data)
	}
	dataOffset += 2

	// offsets
	offsets := make([]uint16, leafCount-1, leafCount-1)
	for i := 0; i < leafCount-1; i++ {
		offsets[i] = binary.LittleEndian.Uint16(data[dataOffset+i*2 : dataOffset+i*2+2])
	}
	dataOffset += 2 * (leafCount - 1)

	// leaves
	leaves := make([][]byte, leafCount, leafCount)
	for i := 0; i < leafCount; i++ {
		leaves[i] = data[dataOffset : dataOffset+32]
		fmt.Printf("%d: %#x\n", i, leaves[i])
		dataOffset += 32
	}

	node, err := treeOffsetProofToNode(offsets, leaves)
	if err != nil {
		return nil, err
	}

	gIndex := offsetsToGIndex(offsets)

	return node.Prove(gIndex)
}

func offsetsToGIndex(offsets []uint16) int {
	base := int(math.Pow(2, float64(len(offsets))))
	value := 0
	for _, offset := range offsets {
		value = value << 1
		if offset == 1 {
			value |= 1
		}
	}
	return base + value
}

// treeOffsetProofToNode Recreate a `Node` given offsets and leaves of a tree-offset proof
// See https://github.com/protolambda/eth-merkle-trees/blob/master/tree_offsets.md
func treeOffsetProofToNode(offsets []uint16, leaves [][]byte) (*ssz.Node, error) {
	if len(leaves) == 0 {
		return nil, fmt.Errorf("proof must contain gt 0 leaves")
	} else if len(leaves) == 1 {
		return ssz.LeafFromBytes(leaves[0]), nil
	} else {
		// the offset popped from the list is the # of leaves in the left subtree
		pivot := offsets[0]
		left, err := treeOffsetProofToNode(offsets[1:pivot], leaves[0:pivot])
		if err != nil {
			return nil, err
		}
		right, err := treeOffsetProofToNode(offsets[pivot:], leaves[pivot:])
		if err != nil {
			return nil, err
		}
		return ssz.NewNodeWithLR(left, right), nil
	}
}

// makeReceiptProofs make EL Receipt Trie and proof for receipt which has BTP message
func makeReceiptProofs(receipts []*etypes.Receipt, logs []etypes.Log) ([]*receiptProof, error) {
	receiptTrie, _, err := trieFromReceipts(receipts)
	if err != nil {
		return nil, err
	}

	return getReceiptProofs(receiptTrie, logs)
}

// trieFromReceipts make receipt MPT with receipts
func trieFromReceipts(receipts etypes.Receipts) (tr *trie.Trie, db *trie.Database, err error) {
	db = trie.NewDatabase(rawdb.NewMemoryDatabase())
	tr = trie.NewEmpty(db)

	for i := 0; i < receipts.Len(); i++ {
		var path, rawReceipt []byte
		path, err = rlp.EncodeToBytes(uint64(i))
		if err != nil {
			return
		}

		rawReceipt, err = receipts[i].MarshalBinary()
		if err != nil {
			return
		}
		tr.Update(path, rawReceipt)
	}
	return
}

func getReceiptProofs(tr *trie.Trie, logs []etypes.Log) ([]*receiptProof, error) {
	keys := make(map[uint]bool)
	rps := make([]*receiptProof, 0)
	for _, l := range logs {
		if _, ok := keys[l.TxIndex]; ok {
			continue
		}
		keys[l.TxIndex] = true
		key, err := rlp.EncodeToBytes(uint64(l.TxIndex))
		if err != nil {
			return nil, err
		}
		nodes := light.NewNodeSet()
		err = tr.Prove(key, 0, nodes)
		if err != nil {
			return nil, err
		}
		proof, err := rlp.EncodeToBytes(nodes.NodeList())
		if err != nil {
			return nil, err
		}
		rps = append(rps, &receiptProof{Key: key, Proof: proof})
	}
	return rps, nil
}
