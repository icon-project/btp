package pra

// import (
// 	"testing"

// 	"github.com/icon-project/btp/chain/pra/substrate"
// 	"github.com/icon-project/btp/common/log"
// 	"github.com/stretchr/testify/assert"
// 	"github.com/stretchr/testify/require"
// )

// func TestRelayReceiver(t *testing.T) {
// 	t.Run("should build newVotes", func(t *testing.T) {
// 		t.Skip("Manual testing only")

// 		substrateClient, err := substrate.NewSubstrateClient("wss://kusama-rpc.polkadot.io")
// 		require.NoError(t, err)

// 		r := relayReceiver{
// 			c:   substrateClient,
// 			log: log.New(),
// 		}

// 		gj, _, err := r.c.GetJustificationsAndUnknownHeaders(8007753)
// 		require.NoError(t, err)
// 		require.NotNil(t, gj)

// 		v, err := r.newVotes(gj, substrate.SetId(3000))
// 		assert.NoError(t, err)
// 		assert.NotNil(t, v)
// 	})

// 	t.Run("should 1 blockheader when call pullBlockheader at UnknownHeaders nil", func(t *testing.T) {
// 		t.Skip("Manual testing only")

// 		substrateClient, err := substrate.NewSubstrateClient("https://rpc-relay.testnet.moonbeam.network")
// 		require.NoError(t, err)

// 		r := relayReceiver{
// 			c:   substrateClient,
// 			log: log.New(),
// 		}

// 		gj, hds, err := r.c.GetJustificationsAndUnknownHeaders(1091908)
// 		require.NoError(t, err)
// 		require.NotNil(t, gj)
// 		require.Len(t, hds, 0)

// 		fullhds, err := r.pullBlockHeaders(gj, hds)
// 		assert.NotNil(t, fullhds)
// 		assert.Len(t, fullhds, 1)
// 	})

// 	t.Run("should N+1 blockheader when call pullBlockheader at UnknownHeaders N", func(t *testing.T) {
// 		t.Skip("Manual testing only")

// 		substrateClient, err := substrate.NewSubstrateClient("https://rpc-relay.testnet.moonbeam.network")
// 		require.NoError(t, err)

// 		r := relayReceiver{
// 			c:   substrateClient,
// 			log: log.New(),
// 		}

// 		gj, hds, err := r.c.GetJustificationsAndUnknownHeaders(1091000)
// 		require.NoError(t, err)
// 		require.NotNil(t, gj)
// 		require.Equal(t, 908, len(hds))

// 		fullhds, err := r.pullBlockHeaders(gj, hds)
// 		assert.NoError(t, err)
// 		assert.NotNil(t, fullhds)
// 		assert.Equal(t, 909, len(fullhds))
// 		assert.EqualValues(t, 1091908, fullhds[len(fullhds)-1].Number)
// 	})
// }
