package icon

import (
	"fmt"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/mbt"
	"unsafe"
)

const (
	RelayMessageTypeReserved = iota
	RelayMessageTypeBlockUpdate
	RelayMessageTypeMessageProof
	RelayMessageTypeBlockProof
)

type BTPRelayMessage struct {
	height     int64
	messageSeq int
	Messages   []*TypePrefixedMessage
}

func (rm *BTPRelayMessage) Height() int64 {
	return rm.height
}

func (rm *BTPRelayMessage) MessageSeq() int {
	return rm.messageSeq
}

func (rm *BTPRelayMessage) SetHeight(height int64) {
	rm.height = height
}

func (rm *BTPRelayMessage) SetMessageSeq(seq int) {
	rm.messageSeq = seq
}

func (rm *BTPRelayMessage) AppendMessage(tpm *TypePrefixedMessage) {
	rm.Messages = append(rm.Messages, tpm)
}

func (rm *BTPRelayMessage) Size() int {
	var size int
	for _, tm := range rm.Messages {
		size += int(unsafe.Sizeof(tm))
	}
	return size
}

type TypePrefixedMessage struct {
	Type    int
	Payload []byte
}

func NewTypePrefixedMessage(v interface{}) (*TypePrefixedMessage, error) {
	mt := RelayMessageTypeReserved
	switch v.(type) {
	case BTPBlockUpdate, *BTPBlockUpdate:
		mt = RelayMessageTypeBlockUpdate
	case mbt.MerkleBinaryTreeProof, *mbt.MerkleBinaryTreeProof:
		mt = RelayMessageTypeMessageProof
	default:
		return nil, fmt.Errorf("invalid valud")
	}
	return &TypePrefixedMessage{
		Type:    mt,
		Payload: codec.RLP.MustMarshalToBytes(v),
	}, nil
}

func NewRelayMessage() *BTPRelayMessage {
	rm := &BTPRelayMessage{
		Messages: make([]*TypePrefixedMessage, 0),
	}
	return rm
}

type BTPBlockData struct {
	Bu            *BTPBlockUpdate
	Mt            *mbt.MerkleBinaryTree
	PartialOffset int
	Seq           uint64
	HeightOfDst   int64
	HeightOfSrc   int64
}
