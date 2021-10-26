package main

import (
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain/pra/substrate"
)

type blockInfo struct {
	BlockNumber            uint64
	Hash                   substrate.SubstrateHash
	Header                 *substrate.SubstrateHeader
	ScaleEncodedHeader     []byte
	StorageKey             substrate.SubstrateStorageKey
	SystemEventsStorageRaw *substrate.SubstrateStorageDataRaw
	SystemEventsReadProof  substrate.SubstrateReadProof
}

func main() {
	cl, _ := substrate.NewSubstrateClient("wss://wss.testnet.moonbeam.network")
	cl.Init()

	bi := blockInfo{
		BlockNumber: 814054,
	}

	var err error

	bi.Hash, _ = cl.GetBlockHash(814054)
	bi.Header, _ = cl.GetHeader(bi.Hash)
	bi.ScaleEncodedHeader, _ = substrate.NewEncodedSubstrateHeader(*bi.Header)
	bi.StorageKey, err = cl.GetSystemEventStorageKey(bi.Hash)

	fmt.Println(err)

	bi.SystemEventsStorageRaw, _ = cl.GetStorageRaw(bi.StorageKey, bi.Hash)
	bi.SystemEventsReadProof, _ = cl.GetReadProof(bi.StorageKey, bi.Hash)

	b, _ := json.Marshal(bi)
	fmt.Println(string(b))
}
