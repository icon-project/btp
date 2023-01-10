package icon

import (
	"encoding/base64"
	"math/big"
	"sync"

	"github.com/icon-project/btp/chain"
	//"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
)

type btp struct {
	l   log.Logger
	dst chain.BtpAddress

	rm          *BTPRelayMessage
	mtx         sync.RWMutex //TODO refactoring
	rmSize      int
	txSizeLimit int
}

func NewBTP(l log.Logger, dst chain.BtpAddress, txSizeLimit int, mtx sync.RWMutex) *btp {
	c := &btp{
		dst:         dst,
		l:           l,
		txSizeLimit: txSizeLimit,
		mtx:         mtx,
		rm: &BTPRelayMessage{
			Messages: make([]*TypePrefixedMessage, 0),
		},
	}

	return c
}

func (c *btp) isOverLimit(size int) bool {
	return c.txSizeLimit < size
}

func (c *btp) addRelayMessage(bu *BTPBlockUpdate, height int64, msgs []string) (*BTPBlockData, error) {
	c.mtx.Lock()
	defer c.mtx.Unlock()
	var mt *mbt.MerkleBinaryTree
	var err error

	if len(msgs) > 0 {
		result := make([][]byte, 0)
		for _, mg := range msgs {
			m, err := base64.StdEncoding.DecodeString(mg)
			if err != nil {
				return nil, err
			}
			result = append(result, m)
		}

		//TODO refactoring networkTypeId
		if mt, err = mbt.NewMerkleBinaryTree(mbt.HashFuncByUID("eth"), result); err != nil {
			return nil, err
		}
	}

	btpBlock := &BTPBlockData{
		Bu: bu,
		Mt: mt, PartialOffset: 0,
		Height: height}

	return btpBlock, nil
}

func (c *btp) segment(bd *BTPBlockData, ss *[]*chain.Segment) error {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	//blockUpdate
	if bd.Bu != nil {
		c.blockUpdateSegment(bd.Bu, bd.Height, ss)
	}
	//messageProof
	if bd.Mt != nil {
		c.messageSegment(bd, ss)
	}

	return nil
}

func (c *btp) addSegment(ss *[]*chain.Segment) error {
	//c.mtx.Lock()
	//defer c.mtx.Unlock()
	b, err := codec.RLP.MarshalToBytes(c.rm)
	if err != nil {
		return err
	}
	s := &chain.Segment{
		Height:           c.rm.Height(),
		TransactionParam: b,
		EventSequence:    big.NewInt(int64(c.rm.MessageSeq())), //TODO refactoring type convert
	}

	*ss = append(*ss, s)
	c.rm.Messages = c.rm.Messages[:0]
	c.rmSize = 0
	return nil
}

func (c *btp) blockUpdateSegment(bu *BTPBlockUpdate, heightOfSrc int64, ss *[]*chain.Segment) error {
	tpm, err := NewTypePrefixedMessage(bu)
	size, err := sizeOfTypePrefixedMessage(tpm)
	if err != nil {
		return err
	}

	if c.isOverLimit(c.rmSize + size) {
		c.addSegment(ss)
	}
	c.rm.SetHeight(heightOfSrc)
	c.rm.AppendMessage(tpm)
	c.rmSize += size
	return nil
}

func (c *btp) messageSegment(bd *BTPBlockData, ss *[]*chain.Segment) error {
	var endIndex int
	c.rm.SetHeight(bd.Height)

	for endIndex = bd.PartialOffset + 1; endIndex <= bd.Mt.Len(); endIndex++ {
		//TODO refactoring
		p, err := bd.Mt.Proof(bd.PartialOffset+1, endIndex)
		if err != nil {
			return err
		}
		tpm, err := NewTypePrefixedMessage(*p)
		if err != nil {
			return err
		}
		size, err := sizeOfTypePrefixedMessage(tpm)
		if err != nil {
			return err
		}

		if c.isOverLimit(c.rmSize + size) {
			c.addSegment(ss)
			c.rm.SetMessageSeq(endIndex)
			c.rm.AppendMessage(tpm)
			bd.PartialOffset = endIndex
			c.rmSize += size
		}

		if bd.Mt.Len() == endIndex && bd.PartialOffset != endIndex {
			c.rm.SetMessageSeq(endIndex)
			c.rm.AppendMessage(tpm)
			bd.PartialOffset = bd.Mt.Len()
			c.rmSize += size
		}

	}
	return nil
}

func (c *btp) Segments(bu *BTPBlockUpdate, height, seq int64, maxSizeTx bool,
	msgs []string, offset int64, ss *[]*chain.Segment) error {

	bd, err := c.addRelayMessage(bu, height, msgs)
	if err != nil {
		return nil
	}

	if err = c.segment(bd, ss); err != nil {
		return err
	}

	if !maxSizeTx {
		if c.rmSize > 0 {
			if err = c.addSegment(ss); err != nil {
				return err
			}
		}
	}

	return nil
}

func (c *btp) RemoveSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	ssIndex := 0
	for i, s := range ss {
		if (s.Height == bs.Verifier.Height || s.EventSequence == ss[i].EventSequence) || s.Height < bs.Verifier.Height {
			ssIndex = i
		}
	}
	c.l.Debugf("remove segment h:%d seq:%d", bs.Verifier.Height, ss[ssIndex].EventSequence)
	ss = ss[ssIndex:]

}

func (c *btp) UpdateSegment(bs *chain.BMCLinkStatus, ss []*chain.Segment) error {
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

//TODO refactoring
func sizeOfTypePrefixedMessage(tpm *TypePrefixedMessage) (int, error) {
	b, err := codec.RLP.MarshalToBytes(tpm)
	if err != nil {
		return 0, err
	}
	return len(b), nil
}
