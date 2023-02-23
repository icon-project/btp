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
	nextBls *types.BMCLinkStatus
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

func (c *relayMessageItem) UpdateBMCLinkStatus(bls *types.BMCLinkStatus) error {
	bls.Verifier.Height = c.nextBls.Verifier.Height
	bls.RxSeq = c.nextBls.RxSeq
	bls.TxSeq = c.nextBls.TxSeq
	return nil
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

type blockProof struct {
	relayMessageItem
	ph int64
}

func (c *blockProof) ProofHeight() int64 {
	return c.ph
}

func NewBlockUpdate(bs *types.BMCLinkStatus, targetHeight int64) *blockUpdate {
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

func NewMessageProof(bs *types.BMCLinkStatus, ls int64, rp *ReceiptProof) (*MessageProof, error) {
	//update bls
	nextBls := &types.BMCLinkStatus{}
	nextBls.Verifier.Height = bs.Verifier.Height
	nextBls.TxSeq = bs.TxSeq
	nextBls.RxSeq = ls

	rm := &BridgeRelayMessage{
		Receipts: make([][]byte, 0),
	}

	var (
		b   []byte
		err error
	)
	numOfEvents := 0
	numOfEvents += len(rp.Events)
	if b, err = codec.RLP.MarshalToBytes(rp.Events); err != nil {
		return nil, err
	}

	r := &Receipt{
		Index:  rp.Index,
		Events: b,
		Height: rp.Height,
	}
	if b, err = codec.RLP.MarshalToBytes(r); err != nil {
		return nil, err
	}
	rm.Receipts = append(rm.Receipts, b)

	if b, err = codec.RLP.MarshalToBytes(rm); err != nil {
		return nil, err
	}

	return &MessageProof{
		startSeq: bs.RxSeq,
		lastSeq:  ls,
		relayMessageItem: relayMessageItem{
			it:      link.TypeMessageProof,
			payload: b,
			nextBls: nextBls,
		},
	}, nil
}

type BridgeRelayMessage struct {
	Receipts [][]byte
}
