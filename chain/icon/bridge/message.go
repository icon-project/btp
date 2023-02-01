package bridge

import (
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/types"
)

type ReceiptProof struct {
	Index  int64
	Events []*Event
	Height int64
}

type RelayMessage struct {
	Receipts [][]byte
}

type Receipt struct {
	Index  int64
	Events []byte
	Height int64
}

type Event struct { //EventDataBTPMessage
	Next     string
	Sequence int64
	Message  []byte
}

type relayMessageItem struct {
	it      link.MessageItemType
	rp      *ReceiptProof
	bls     *types.BMCLinkStatus
	payload []byte
}

func (c *relayMessageItem) Type() link.MessageItemType {
	return c.it
}

func (c *relayMessageItem) Bytes() []byte {
	return c.payload
}

func (c *relayMessageItem) Len() int64 {
	return int64(len(c.payload))
}

func (c *relayMessageItem) UpdateBMCLinkStatus(status *types.BMCLinkStatus) error {
	return nil
}

func (c *relayMessageItem) ReceiptProof() *ReceiptProof {
	return c.rp
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

func NewMessageProof(ss, ls int64, pd int, rp *ReceiptProof) *MessageProof {
	return &MessageProof{
		startSeq: ss,
		lastSeq:  ls,
		relayMessageItem: relayMessageItem{
			it:      link.TypeMessageProof,
			payload: codec.RLP.MustMarshalToBytes(rp.Events),
			rp:      rp,
		},
	}
}

type BridgeRelayMessage struct {
	Receipts [][]byte
}
