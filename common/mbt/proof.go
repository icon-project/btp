package mbt

import (
	"encoding/hex"
	"fmt"
)

func NumberToLevel(n int) int {
	if n == 0 {
		return levelLeaf - 1
	}
	if n == 1 {
		return levelLeaf
	}
	if n == 2 {
		return levelBranch
	}
	l := levelBranch + 1
	for n = (n - 1) >> 2; n > 0; n = n >> 1 {
		l++
	}
	return l
}

func (n *node) proof(num int) (l, r []ProofNode) {
	if n.level < levelBranch {
		l = make([]ProofNode, 0)
		r = make([]ProofNode, 0)
		return
	}
	maxNumChild := 1 << (n.level - levelBranch)
	if num > maxNumChild {
		l, r = n.r.proof(num - maxNumChild)
		l = append(l, ProofNode{NumOfLeaf: n.l.numOfLeaf, Value: n.l.h})
	} else {
		l, r = n.l.proof(num)
		r = append(r, ProofNode{NumOfLeaf: n.r.numOfLeaf, Value: n.r.h})
	}
	return
}

func (n *node) verify() error {
	if n.level == levelLeaf {
		if n.numOfLeaf != 1 {
			return fmt.Errorf("invalid numOfLeaf, expected: 1, value: %d", n.numOfLeaf)
		}
		return nil
	}
	if n.l != nil {
		if n.l.level < n.r.level {
			return fmt.Errorf("invalid level l:%d r:%d", n.l.level, n.r.level)
		}
		if err := n.l.verify(); err != nil {
			return err
		} else if n.level > levelBranch {
			if v := 1 << (n.l.level - levelLeaf); v != n.l.numOfLeaf {
				return fmt.Errorf("invalid numOfLeaf, expected: %d, value: %d", v, n.l.numOfLeaf)
			}
		}
		if err := n.r.verify(); err != nil {
			return err
		}
	}
	return nil
}

type MerkleBinaryTreeProof struct {
	ProofInLeft  []ProofNode
	Contents     [][]byte
	ProofInRight []ProofNode
	hashFunc     HashFunc
}

type ProofNode struct {
	NumOfLeaf int
	Value     []byte
}

func (m *MerkleBinaryTree) ProofOfAll() *MerkleBinaryTreeProof {
	if m.Len() < 1 {
		return nil
	}
	return &MerkleBinaryTreeProof{
		Contents: m.contents[:],
		hashFunc: m.hashFunc,
	}
}

func (m *MerkleBinaryTree) ProofLength(begin, end int) (int, error) {
	//TODO implement me
	panic("implement me")
}

func (m *MerkleBinaryTree) Proof(begin, end int) (*MerkleBinaryTreeProof, error) {
	if begin < 1 || end < 1 || end > m.Len() {
		return nil, OutOfRange
	}
	if begin > end {
		return nil, fmt.Errorf("begin should be less than end")
	}
	if begin == 1 && end == m.Len() {
		return m.ProofOfAll(), nil
	}
	var l, r []ProofNode
	if begin == 1 {
		if end == m.Len() {
			return m.ProofOfAll(), nil
		} else {
			_, r = m.root.proof(end)
		}
	} else {
		l, r = m.root.proof(begin)
		if begin != end {
			if end == m.Len() {
				r = r[:0]
			} else {
				_, r = m.root.proof(end)
			}
		}
	}
	p := &MerkleBinaryTreeProof{
		Contents:     m.contents[begin-1 : end],
		ProofInLeft:  make([]ProofNode, len(l)),
		ProofInRight: r,
		hashFunc:     m.hashFunc,
	}

	j := len(l) - 1
	for _, n := range l {
		p.ProofInLeft[j].NumOfLeaf = n.NumOfLeaf
		p.ProofInLeft[j].Value = n.Value
		j--
	}
	return p, nil
}

func (p *MerkleBinaryTreeProof) Root() (h []byte, left int, total int, err error) {
	r := &node{hashFunc: p.hashFunc}
	for _, n := range p.ProofInLeft {
		r = r.lazyAdd(n.NumOfLeaf, n.Value)
		left += n.NumOfLeaf
	}
	for _, c := range p.Contents {
		r = r.lazyAdd(1, p.hashFunc(c))
		total++
	}
	for _, n := range p.ProofInRight {
		r = r.lazyAdd(n.NumOfLeaf, n.Value)
		total += n.NumOfLeaf
	}
	r.ensureHash(false)
	h = r.h
	total = left + total
	if r.numOfLeaf != total {
		err = fmt.Errorf("total doesn't match sum: %d, node: %d", total, r.numOfLeaf)
		return
	}
	if err = r.verify(); err != nil {
		return
	}
	return
}

func (p *MerkleBinaryTreeProof) SetHashFunc(hashFunc HashFunc) {
	p.hashFunc = hashFunc
}

func (p *MerkleBinaryTreeProof) String() string {
	s := "MerkleBinaryTreeProof{"
	for i, pn := range p.ProofInLeft {
		s += fmt.Sprintf("ProofInLeft[%d]:%s\n", i, pn.String())
	}
	for i, c := range p.Contents {
		s += fmt.Sprintf("Contents[%d]:%s\n", i, hex.EncodeToString(c))
	}
	for i, pn := range p.ProofInRight {
		s += fmt.Sprintf("ProofInRight[%d]:%s\n", i, pn.String())
	}
	s += "}"
	return s
}

func (pn ProofNode) String() string {
	return fmt.Sprintf("{NumOfLeaf:%d,Value:%s}",
		pn.NumOfLeaf, hex.EncodeToString(pn.Value))
}
