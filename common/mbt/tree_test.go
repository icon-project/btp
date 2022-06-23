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
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

var (
	data = []string{
		"dog",
		"cat",
		"elephant",
		"bird",
		"monkey",
		"lion",
		"tiger",
		"zebra",
	}
	contents = stringListToBytesList(data)
)

func stringListToBytesList(sl []string) [][]byte {
	bl := make([][]byte, len(sl))
	for i, s := range sl {
		bl[i] = []byte(s)
	}
	return bl
}

func TestMerkleBinaryTree_Basic(t *testing.T) {
	const lenOfTree = 7
	var err error
	m := &MerkleBinaryTree{hashFunc: Sha3FIPS256}
	for i, c := range contents[:lenOfTree] {
		err = m.Add(c)
		assert.NoError(t, err)
		for j := 0; j <= i; j++ {
			c, err = m.Get(j + 1)
			assert.NoError(t, err)
			assert.Equal(t, data[j], string(c))
		}
	}
	_, err = m.Get(0)
	assert.Error(t, OutOfRange, err)
	_, err = m.Get(m.Len() + 1)
	assert.Error(t, OutOfRange, err)
	fmt.Println(m.String())

	var tm *MerkleBinaryTree
	tm, err = NewMerkleBinaryTree(Sha3FIPS256, contents[:lenOfTree])
	assert.NoError(t, err)
	assertEqualMerkleBinaryTree(t, m, tm)
}

func assertEqualMerkleBinaryTree(t *testing.T, m1, m2 *MerkleBinaryTree) {
	assert.Equal(t, m1.Len(), m2.Len())
	assert.Equal(t, m1.Root(), m2.Root())
	assert.Equal(t, m1.contents, m2.contents)
	l1 := make([]*node, 0)
	l1 = flat(m1.root, l1)
	l2 := make([]*node, 0)
	l2 = flat(m2.root, l2)
	assert.Equal(t, len(l1), len(l2))
	for i, n1 := range l1 {
		n2 := l2[i]
		assert.Equal(t, n1.level, n2.level)
		assert.Equal(t, n1.h, n2.h)
		assert.Equal(t, n1.numOfLeaf, n2.numOfLeaf)
	}
}

func flat(n *node, l []*node) []*node {
	l = append(l, n)
	if n.l != nil {
		l = flat(n.l, l)
	}
	if n.r != nil {
		l = flat(n.r, l)
	}
	return l
}
