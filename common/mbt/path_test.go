package mbt

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMerkleBinaryTreePath_Basic(t *testing.T) {
	const lenOfTree = 7
	m, err := NewMerkleBinaryTree(Sha3FIPS256, contents[:lenOfTree])
	assert.NoError(t, err)

	var mPath *MerkleBinaryTreePath
	num := 4
	mPath, err = m.Path(num)
	assert.NoError(t, err)
	assert.Equal(t, data[num-1], string(mPath.Content))
	assert.Equal(t, m.Root(), mPath.Root())
	fmt.Println(mPath.String())
}
