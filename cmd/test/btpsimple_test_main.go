package main

import (
	"fmt"
	"github.com/ethereum/go-ethereum/accounts/keystore"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/rlp"
	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/cmd/btpsimple/module/bsc"
	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"io/ioutil"
	"math/big"
	"strings"
)

func test_SenderReceiver() {
	var wallet bsc.Wallet
	var src, dst module.BtpAddress

	err := src.Set("btp://0x516d3.icon/cx7797ca6474db6839b0ce6f2b0daa68dd737602c6")
	if err != nil {
		fmt.Println(err)
	}
	err = dst.Set("btp://0xa261be.icon/0xa813e24340141101a417B599Db481823C523e788")
	if err != nil {
		fmt.Println(err)
	}

	receiver := icon.NewReceiver(src, dst, "http://localhost:9080/api/v3/src", nil, log.GlobalLogger())
	sender := bsc.NewSender(src, dst, wallet, "ws://localhost:8545/", nil, log.GlobalLogger())

	bu, err := receiver.GetBlockUpdate(112)
	//fmt.Println(bu, err)
	rm := &module.RelayMessage{
		From:         src,
		BlockUpdates: make([]*module.BlockUpdate, 0),
		Seq:          1,
	}

	rm.BlockUpdates = append(rm.BlockUpdates, bu)
	//fmt.Println(rm.BlockUpdates[0])
	segments, err := sender.Segment(rm, 111)
	if err != nil {
		fmt.Println(err)
	}

	//fmt.Println(seg[0])
	for j, segment := range segments {
		relay, err := sender.Relay(segment)
		if err != nil {
			fmt.Println(err)
		}
		fmt.Println(j, relay)
	}

}

func test_wallet() {
	keyjson, err := ioutil.ReadFile("/Users/mo/web3labs/icon/btp/devnet/docker/icon-bsc/work/bsc.bsc.ks.json")
	if err != nil {
		fmt.Println(err)
	}
	//fmt.Println(keyjson)
	key, err := keystore.DecryptKey(keyjson, "Perlia0")
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println(key)

	keys, err := wallet.DecryptEvmKeyStore(keyjson, []byte("Perlia0"))
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println(keys)
}

func main() {
	//var header types.Header
	fmt.Println(strings.ToLower("btp://0x97.bsc/0xAaFc8EeaEE8d9C8bD3262CCE3D73E56DeE3FB776"))
	var bytes []byte

	header := types.Header{
		Number: big.NewInt(300),
	}

	bytes, err := codec.RLP.MarshalToBytes(header)
	if err != nil {
		fmt.Println("Error: ", err)
	}
	fmt.Println(bytes)
	fmt.Println(header.Number)

	var tmp types.Header
	_, err = codec.RLP.UnmarshalFromBytes(bytes, &tmp)
	if err != nil {
		fmt.Println("Error: ", err)
	}
	fmt.Println(tmp.Number)
}

func main1() {
	var block types.Header
	var bytes []byte

	block.Number = big.NewInt(600)
	bytes, err := rlp.EncodeToBytes(&block)
	if err != nil {
		fmt.Println("Error: ", err)
	}
	//fmt.Println(bytes)
	fmt.Println(hexutil.Encode(bytes))
	var tmp types.Header
	err = rlp.DecodeBytes(bytes, &tmp)
	if err != nil {
		fmt.Println("Error: ", err)
	}
	fmt.Println(tmp.Number)

}
