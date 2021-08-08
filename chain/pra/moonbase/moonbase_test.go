package moonbase

import (
	"fmt"
	"reflect"
	"testing"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	types "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMoonriverEventRecord(t *testing.T) {
	t.Skip("Manual only")
	api, err := gsrpc.NewSubstrateAPI("wss://wss.testnet.moonbeam.network")
	require.NoError(t, err)

	type test struct {
		blockNumber      int
		moduleEventNames []string
	}

	// TODO add all event on chain
	tests := []test{
		{blockNumber: 243387, moduleEventNames: []string{
			"Democracy_Proposed",
		}},
		{blockNumber: 243336, moduleEventNames: []string{
			"Democracy_PreimageNoted",
		}},
		{blockNumber: 223200, moduleEventNames: []string{
			"Democracy_Tabled",
			"Democracy_Started",
			"Democracy_PreimageUsed",
			"Democracy_Executed",
		}},
		{blockNumber: 243433, moduleEventNames: []string{
			"Balances_Transfer",
		}},
		{blockNumber: 243298, moduleEventNames: []string{
			"Balances_Endowed",
		}},
		{blockNumber: 233385, moduleEventNames: []string{
			"AuthorMapping_AuthorRotated",
			"Treasury_Deposit",
		}},
		{blockNumber: 238828, moduleEventNames: []string{
			"AuthorFilter_EligibleUpdated",
			"Sudo_Sudid",
			"EVM_Log",
		}},
		{blockNumber: 231000, moduleEventNames: []string{
			"Balances_Transfer",
			"Ethereum_Executed",
			"ParachainStaking_ReservedForParachainBond",
			"ParachainStaking_Rewarded",
			"ParachainStaking_CollatorChosen",
			"ParachainStaking_NewRound",
			"System_ExtrinsicSuccess",
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
			eventRecords := MoonbaseEventRecord{}
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
