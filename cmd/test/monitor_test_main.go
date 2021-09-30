package main

import (
	"fmt"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

const (
	EventSignature = "Message(str,int,bytes)"
)

func monitor(client *icon.Client) {
	ef := &icon.EventFilter{
		Addr:      icon.Address("cxe284d1774ade175c273b52742c3763ec5fe43844"),
		Signature: EventSignature,
		//Indexed:   []*string{&s},
	}
	resp := &icon.BlockNotification{}
	p := &icon.BlockRequest{
		Height:       icon.HexInt("0xB0052"),
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
	client := icon.NewClient("wss://btp.net.solidwallet.io/api/v3", log.New())
	p := &icon.BlockHeightParam{
		Height: icon.HexInt("0xB0053"),
	}

	b, _ := client.GetBlockHeaderByHeight(p)
	var bh icon.BlockHeader
	_, err := codec.RLP.UnmarshalFromBytes(b, &bh)

	if err != nil {
		fmt.Println(err)
	}

	fmt.Println(bh.Height)
}
