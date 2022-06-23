/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mbt

import (
	"encoding/hex"
	"fmt"

	"golang.org/x/crypto/sha3"
)

func Sha3FIPS256(l ...[]byte) []byte {
	h := sha3.New256()
	for _, b := range l {
		h.Write(b)
	}
	var digest [32]byte
	h.Sum(digest[:0])
	return digest[:]
}

func Sha3Keccak256(data ...[]byte) []byte {
	h := sha3.NewLegacyKeccak256()
	for _, b := range data {
		h.Write(b)
	}
	var digest [32]byte
	h.Sum(digest[:0])
	return digest[:]
}

type HashFunc func(l ...[]byte) []byte

var (
	uidHashFuncs = map[string]HashFunc{
		"icon": Sha3FIPS256,
		"eth":  Sha3Keccak256,
	}
)

func HashFuncByUID(uid string) HashFunc {
	return uidHashFuncs[uid]
}

const (
	levelInit = iota
	levelLeaf
	levelBranch
)

var (
	InvalidContent = fmt.Errorf("content should not be null")
	OutOfRange     = fmt.Errorf("out of range")
)

type node struct {
	l         *node
	r         *node
	h         []byte
	level     int
	numOfLeaf int
	hashFunc  HashFunc
}

func (n *node) add(h []byte) *node {
	if n.level == levelInit {
		n.level = levelLeaf
		n.h = h
		n.numOfLeaf = 1
	} else {
		if n.l != nil && n.l.level != n.r.level {
			n.r = n.r.add(h)
			n.numOfLeaf++
			n.h = n.hashFunc(n.l.h, n.r.h)
		} else {
			r := &node{
				l: n,
				r: &node{
					h:         h,
					level:     levelLeaf,
					numOfLeaf: 1,
					hashFunc:  n.hashFunc,
				},
				level:    n.level + 1,
				hashFunc: n.hashFunc,
			}
			r.numOfLeaf = n.numOfLeaf + 1
			r.h = n.hashFunc(n.h, h)
			return r
		}
	}
	return n
}

func (n *node) lazyAdd(numOfLeaf int, h []byte) *node {
	if n.level == levelInit {
		n.level = NumberToLevel(numOfLeaf)
		n.numOfLeaf = numOfLeaf
		n.h = h
	} else {
		if n.l != nil && n.l.numOfLeaf != n.r.numOfLeaf {
			n.r = n.r.lazyAdd(numOfLeaf, h)
			n.numOfLeaf += numOfLeaf
		} else {
			r := &node{
				l: n,
				r: &node{
					h:         h,
					level:     NumberToLevel(numOfLeaf),
					numOfLeaf: numOfLeaf,
					hashFunc:  n.hashFunc,
				},
				level:    n.level + 1,
				hashFunc: n.hashFunc,
			}
			r.numOfLeaf = n.numOfLeaf + numOfLeaf
			return r
		}
	}
	return n
}

func (n *node) ensureHash(force bool) {
	if n.level < levelBranch {
		return
	}
	if force || len(n.h) == 0 {
		if n.l != nil {
			n.l.ensureHash(force)
		}
		if n.r != nil {
			n.r.ensureHash(force)
		}
		n.h = n.hashFunc(n.l.h, n.r.h)
	}
}

func (n *node) String() string {
	s := fmt.Sprintf("%p [%d] h:%s l: %p, r: %p num: %d\n",
		n, n.level, hex.EncodeToString(n.h), n.l, n.r, n.numOfLeaf)
	if n.l != nil {
		s += n.l.String()
	}
	if n.r != nil {
		s += n.r.String()
	}
	return s
}

type MerkleBinaryTree struct {
	root     *node
	contents [][]byte //nullable
	hashFunc func(l ...[]byte) []byte
}

func (m *MerkleBinaryTree) Add(content []byte) error {
	if content == nil {
		return InvalidContent
	}
	if m.root == nil {
		m.root = &node{hashFunc: m.hashFunc}
	}
	m.root = m.root.add(m.hashFunc(content))
	m.contents = append(m.contents, content)
	return nil
}

func (m *MerkleBinaryTree) Len() int {
	return len(m.contents)
}

func (m *MerkleBinaryTree) Root() []byte {
	if m.root == nil {
		return nil
	}
	return m.root.h
}

func (m *MerkleBinaryTree) Get(num int) ([]byte, error) {
	if num < 1 || num > m.Len() {
		return nil, OutOfRange
	}
	return m.contents[num-1], nil
}

func (m *MerkleBinaryTree) String() string {
	if m.root == nil {
		return "MerkleBinaryTree{Len:0, Nodes: nil}"
	}
	return fmt.Sprintf("MerkleBinaryTree{Len:%d, Nodes:\n%s}", m.Len(), m.root.String())
}

func NewMerkleBinaryTree(hashFunc HashFunc, contents [][]byte) (*MerkleBinaryTree, error) {
	if hashFunc == nil {
		return nil, fmt.Errorf("hashFunc cannot be nil")
	}
	m := &MerkleBinaryTree{contents: contents, hashFunc: hashFunc}
	if len(contents) > 0 {
		m.root = &node{hashFunc: hashFunc}
		for _, c := range contents {
			if c == nil {
				return nil, InvalidContent
			}
			m.root = m.root.lazyAdd(1, hashFunc(c))
		}
		m.root.ensureHash(false)
	} else {
		m.contents = make([][]byte, 0)
	}
	return m, nil
}
