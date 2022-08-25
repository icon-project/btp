package icon

import (
	"fmt"
	"math/big"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/mbt"
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
	segments   *chain.Segment
}

func (rm *BTPRelayMessage) Height() int64 {
	return rm.height
}

func (rm *BTPRelayMessage) MessageSeq() int {
	return rm.messageSeq
}

func (rm *BTPRelayMessage) Segments() *chain.Segment {
	return rm.segments
}

func (rm *BTPRelayMessage) SetHeight(height int64) {
	rm.height = height
}

func (rm *BTPRelayMessage) SetMessageSeq(seq int) {
	rm.messageSeq = seq
}

func (rm *BTPRelayMessage) SetSegments(tpm []byte, height int64, seq int64) {
	rm.segments = &chain.Segment{
		Height:           height,
		TransactionParam: tpm,
		EventSequence:    big.NewInt(seq),
	}
}

func (rm *BTPRelayMessage) AppendMessage(tpm *TypePrefixedMessage) {
	rm.Messages = append(rm.Messages, tpm)
}

func (rm *BTPRelayMessage) Size() (int, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	return len(b), err
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
	Height        int64
	MessageCnt    int64
	Bu            *BTPBlockUpdate
	Mt            *mbt.MerkleBinaryTree
	PartialOffset int
}
