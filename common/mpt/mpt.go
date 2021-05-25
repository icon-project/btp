package mpt

import (
	"bytes"
	"fmt"

	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/goloop/common/codec"
)

//refer service/txresult/receiptlist.go:59 receiptList.GetProof(n int) ([][]byte, error)
//refer common/trie/ompt/mpt.go:263 mpt.GetProof(k []byte) [][]byte
type MptNode struct {
	Header *MptHeader
	Link   []*MptLink
	Data   []byte
}
type MptHeader struct {
	Prefix  byte
	Nibbles []byte
}

func (mh *MptHeader) IsLeaf() bool {
	return (mh.Prefix & 0x20) != 0
}

type MptLink struct {
	Hash []byte
	ptr  *MptNode
}

func (n *MptNode) RLPEncodeSelf(e codec.Encoder) error {
	return fmt.Errorf("not implemented")
}
func (n *MptNode) RLPDecodeSelf(d codec.Decoder) error {
	var bl [][]byte
	if err := d.Decode(&bl); err != nil {
		return err
	}
	switch len(bl) {
	case 2:
		n.Header = &MptHeader{bl[0][0], bl[0][1:]}
		if n.Header.IsLeaf() {
			n.Data = bl[1]
		} else {
			n.Link = make([]*MptLink, 1)
			n.Link[0] = &MptLink{Hash: bl[1]}
		}
	case 17:
		n.Link = make([]*MptLink, 16)
		for i, b := range bl[:16] {
			n.Link[i] = &MptLink{Hash: b}
		}
		n.Data = bl[16]
	default:
		return fmt.Errorf("invalid list length %d", len(bl))
	}
	return nil
}

type MptProof struct {
	Nodes  []MptNode
	Hashes [][]byte
}

func (mp *MptProof) Leaf() *MptNode {
	return &mp.Nodes[len(mp.Nodes)-1]
}

func NewMptProof(bl [][]byte) (*MptProof, error) {
	mp := &MptProof{
		Nodes:  make([]MptNode, len(bl)),
		Hashes: make([][]byte, len(bl)),
	}
	for i, b := range bl {
		_, err := codec.RLP.UnmarshalFromBytes(b, &mp.Nodes[i])
		if err != nil {
			return nil, err
		}
		mp.Hashes[i] = crypto.SHA3Sum256(b)
	}
Loop:
	for i := 0; i < len(bl); i++ {
		mn := mp.Nodes[i]
		if mn.Header != nil {
			if !mn.Header.IsLeaf() {
				ml := mn.Link[0]
				if !bytes.Equal(ml.Hash, mp.Hashes[i+1]) {
					return nil, fmt.Errorf("invalid link[%d] hash[%d]", i, i+1)
				}
				ml.ptr = &mp.Nodes[i+1]
			}
		} else {
			for _, ml := range mn.Link {
				if len(ml.Hash) > 0 && bytes.Equal(ml.Hash, mp.Hashes[i+1]) {
					ml.ptr = &mp.Nodes[i+1]
					continue Loop
				}
			}
			return nil, fmt.Errorf("invalid branch[%d], not found node hash:%x", i, mp.Hashes[i+1])
		}
	}
	return mp, nil
}
