package substrate

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestClient(t *testing.T) {
	t.Run("should return ValidationData", func(t *testing.T) {
		t.Skip("Manual run only")
		c, err := NewSubstrateClient("wss://wss.testnet.moonbeam.network")
		require.NoError(t, err)

		/**
		{
			validationData: {
			parentHead: 0xc3e2e26d7f56e8c0ae64e98d2cdb4376c509bf44a3f817e811167051a6c60eec4258170064e4934621abe9757b4bdcb30df44956df385633c4416e2a84355c2b59eecd9a20c2fc21304546193eaaf5c7473b45e23c58153f9f3977984366afb7d3053bfe0c046e6d627380c8a8aa2e9668d65435cc51d00685add26c6a303f3261a841f67972e54a18b70c0466726f6e090201b0a7e8f200bd7ccd579d4bc0f8f8618815d978ee70cc8bd723b593f4aafd656a0c59df4aa35ba633f91ead78caa8af4b1914b00017bac11675c37722972def0d88ad33a2fc871c6c055302bfdf7109415307eb463b4fec0f0d6772a8d7e4ad5db85aaeae4e4d879c836031b220847b01cf018f3a439d02490935302c92c624b7c1056e6d6273010116efc94625d3c4f16405f8c29dd0d6c0e629e04c3bd4a8984b5492d822a6244024a73faff07d28ae87bfcf47656887f68787e04b0778f5c587af8968e905d18a,
			relayParentNumber: 816,424,
			relayParentStorageRoot: 0x5f8a015c72d729af9680153a14c3f03994e29184fff2eb1f3db991da611a0a82,
			maxPovSize: 52,428,800
			},
		}
		**/
		c.GetValidationData(NewSubstrateHashFromHexString("0xfd7a2619ce12f375cd2c170090ea5f206ab69068affba91c75eda76925e13284"))
	})

	t.Run("should return decode FinalityProof on polkadot", func(t *testing.T) {
		t.Skip("Manual run only")
		c, err := NewSubstrateClient("wss://rpc.polkadot.io")
		require.NoError(t, err)

		gj, hds, err := c.GetJustificationsAndUnknownHeaders(6247439)
		assert.NoError(t, err)
		assert.NotEmpty(t, hds)
		assert.NotNil(t, gj)
	})

	t.Run("should return decode FinalityProof on kusama", func(t *testing.T) {
		t.Skip("Manual run only")
		c, err := NewSubstrateClient("wss://kusama-rpc.polkadot.io")
		require.NoError(t, err)

		gj, hds, err := c.GetJustificationsAndUnknownHeaders(8007753)
		assert.NoError(t, err)
		assert.NotEmpty(t, hds)
		assert.NotNil(t, gj)
	})

	t.Run("should return decode FinalityProof on westend", func(t *testing.T) {
		t.Skip("Manual run only")
		c, err := NewSubstrateClient("wss://westend-rpc.polkadot.io")
		require.NoError(t, err)

		gj, hds, err := c.GetJustificationsAndUnknownHeaders(6788687)
		assert.NoError(t, err)
		assert.NotEmpty(t, hds)
		assert.NotNil(t, gj)
	})

	t.Run("should return decode FinalityProof on moonbase relay chain", func(t *testing.T) {
		t.Skip("Manual run only")
		// c, err := NewSubstrateClient("wss://wss-relay.testnet.moonbeam.network/")
		c, err := NewSubstrateClient("https://rpc-relay.testnet.moonbeam.network/")
		require.NoError(t, err)

		gj, hds, err := c.GetJustificationsAndUnknownHeaders(1016190)
		assert.NoError(t, err)
		assert.NotEmpty(t, hds)
		assert.NotNil(t, gj)
	})

	t.Run("should return multiple block headers", func(t *testing.T) {
		t.Skip("Manual run only")
		// c, err := NewSubstrateClient("wss://wss-relay.testnet.moonbeam.network/")
		c, err := NewSubstrateClient("https://rpc-relay.testnet.moonbeam.network/")
		require.NoError(t, err)

		bh, err := c.GetBlockHeaderByBlockNumbers([]SubstrateBlockNumber{913571, 913572, 913573, 913574, 913575, 913576, 913577})
		assert.NoError(t, err)
		assert.NotNil(t, bh)
	})
}
