package mbt

import (
	"encoding/hex"
	"fmt"
)

func (n *node) path(num int) []PathNode {
	if n.level < levelBranch {
		return make([]PathNode, 0)
	}
	maxNumChild := 1 << (n.level - levelBranch)
	var l []PathNode
	if num > maxNumChild {
		l = n.r.path(num - maxNumChild)
		l = append(l, PathNode{Right: false, Value: n.l.h})
	} else {
		l = n.l.path(num)
		l = append(l, PathNode{Right: true, Value: n.r.h})
	}
	return l
}

type MerkleBinaryTreePath struct {
	Content  []byte
	Path     []PathNode
	hashFunc HashFunc
}

type PathNode struct {
	Right bool
	Value []byte
}

func (m *MerkleBinaryTree) Path(num int) (*MerkleBinaryTreePath, error) {
	if num < 1 || num > m.Len() {
		return nil, OutOfRange
	}
	p := &MerkleBinaryTreePath{hashFunc: m.hashFunc}
	p.Content = m.contents[num-1]
	p.Path = m.root.path(num)
	return p, nil
}

func (p *MerkleBinaryTreePath) Root() []byte {
	h := p.hashFunc(p.Content)
	for _, n := range p.Path {
		if n.Right {
			h = p.hashFunc(h, n.Value)
		} else {
			h = p.hashFunc(n.Value, h)
		}
	}
	return h
}

func (p *MerkleBinaryTreePath) SetHashFunc(hashFunc HashFunc) {
	p.hashFunc = hashFunc
}

func (p *MerkleBinaryTreePath) String() string {
	s := "MerkleBinaryTreePath{"
	s += fmt.Sprintf("Content:%s\n", hex.EncodeToString(p.Content))
	for i, pn := range p.Path {
		s += fmt.Sprintf("Path[%d]:%s\n", i, pn.String())
	}
	s += "}"
	return s
}

func (pn PathNode) String() string {
	return fmt.Sprintf("{Right:%v,Value:%s}",
		pn.Right, hex.EncodeToString(pn.Value))
}

func RootByMerkleNode(hashFunc HashFunc, leaf []byte, nList []MerkleNode) []byte {
	h := leaf
	for _, n := range nList {
		if n.Dir == DirRight {
			h = hashFunc(h, n.Value)
		} else {
			h = hashFunc(n.Value, h)
		}
	}
	return h
}

type Dir int

const (
	DirLeft = Dir(iota)
	DirRight
)

type MerkleNode struct {
	Dir   Dir
	Value []byte
}
