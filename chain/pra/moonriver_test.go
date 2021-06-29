package pra

import (
	"testing"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	types "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
)

func TestMoonriverEventRecord(t *testing.T) {
	api, err := gsrpc.NewSubstrateAPI("wss://wss.testnet.moonbeam.network")
	assert.NoError(t, err)

	key := types.NewStorageKey(types.MustHexDecodeString("0x26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))

	meta, err := api.RPC.State.GetMetadataLatest()
	assert.NoError(t, err)

	for i := 231000; i < 231010; i++ {
		hash, err := api.RPC.Chain.GetBlockHash(uint64(i))
		assert.NoError(t, err)

		storageRaw, err := api.RPC.State.GetStorageRaw(key, hash)
		assert.NoError(t, err)

		eventRecords := MoonriverEventRecord{}
		assert.NotPanicsf(t, func() {
			err = types.EventRecordsRaw(*storageRaw).DecodeEventRecords(meta, &eventRecords)
			assert.NoErrorf(t, err, "failed at %s", hash.Hex())
		}, "blockhash: %s", hash.Hex())
	}
}
