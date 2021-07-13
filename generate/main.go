package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain/pra"
)

type BlockInfo struct {
	BlockNumber            uint64
	Hash                   pra.SubstrateHash
	Header                 pra.SubstrateHeader
	ScaleEncodedHeader     []byte
	MetaData               pra.SubstrateMetaData
	StorageKey             pra.SubstrateStorageKey
	SystemEventsStorageRaw pra.SubstrateStorageDataRaw
	SystemEventsReadProof  pra.ReadProof
}

func main() {
	// api, err := gsrpc.NewSubstrateAPI("wss://icon-btp.ecl.vn:34008")
	api, err := gsrpc.NewSubstrateAPI("wss://wss.testnet.moonbeam.network")
	if err != nil {
		fmt.Println(err)
	}

	blockNumber := uint64(315553)

	hash, err := api.RPC.Chain.GetBlockHash(blockNumber)
	if err != nil {
		fmt.Println(err)
	}

	header, err := api.RPC.Chain.GetHeader(hash)
	if err != nil {
		fmt.Println(err)
	}

	meta, err := api.RPC.State.GetMetadata(hash)
	if err != nil {
		fmt.Println(err)
	}

	key, err := types.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		fmt.Println(err)
	}

	storage, err := api.RPC.State.GetStorageRaw(key, hash)
	if err != nil {
		fmt.Println(err)
	}

	var readProof pra.ReadProof
	err = api.Client.Call(&readProof, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	if err != nil {
		fmt.Println(err)
	}

	seh, err := types.EncodeToBytes(header)
	if err != nil {
		fmt.Println(err)
	}

	bi := BlockInfo{
		BlockNumber:            blockNumber,
		Hash:                   hash,
		Header:                 *header,
		ScaleEncodedHeader:     seh,
		MetaData:               *meta,
		StorageKey:             key,
		SystemEventsStorageRaw: *storage,
		SystemEventsReadProof:  readProof,
	}

	b, err := json.Marshal(bi)
	if err != nil {
		fmt.Println(err)
	}

	err = ioutil.WriteFile("chain/pra/assets/moonbase_blockinfo_315553.json", b, 0644)
	if err != nil {
		fmt.Println(err)
	}
}
