package main

import (
	"fmt"
	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/cmd/btpsimple/module/bsc"
	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/log"
)

func main() {
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
