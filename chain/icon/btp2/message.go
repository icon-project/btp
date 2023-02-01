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
	payload []byte
}

func (c *relayMessageItem) Type() link.MessageItemType {
	return c.it
}

func (c *relayMessageItem) Len() int64 {
	return int64(len(c.payload))
}

func (c *relayMessageItem) UpdateBMCLinkStatus(status *types.BMCLinkStatus) error {
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

func NewBlockUpdate(srcHeight, targetHeight int64, v interface{}) *blockUpdate {
	return &blockUpdate{
		srcHeight:    srcHeight,
		targetHeight: targetHeight,
		blockProof: blockProof{
			relayMessageItem: relayMessageItem{
				it:      link.TypeBlockUpdate,
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

func NewMessageProof(ss, ls int64, v interface{}) *MessageProof {
	return &MessageProof{
		startSeq: ss,
		lastSeq:  ls,
		relayMessageItem: relayMessageItem{
			it:      link.TypeMessageProof,
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
