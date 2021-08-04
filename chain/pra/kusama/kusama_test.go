package kusama

import (
	"fmt"
	"reflect"
	"testing"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestKusamaEventRecord(t *testing.T) {
	api, err := gsrpc.NewSubstrateAPI("wss://kusama-rpc.polkadot.io")
	require.NoError(t, err)

	type test struct {
		blockNumber      int
		moduleEventNames []string
	}

	// TODO add all event on chain
	tests := []test{
		{blockNumber: 8637954, moduleEventNames: []string{
			"ParasInclusion_CandidateIncluded",
		}},
	}

	meta, err := api.RPC.State.GetMetadataLatest()
	require.NoError(t, err)

	for _, test := range tests {
		i := test.blockNumber

		hash, err := api.RPC.Chain.GetBlockHash(uint64(i))
		require.NoError(t, err)

		key, err := types.CreateStorageKey(meta, "System", "Events", nil, nil)
		require.NoError(t, err)

		storageRaw, err := api.RPC.State.GetStorageRaw(key, hash)
		require.NoError(t, err)

		assert.NotPanics(t, func() {
			eventRecords := KusamaEventRecord{}
			err = types.EventRecordsRaw(*storageRaw).DecodeEventRecords(meta, &eventRecords)
			assert.NoErrorf(t, err, "failed at %s", hash.Hex())

			values := reflect.ValueOf(eventRecords)
			for _, me := range test.moduleEventNames {
				t.Run(fmt.Sprintf("should contains %s event", me), func(t *testing.T) {
					assert.Greater(t, values.FieldByName(me).Len(), 0)
				})
			}
		})
	}
}
