package icon

import (
	"encoding/base64"
	"math/big"
	"sync"
	"unsafe"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

type bridge struct {
	l   log.Logger
	dst chain.BtpAddress

	rm          *chain.RelayMessage
	mtx         sync.RWMutex //TODO refactoring
	rmSize      int
	txSizeLimit int
}

type BridgeRelayMessage struct {
	Receipts [][]byte
}

type Receipt struct {
	Index  int64
	Events []byte
	Height int64
}

func NewBridge(l log.Logger, dst chain.BtpAddress, txSizeLimit int, mtx sync.RWMutex) *bridge {
	c := &bridge{
		dst:         dst,
		l:           l,
		txSizeLimit: txSizeLimit,
		mtx:         mtx,
		rm: &chain.RelayMessage{
			ReceiptProofs: make([]*chain.ReceiptProof, 0),
		},
	}

	return c
}

func (c *bridge) makeReceiptProofList(bu *BTPBlockUpdate, seq int64,
	msgs []string, offset int64) ([]*chain.ReceiptProof, error) {
	bh := &BTPBlockHeader{}
	if _, err := codec.RLP.UnmarshalFromBytes(bu.BTPBlockHeader, bh); err != nil {
		return nil, err
	}
	if bh.MessageCount == 0 {
		return nil, nil
	}
	sn := offset + bh.UpdateNumber>>1
	rps := make([]*chain.ReceiptProof, 0)
	evts := make([]*chain.Event, 0)

	for _, msg := range msgs {
		if sn > seq {
			evt, err := messageToEvent(c.dst.String(), msg, sn)
			if err != nil {
				return nil, err
			}
			evts = append(evts, evt)
		}
		sn++
	}
	if len(evts) == 0 {
		return nil, nil
	}
	rp := &chain.ReceiptProof{
		Index:  0,
		Events: evts,
		Height: bh.MainHeight,
	}
	rps = append(rps, rp)
	return rps, nil
}

func sizeOfEvent(rp *chain.Event) int {
	return int(unsafe.Sizeof(rp))
}

func (c *bridge) isOverLimit(size int) bool {
	return c.txSizeLimit < size
}

func (c *bridge) addSegment(ss []*chain.Segment) error {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	rm := &BridgeRelayMessage{
		Receipts: make([][]byte, 0),
	}
	var (
		b   []byte
		err error
	)
	numOfEvents := 0
	for _, rp := range c.rm.ReceiptProofs {
		if len(rp.Events) == 0 {
			continue
		}
		numOfEvents += len(rp.Events)
		if b, err = codec.RLP.MarshalToBytes(rp.Events); err != nil {
			return err
		}
		r := &Receipt{
			Index:  int64(rp.Index),
			Events: b,
			Height: rp.Height,
		}
		if b, err = codec.RLP.MarshalToBytes(r); err != nil {
			return err
		}
		rm.Receipts = append(rm.Receipts, b)
	}
	if b, err = codec.RLP.MarshalToBytes(rm); err != nil {
		return err
	}
	lrp := c.rm.ReceiptProofs[len(c.rm.ReceiptProofs)-1]
	le := lrp.Events[len(lrp.Events)-1]
	s := &chain.Segment{
		TransactionParam: b,
		Height:           lrp.Height,
		EventSequence:    le.Sequence,
		NumberOfEvent:    numOfEvents,
	}
	ss = append(ss, s)
	lrp.Events = lrp.Events[:0]
	c.rm.ReceiptProofs[0] = lrp
	c.rm.ReceiptProofs = c.rm.ReceiptProofs[:1]
	c.rmSize = 0
	return nil
}

//TODO rename func
func (c *bridge) Segments(bu *BTPBlockUpdate, seq int64, maxSizeTx bool,
	msgs []string, offset int64, ss []*chain.Segment) error {
	var err error
	rps, err := c.makeReceiptProofList(bu, seq, msgs, offset)

	for _, rp := range rps {
		trp := &chain.ReceiptProof{
			Index:  rp.Index,
			Events: make([]*chain.Event, 0),
			Height: rp.Height,
		}
		c.rm.ReceiptProofs = append(c.rm.ReceiptProofs, trp)

		for _, e := range rp.Events {
			size := sizeOfEvent(e)
			if c.isOverLimit(c.rmSize+size) && c.rmSize > 0 {
				if err = c.addSegment(ss); err != nil {
					return err
				}
			}
			trp.Events = append(trp.Events, e)
			c.rmSize += size
		}

		//last event
		if c.isOverLimit(c.rmSize) {
			if err = c.addSegment(ss); err != nil {
				return err
			}
		}

		//remove last receipt if empty
		if len(trp.Events) == 0 {
			c.rm.ReceiptProofs = c.rm.ReceiptProofs[:len(c.rm.ReceiptProofs)-1]
		}
	}

	if !maxSizeTx {
		//immediately relay
		if c.rmSize > 0 {
			if err = c.addSegment(ss); err != nil {
				return err
			}
		}
	}

	return nil
}

//TODO rename func
func (c *bridge) RemoveSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) {
	r := 0
	for i, s := range ss {
		if s.EventSequence.Int64() <= bs.RxSeq.Int64() {
			r = i + 1
		}
	}
	c.removeSegment(ss, r)
}

//TODO rename func
func (c *bridge) UpdateSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) error {

	vs := &VerifierStatus{}
	_, err := codec.RLP.UnmarshalFromBytes(bs.Verifier.Extra, vs)
	if err != nil {
		return err
	}

	if !(bs.RxSeq.Int64() < ss[0].Height) {
		for i := 0; i < len(ss); i++ {
			if (bs.RxSeq.Int64() <= ss[i].Height) || (ss[i].Height > bs.Verifier.Height) {
				ss[i].GetResultParam = nil
			}
		}
	} else {
		c.l.Panicf("No relay messages collected.")
	}

	return nil
}

func messageToEvent(next, msg string, seq int64) (*chain.Event, error) {
	b, err := base64.StdEncoding.DecodeString(msg)
	if err != nil {
		return nil, err
	}
	evt := &chain.Event{
		Next:     chain.BtpAddress(next),
		Sequence: big.NewInt(seq),
		Message:  b,
	}
	return evt, nil
}

func (c *bridge) removeSegment(ss []*chain.Segment, offset int) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	if offset < 1 {
		return
	}
	c.l.Debugf("removeSegment ss:%d removeRelayMessage %d ~ %d",
		len(ss),
		ss[0].EventSequence,
		ss[offset-1].EventSequence)
	ss = ss[offset:]
}
