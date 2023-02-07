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
	"fmt"

	"github.com/attestantio/go-eth2-client/spec/altair"
	"github.com/attestantio/go-eth2-client/spec/phase0"
	ssz "github.com/ferranbt/fastssz"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/types"
)

type BTPRelayMessage struct {
	Messages []*TypePrefixedMessage
}

type relayMessageItem struct {
	it      link.MessageItemType
	pd      int
	payload []byte
}

func (c *relayMessageItem) Type() link.MessageItemType {
	return c.it
}

func (c *relayMessageItem) Len() int64 {
	return int64(len(c.payload))
}

func (c *relayMessageItem) UpdateBMCLinkStatus(status *types.BMCLinkStatus) error {
	// TODO overwrite at bu, bp, mp if necessary
	return nil
}

func (c *relayMessageItem) Payload() []byte {
	return c.payload
}

type blockProof struct {
	relayMessageItem
	ph int64
}

func (c *blockProof) ProofHeight() int64 {
	return c.ph
}

type blockUpdate struct {
	blockProof
	srcHeight    int64
	targetHeight int64
}

func (c *blockUpdate) SrcHeight() int64 {
	return c.srcHeight
}

func (c *blockUpdate) TargetHeight() int64 {
	return c.targetHeight
}

func NewBlockUpdate(srcHeight, targetHeight int64, pd int, v interface{}) *blockUpdate {
	return &blockUpdate{
		srcHeight:    srcHeight,
		targetHeight: targetHeight,
		blockProof: blockProof{
			relayMessageItem: relayMessageItem{
				it:      link.TypeBlockUpdate,
				pd:      pd,
				payload: codec.RLP.MustMarshalToBytes(v),
			},
			ph: targetHeight,
		},
	}
}

type MessageProof struct {
	relayMessageItem
	startSeq int64
	lastSeq  int64
}

func (m *MessageProof) StartSeqNum() int64 {
	return m.startSeq
}

func (m *MessageProof) LastSeqNum() int64 {
	return m.lastSeq
}

func NewMessageProof(ss, ls int64, pd int, v interface{}) *MessageProof {
	return &MessageProof{
		startSeq: ss,
		lastSeq:  ls,
		relayMessageItem: relayMessageItem{
			it:      link.TypeMessageProof,
			pd:      pd,
			payload: codec.RLP.MustMarshalToBytes(v),
		},
	}
}

const (
	RelayMessageTypeReserved = iota
	RelayMessageTypeBlockUpdate
	RelayMessageTypeMessageProof
	RelayMessageTypeBlockProof
)

type TypePrefixedMessage struct {
	Type    int
	Payload []byte
}

func NewTypePrefixedMessage(rmi link.RelayMessageItem) (*TypePrefixedMessage, error) {
	mt := RelayMessageTypeReserved
	switch rmi.Type() {
	case link.TypeBlockUpdate:
		mt = RelayMessageTypeBlockUpdate
	case link.TypeMessageProof:
		mt = RelayMessageTypeMessageProof
	default:
		return nil, fmt.Errorf("invalid valud")
	}
	return &TypePrefixedMessage{
		Type:    mt,
		Payload: rmi.(*relayMessageItem).Payload(),
	}, nil
}

type blockUpdateData struct {
	AttestedHeader          *altair.LightClientHeader
	FinalizedHeader         *altair.LightClientHeader
	FinalizedHeaderBranch   [][]byte
	SyncAggregate           *altair.SyncAggregate
	SignatureSlot           phase0.Slot
	NextSyncCommittee       *altair.SyncCommittee
	NextSyncCommitteeBranch [][]byte
}

func (b *blockUpdateData) RLPEncodeSelf(e codec.Encoder) error {
	e2, err := e.EncodeList()
	if err != nil {
		return err
	}
	ah, err := b.AttestedHeader.MarshalSSZ()
	if err != nil {
		return err
	}
	fh, err := b.FinalizedHeader.MarshalSSZ()
	if err != nil {
		return err
	}
	sa, err := b.SyncAggregate.MarshalSSZ()
	if err != nil {
		return err
	}
	nsc, err := b.NextSyncCommittee.MarshalSSZ()
	if err != nil {
		return err
	}
	if err = e2.EncodeMulti(
		ah, fh, b.FinalizedHeaderBranch, sa, b.SignatureSlot, nsc, b.NextSyncCommitteeBranch,
	); err != nil {
		return err
	}
	return nil
}

func (b *blockUpdateData) RLPDecodeSelf(d codec.Decoder) error {
	d2, err := d.DecodeList()
	if err != nil {
		return err
	}
	var ah, fh, sa, nsc []byte
	if _, err = d2.DecodeMulti(
		&ah, &fh, &b.FinalizedHeaderBranch, &sa, &b.SignatureSlot, &nsc, &b.NextSyncCommitteeBranch,
	); err != nil {
		return err
	}
	b.AttestedHeader = new(altair.LightClientHeader)
	err = b.AttestedHeader.UnmarshalSSZ(ah)
	if err != nil {
		return err
	}
	b.FinalizedHeader = new(altair.LightClientHeader)
	err = b.FinalizedHeader.UnmarshalSSZ(fh)
	if err != nil {
		return err
	}
	b.SyncAggregate = new(altair.SyncAggregate)
	err = b.SyncAggregate.UnmarshalSSZ(sa)
	if err != nil {
		return err
	}
	b.NextSyncCommittee = new(altair.SyncCommittee)
	err = b.NextSyncCommittee.UnmarshalSSZ(nsc)
	if err != nil {
		return err
	}
	return nil
}

type blockProofData struct {
	Header *altair.LightClientHeader
	Proof  *ssz.Proof // proof for BeaconState.BlockRoots or BeaconState.HistoricalRoots
}

func (b *blockProofData) RLPEncodeSelf(e codec.Encoder) error {
	e2, err := e.EncodeList()
	if err != nil {
		return err
	}
	h, err := b.Header.MarshalSSZ()
	if err != nil {
		return err
	}
	if err = e2.EncodeMulti(h, b.Proof); err != nil {
		return err
	}
	return nil
}

func (b *blockProofData) RLPDecodeSelf(d codec.Decoder) error {
	d2, err := d.DecodeList()
	if err != nil {
		return err
	}
	var bs []byte
	if _, err = d2.DecodeMulti(&bs, &b.Proof); err != nil {
		return err
	}
	b.Header = new(altair.LightClientHeader)
	err = b.Header.UnmarshalSSZ(bs)
	if err != nil {
		return err
	}
	return nil
}

type messageProofData struct {
	Slot              phase0.Slot
	ReceiptsRootProof *ssz.Proof
	ReceiptProofs     []*receiptProof

	header *altair.LightClientHeader
}

func (m *messageProofData) RLPEncodeSelf(e codec.Encoder) error {
	e2, err := e.EncodeList()
	if err != nil {
		return err
	}
	if err = e2.EncodeMulti(m.Slot, m.ReceiptsRootProof, m.ReceiptProofs); err != nil {
		return err
	}
	return nil
}

func (m *messageProofData) RLPDecodeSelf(d codec.Decoder) error {
	d2, err := d.DecodeList()
	if err != nil {
		return err
	}
	if _, err = d2.DecodeMulti(&m.Slot, &m.ReceiptsRootProof, &m.ReceiptProofs); err != nil {
		return err
	}
	return nil
}

type receiptProof struct {
	Key   []byte `json:"key"`   // rlp.encode(receipt index)
	Proof []byte `json:"proof"` // proof for receipt
}
