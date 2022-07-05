package mbt

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/icon-project/btp/common/codec"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestMerkleBinaryTreeProof_Basic(t *testing.T) {
	const lenOfTree = 7
	m, err := NewMerkleBinaryTree(Sha3FIPS256, contents[:lenOfTree])
	assert.NoError(t, err)

	begin, end := 2, 5
	var mProof *MerkleBinaryTreeProof
	mProof, err = m.Proof(begin, end)

	leftTest, err := codec.RLP.MarshalToBytes(mProof.ProofInLeft)
	rightTest, err := json.Marshal(mProof.ProofInRight)
	//leftTest := unsafe.Sizeof(mProof.ProofInLeft)
	//rightTest := unsafe.Sizeof(mProof.ProofInRight)

	fmt.Printf("l %d. r %d", leftTest, rightTest)

	assert.NoError(t, err)
	for i, j := begin-1, 0; i < end; i, j = i+1, j+1 {
		assert.Equal(t, data[i], string(mProof.Contents[j]))
	}
	var r []byte
	var left, total int
	r, left, total, err = mProof.Root()
	assert.NoError(t, err)
	assert.Equal(t, begin-1, left)
	assert.Equal(t, m.Len(), total)
	assert.Equal(t, m.Root(), r)
	fmt.Println(mProof.String())
}

func TestMerkleBinaryTreeProof_FakeProofInLeft(t *testing.T) {
	const lenOfTree = 8
	m, err := NewMerkleBinaryTree(Sha3FIPS256, contents[:lenOfTree])
	assert.NoError(t, err)
	begin, end := 7, 8
	var validProof *MerkleBinaryTreeProof
	validProof, err = m.Proof(begin, end)
	assert.NoError(t, err)

	var r, fr []byte
	var left, total, fleft, ftotal int
	r, left, total, err = validProof.Root()
	assert.NoError(t, err)
	assert.Equal(t, begin-1, left)
	assert.Equal(t, m.Len(), total)
	assert.Equal(t, m.Root(), r)
	fmt.Println("validProof:", hex.EncodeToString(r), left, total)

	fakeLeaf := merkleNode(m.hashFunc, contents[4:6])
	fakeLeaf2 := merkleNode(m.hashFunc, contents[6:8])
	fakeProof := &MerkleBinaryTreeProof{
		ProofInLeft: []ProofNode{
			{
				NumOfLeaf: 6,
				Value:     merkleNode(m.hashFunc, contents[0:4]).h,
			},
		},
		Contents: [][]byte{
			append(fakeLeaf.l.h, fakeLeaf.r.h...),
			append(fakeLeaf2.l.h, fakeLeaf2.r.h...),
		},
		hashFunc: m.hashFunc,
	}
	fr, fleft, ftotal, err = fakeProof.Root()
	assert.Equal(t, r, fr)
	assert.Equal(t, left, fleft)
	assert.Equal(t, total, ftotal)
	fmt.Println("fakeProof:", hex.EncodeToString(fr), fleft, ftotal)
	assert.Error(t, err)
	fmt.Println("fakeProof error:", err)
}

func TestMerkleBinaryTreeProof_FakeProofInRight(t *testing.T) {
	const lenOfTree = 6
	m, err := NewMerkleBinaryTree(Sha3FIPS256, contents[:lenOfTree])
	assert.NoError(t, err)
	begin, end := 1, 1
	var validProof *MerkleBinaryTreeProof
	validProof, err = m.Proof(begin, end)
	assert.NoError(t, err)

	var r, fr []byte
	var left, total, fleft, ftotal int
	r, left, total, err = validProof.Root()
	assert.NoError(t, err)
	assert.Equal(t, begin-1, left)
	assert.Equal(t, m.Len(), total)
	assert.Equal(t, m.Root(), r)
	fmt.Println("validProof:", hex.EncodeToString(r), left, total)

	fakeLeaf := merkleNode(m.hashFunc, contents[0:2])
	fakeProof := &MerkleBinaryTreeProof{
		Contents: [][]byte{
			append(fakeLeaf.l.h, fakeLeaf.r.h...),
		},
		ProofInRight: []ProofNode{
			{
				NumOfLeaf: 1,
				Value:     merkleNode(m.hashFunc, contents[2:4]).h,
			},
			{
				NumOfLeaf: 4,
				Value:     merkleNode(m.hashFunc, contents[4:6]).h,
			},
		},
		hashFunc: m.hashFunc,
	}
	fr, fleft, ftotal, err = fakeProof.Root()
	assert.Equal(t, r, fr)
	assert.Equal(t, left, fleft)
	assert.Equal(t, total, ftotal)
	fmt.Println("fakeProof:", hex.EncodeToString(fr), fleft, ftotal)
	assert.Error(t, err)
	fmt.Println("fakeProof error:", err)
}

func merkleNode(hashFunc HashFunc, contents [][]byte) *node {
	n := &node{hashFunc: hashFunc}
	for _, c := range contents {
		if c == nil {
			panic(InvalidContent)
		}
		n = n.lazyAdd(1, hashFunc(c))
	}
	n.ensureHash(false)
	return n
}
