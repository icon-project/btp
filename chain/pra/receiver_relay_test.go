package pra

import (
	"fmt"
	"os"
	"testing"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/codec"
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

		v, err := r.newVotes(gj, substrate.SetId(3000))
		assert.NoError(t, err)
		assert.NotNil(t, v)
	})

	t.Run("should build 1 finalityproof with blockInclude as same as Justification Block", func(t *testing.T) {
		t.Skip("Manual testing only")

		substrateClient, err := substrate.NewSubstrateClient("wss://kusama-rpc.polkadot.io")
		require.NoError(t, err)

		r := relayReceiver{
			c:   substrateClient,
			log: log.New(),
		}

		finalizedHash, err := r.c.GetFinalizedHead()
		require.NoError(t, err)

		finalizeHeader, err := r.c.GetHeader(finalizedHash)
		require.NoError(t, err)

		r.expectMtaHeight = uint64(finalizeHeader.Number - 1)

		path, err := os.Getwd()
		require.NoError(t, err)
		r.bmvStatus.RelayMtaOffset = int64(r.expectMtaHeight - 1)
		r.prepareDatabase(int64(r.expectMtaHeight-1), path, "kusama")

		fps, err := r.buildFinalityProof(finalizeHeader, &finalizedHash)
		assert.NoError(t, err)
		assert.Equal(t, len(fps), 1)
		assert.EqualValues(t, r.store.Height(), 2)
	})

	t.Run("should build 2 finalityproof with candidateInclude smaller than Justification Block", func(t *testing.T) {
		t.Skip("Manual testing only")

		substrateClient, err := substrate.NewSubstrateClient("wss://kusama-rpc.polkadot.io")
		require.NoError(t, err)

		r := relayReceiver{
			c:   substrateClient,
			log: log.New(),
		}

		finalizedHash, err := r.c.GetFinalizedHead()
		require.NoError(t, err)

		finalizeHeader, err := r.c.GetHeader(finalizedHash)
		require.NoError(t, err)

		candidateIncludeHash, err := r.c.GetBlockHash(uint64(finalizeHeader.Number - 3))
		require.NoError(t, err)

		candidateIncludeHeader, err := r.c.GetHeader(candidateIncludeHash)
		require.NoError(t, err)

		r.expectMtaHeight = uint64(finalizeHeader.Number - 5)

		path, err := os.Getwd()
		require.NoError(t, err)
		r.bmvStatus.RelayMtaOffset = int64(r.expectMtaHeight - 1)
		r.prepareDatabase(int64(r.expectMtaHeight-1), path, "kusama")

		fps, err := r.buildFinalityProof(candidateIncludeHeader, &candidateIncludeHash)
		assert.NoError(t, err)
		assert.Equal(t, len(fps), 2)

		var fp1 ParachainFinalityProof
		_, err = codec.RLP.UnmarshalFromBytes(fps[0], &fp1)
		assert.NoError(t, err)

		var lastDecodeHeader substrate.SubstrateHeader
		for i := 0; i < len(fp1.RelayBlockUpdates); i++ {
			var bu RelayBlockUpdate
			_, err = codec.RLP.UnmarshalFromBytes(fp1.RelayBlockUpdates[len(fp1.RelayBlockUpdates)-1], &bu)
			assert.NoError(t, err)
			var bh substrate.SubstrateHeader
			err = types.DecodeFromBytes(bu.ScaleEncodedBlockHeader, &bh)
			assert.NoError(t, err)
			assert.EqualValues(t, r.store.Height(), bh.Number)
			if i != 0 {
				assert.EqualValues(t, lastDecodeHeader.Number+1, bh.Number)
			}
			lastDecodeHeader = bh
		}

		var fp2 ParachainFinalityProof
		_, err = codec.RLP.UnmarshalFromBytes(fps[1], &fp2)
		assert.NoError(t, err)
		var bp chain.BlockProof
		_, err = codec.RLP.UnmarshalFromBytes(fp2.RelayBlockProof, &bp)
		assert.NoError(t, err)
		encodedIncludeHeader, err := types.EncodeToHexString(candidateIncludeHeader)
		assert.NoError(t, err)

		assert.EqualValues(t, lastDecodeHeader.Number, bp.BlockWitness.Height)
		assert.EqualValues(t, encodedIncludeHeader, fmt.Sprintf("0x%x", bp.Header))
	})

	t.Run("should build 3 finalityproof with candidateInclude smaller than grandpanewauthorities smaller than Justification Block", func(t *testing.T) {
		t.Skip("Manual testing only")

		substrateClient, err := substrate.NewSubstrateClient("wss://kusama-rpc.polkadot.io")
		require.NoError(t, err)

		r := relayReceiver{
			c:   substrateClient,
			log: log.New(),
		}

		candidateIncludeHash, err := r.c.GetBlockHash(uint64(8751755))
		require.NoError(t, err)

		candidateIncludeHeader, err := r.c.GetHeader(candidateIncludeHash)
		require.NoError(t, err)

		r.expectMtaHeight = uint64(8751750)
		path, err := os.Getwd()
		require.NoError(t, err)
		r.bmvStatus.RelayMtaOffset = int64(8751749)
		r.prepareDatabase(int64(r.expectMtaHeight-1), path, "kusama")

		fps, err := r.buildFinalityProof(candidateIncludeHeader, &candidateIncludeHash)
		assert.NoError(t, err)
		assert.Equal(t, len(fps), 3)

		var fp1 ParachainFinalityProof
		_, err = codec.RLP.UnmarshalFromBytes(fps[0], &fp1)
		assert.NoError(t, err)
		var lastBuOfFp1 RelayBlockUpdate
		_, err = codec.RLP.UnmarshalFromBytes(fp1.RelayBlockUpdates[len(fp1.RelayBlockUpdates)-1], &lastBuOfFp1)
		assert.NoError(t, err)
		var lastBuOfFp1Header substrate.SubstrateHeader
		err = types.DecodeFromBytes(lastBuOfFp1.ScaleEncodedBlockHeader, &lastBuOfFp1Header)
		assert.NoError(t, err)
		// GrandpaNewAuthorities
		assert.EqualValues(t, 8751752, lastBuOfFp1Header.Number)

		var fp2 ParachainFinalityProof
		_, err = codec.RLP.UnmarshalFromBytes(fps[1], &fp2)
		assert.NoError(t, err)
		var lastBuOfFp2 RelayBlockUpdate
		_, err = codec.RLP.UnmarshalFromBytes(fp1.RelayBlockUpdates[len(fp1.RelayBlockUpdates)-1], &lastBuOfFp2)
		assert.NoError(t, err)
		var lastBuOfFp2Header substrate.SubstrateHeader
		err = types.DecodeFromBytes(lastBuOfFp1.ScaleEncodedBlockHeader, &lastBuOfFp2Header)
		assert.NoError(t, err)
		// NewJustification or GrandpaNewAuthorities
		// assert.EqualValues(t, , lastBuOfFp1Header.Number)

		var fp3 ParachainFinalityProof
		_, err = codec.RLP.UnmarshalFromBytes(fps[2], &fp3)
		assert.NoError(t, err)
		var bpOfFp3 chain.BlockProof
		_, err = codec.RLP.UnmarshalFromBytes(fp3.RelayBlockProof, &bpOfFp3)
		assert.NoError(t, err)
		encodedIncludeHeader, err := types.EncodeToHexString(candidateIncludeHeader)
		assert.NoError(t, err)

		assert.EqualValues(t, lastBuOfFp2Header.Number, bpOfFp3.BlockWitness.Height)
		assert.EqualValues(t, encodedIncludeHeader, fmt.Sprintf("0x%x", bpOfFp3.Header))
	})

	// t.Run("should 1 blockheader when call pullBlockheader at UnknownHeaders nil", func(t *testing.T) {
	// 	t.Skip("Manual testing only")

	// 	substrateClient, err := substrate.NewSubstrateClient("https://rpc-relay.testnet.moonbeam.network")
	// 	require.NoError(t, err)

	// 	r := relayReceiver{
	// 		c:   substrateClient,
	// 		log: log.New(),
	// 	}

	// 	gj, hds, err := r.c.GetJustificationsAndUnknownHeaders(1091908)
	// 	require.NoError(t, err)
	// 	require.NotNil(t, gj)
	// 	require.Len(t, hds, 0)

	// 	fullhds, err := r.buildBlockUpdates(gj, hds)
	// 	assert.NotNil(t, fullhds)
	// 	assert.Len(t, fullhds, 1)
	// })

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
}
