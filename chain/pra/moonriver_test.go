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

	type test struct {
		blockNumber int
	}

	tests := []test{
		{blockNumber: 231000},
	}

	for _, test := range tests {
		i := test.blockNumber

		hash, err := api.RPC.Chain.GetBlockHash(uint64(i))
		assert.NoError(t, err)

		meta, err := api.RPC.State.GetMetadata(hash)
		assert.NoError(t, err)

		key, err := types.CreateStorageKey(meta, "System", "Events", nil, nil)
		assert.NoError(t, err)

		storageRaw, err := api.RPC.State.GetStorageRaw(key, hash)
		assert.NoError(t, err)

		eventRecords := MoonriverEventRecord{}
		err = types.EventRecordsRaw(*storageRaw).DecodeEventRecords(meta, &eventRecords)
		assert.NoErrorf(t, err, "failed at %s", hash.Hex())
		t.Log(eventRecords.Len())
	}
}
