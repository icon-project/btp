package btp2

import (
	"fmt"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/types"
)

type BTPRelayMessage struct {
	Messages []*TypePrefixedMessage
}

type relayMessageItem struct {
	it      link.MessageItemType
	nextBls *types.BMCLinkStatus
	payload []byte
}

func (c *relayMessageItem) Type() link.MessageItemType {
	return c.it
}

func (c *relayMessageItem) Len() int64 {
	return int64(len(c.payload))
}

func (c *relayMessageItem) UpdateBMCLinkStatus(bls *types.BMCLinkStatus) error {
	bls.Verifier.Height = c.nextBls.Verifier.Height
	bls.RxSeq = c.nextBls.RxSeq
	bls.TxSeq = c.nextBls.TxSeq
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

func NewBlockUpdate(bs *types.BMCLinkStatus, targetHeight int64, v interface{}) *blockUpdate {
	nextBls := &types.BMCLinkStatus{}
	nextBls.Verifier.Height = targetHeight
	nextBls.TxSeq = bs.TxSeq
	nextBls.RxSeq = bs.RxSeq
	return &blockUpdate{
		srcHeight:    bs.Verifier.Height,
		targetHeight: targetHeight,
		blockProof: blockProof{
			relayMessageItem: relayMessageItem{
				it:      link.TypeBlockUpdate,
				payload: codec.RLP.MustMarshalToBytes(v),
				nextBls: nextBls,
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

func NewMessageProof(bs *types.BMCLinkStatus, ls int64, v interface{}) *MessageProof {
	nextBls := &types.BMCLinkStatus{}
	nextBls.Verifier.Height = bs.Verifier.Height
	nextBls.TxSeq = bs.TxSeq
	nextBls.RxSeq = ls

	return &MessageProof{
		startSeq: bs.RxSeq,
		lastSeq:  ls,
		relayMessageItem: relayMessageItem{
			it:      link.TypeMessageProof,
			payload: codec.RLP.MustMarshalToBytes(v),
			nextBls: nextBls,
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
	tpm := &TypePrefixedMessage{}
	switch rmi.Type() {
	case link.TypeBlockUpdate:
		bn := rmi.(*blockUpdate)
		tpm.Type = RelayMessageTypeBlockUpdate
		tpm.Payload = bn.Payload()
	case link.TypeMessageProof:
		bn := rmi.(*MessageProof)
		tpm.Type = RelayMessageTypeMessageProof
		tpm.Payload = bn.Payload()
	default:
		return nil, fmt.Errorf("in valid valud")
	}
	return tpm, nil
}
