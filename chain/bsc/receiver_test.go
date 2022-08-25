package bsc

import (
	"fmt"
	"math/big"
	"testing"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

func TestReceiver_GetReceiptProofs(t *testing.T) {
	var src, dst chain.BtpAddress
	err := src.Set("btp://0x97.icon/0xAaFc8EeaEE8d9C8bD3262CCE3D73E56DeE3FB776")
	err = dst.Set("btp://0xf8aac3.icon/cxea19a7d6e9a926767d1d05eea467299fe461c0eb")
	if err != nil {
		fmt.Println(err)
	}

	r := NewReceiver(src, dst, "http://localhost:8545", nil, log.New())

	blockNotification := &BlockNotification{Height: big.NewInt(191)}
	receiptProofs, err := r.(*receiver).newReceiptProofs(blockNotification)

	//fmt.Println(receiptProofs[0].Proof)

	var bytes [][]byte
	_, err = codec.RLP.UnmarshalFromBytes(receiptProofs[0].Proof, &bytes)

	if err != nil {
		return
	}

	block, err := r.(*receiver).c.GetBlockByHeight(big.NewInt(191))
	fmt.Println(block.ReceiptHash())
	//fmt.Println(block.Hash())
	//fmt.Println(receiptProofs[0])
	//fmt.Println(len(bytes))
	//for _, proof := range bytes {
	//	fmt.Println(hexutil.Encode(proof))
	//}
}
