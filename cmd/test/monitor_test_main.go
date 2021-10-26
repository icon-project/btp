package main

import (
	"fmt"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/log"
)

const (
	EventSignature                      = "Message(str,int,bytes)"
	HandleRelayMessageStarted           = "HandleRelayMessageStarted(int,str)"
	HandleRelayMessageEnded             = "HandleRelayMessageEnded(int,str)"
	BMCAccessed                         = "BMCAccessed(int,str)"
	RelayMessageExtracted               = "RelayMessageExtracted(int,str)"
	LastReceiptRootHashReceived         = "LastReceiptRootHashReceived(str,int,str)"
	ReceiptProofValidated               = "ReceiptProofValidated(int,str)"
	ReceiptEventLogsValidated           = "ReceiptEventLogsValidated(int,str)"
	BlockUpdateHeightValidatingStarted  = "BlockUpdateHeightValidatingStarted(int,int)"
	BlockUpdateHeightValidatingFinished = "BlockUpdateHeightValidatingFinished(int,int)"
)

func monitor(client *icon.Client) {
	ef := &icon.EventFilter{
		Addr:      icon.Address("cx11681792ca3ebd860fde1dc7518470d26f37ae71"),
		Signature: LastReceiptRootHashReceived,
		//Indexed:   []*string{&s},
	}
	resp := &icon.BlockNotification{}
	p := &icon.BlockRequest{
		Height:       icon.HexInt("0x0"),
		EventFilters: []*icon.EventFilter{ef},
	}
	err := client.Monitor("/block", p, resp, func(conn *websocket.Conn, v interface{}) {
		switch t := v.(type) {
		case *icon.BlockNotification:
			h, _ := t.Height.Int()
			if len(t.Indexes) > 0 || len(t.Events) > 0 {
				hash, _ := t.Hash.Value()
				fmt.Println(h, t.Indexes, t.Events, hexutil.Encode(hash))
			}
		}
	})
	if err != nil {
		fmt.Println(err)
	}
}

func test_monitor() {
	client := icon.NewClient("http://localhost:9080/api/v3/icon", log.New())

	fmt.Println("Starting...")

	monitor(client)
	//
	//p := &icon.BlockHeightParam{
	//	Height: icon.HexInt("0xB0053"),
	//}
	//
	//b, _ := client.GetBlockHeaderByHeight(p)
	//var bh icon.BlockHeader
	//_, err := codec.RLP.UnmarshalFromBytes(b, &bh)
	//
	//if err != nil {
	//	fmt.Println("Error: ", err)
	//}
	//
	//fmt.Println("BH height:", bh.Height)
}
