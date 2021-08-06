package pra

import (
	"testing"

	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestRelayReceiver(t *testing.T) {
	t.Run("should build newVotes", func(t *testing.T) {
		t.Skip("Manual testing only")

		substrateClient, err := substrate.NewSubstrateClient("wss://kusama-rpc.polkadot.io")
		require.NoError(t, err)

		r := relayReceiver{
			c:   substrateClient,
			log: log.New(),
		}

		gj, _, err := r.c.GetJustificationsAndUnknownHeaders(8007753)
		require.NoError(t, err)
		require.NotNil(t, gj)

		v, err := r.newVotes(gj)
		assert.NoError(t, err)
		assert.NotNil(t, v)
	})
}
