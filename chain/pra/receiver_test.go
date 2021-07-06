package pra

import (
	"fmt"
	"testing"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestEncodeHeader(t *testing.T) {
	api, err := gsrpc.NewSubstrateAPI("wss://wss.testnet.moonbeam.network")
	require.NoError(t, err)

	hash, err := api.RPC.Chain.GetBlockHash(243221)
	require.NoError(t, err)

	header, err := api.RPC.Chain.GetHeader(hash)
	require.NoError(t, err)

	bytes, err := types.EncodeToBytes(header)
	t.Run("should encode header by scale codec", func(t *testing.T) {
		assert.NoError(t, err)
		assert.NotEmpty(t, bytes)
		assert.Equal(t, "0x4b6ca5b74e19d4bc04280edf20a53a4ebe1402cbb2ef7ed9a1611fb8411a33ca56d80e006415ef11020701d83ae5456ccecb3685eb39acc6b52d3e481214d8f1aa6b5465e347562ba9c7c993862047e1d6f020c25ab4139676afbf7170c254e2f36cefe70c046e6d62738060eed538a43e6738f4c560c5d950be96c72ad591f0c16f564c003b5c7b895c0e0466726f6e890101f7b9c5fb3f5b72f937ed511b173e5a39b9fb3ffaa1cc4dd024a4c7c36c7da8610847fec28647d5f0806548f385257170d76cf6e890f7467ef33b36dcc5b9be1b15d0fdb267aa2fce057cc81a0e2397f5a32ffd8637753f7d0c0c0b7289b002dc3f056e6d627301019a1b7069e8aa71015a15925595589999890dba42cb87dae2b5aabfee791bad47d380392c626eef34701ea3a95d7dd4fc891759cb375991aacbc1ee3663742289",
			fmt.Sprintf("0x%x", bytes),
		)
	})
}

func TestReceiver_ReceiveLoop(t *testing.T) {
	r := NewReceiver("btp://0x507.pra/0x6a436465184fA9b0b5f20fbeFADaF83CaC466ACD", "btp://0x3.icon/cxdc2a468aada7a4826176e87ae72d6ee24c50df0a", "wss://wss.testnet.moonbeam.network", nil, log.New())

	t.Run("should monitor from the given height", func(t *testing.T) {
		err := r.ReceiveLoop(243221, 1, func(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
			assert.EqualValues(t, bu.Height, 243221)
			assert.Equal(t, "0xed27ad8166e3d8a3ff54a4687547b77ef0d95600d94b2521af9f700665100bb3", fmt.Sprintf("0x%x", bu.BlockHash))
			assert.Equal(t, "0x4b6ca5b74e19d4bc04280edf20a53a4ebe1402cbb2ef7ed9a1611fb8411a33ca56d80e006415ef11020701d83ae5456ccecb3685eb39acc6b52d3e481214d8f1aa6b5465e347562ba9c7c993862047e1d6f020c25ab4139676afbf7170c254e2f36cefe70c046e6d62738060eed538a43e6738f4c560c5d950be96c72ad591f0c16f564c003b5c7b895c0e0466726f6e890101f7b9c5fb3f5b72f937ed511b173e5a39b9fb3ffaa1cc4dd024a4c7c36c7da8610847fec28647d5f0806548f385257170d76cf6e890f7467ef33b36dcc5b9be1b15d0fdb267aa2fce057cc81a0e2397f5a32ffd8637753f7d0c0c0b7289b002dc3f056e6d627301019a1b7069e8aa71015a15925595589999890dba42cb87dae2b5aabfee791bad47d380392c626eef34701ea3a95d7dd4fc891759cb375991aacbc1ee3663742289",
				fmt.Sprintf("0x%x", bu.Header),
			)
			assert.Equal(t, "0xf90140b9013b4b6ca5b74e19d4bc04280edf20a53a4ebe1402cbb2ef7ed9a1611fb8411a33ca56d80e006415ef11020701d83ae5456ccecb3685eb39acc6b52d3e481214d8f1aa6b5465e347562ba9c7c993862047e1d6f020c25ab4139676afbf7170c254e2f36cefe70c046e6d62738060eed538a43e6738f4c560c5d950be96c72ad591f0c16f564c003b5c7b895c0e0466726f6e890101f7b9c5fb3f5b72f937ed511b173e5a39b9fb3ffaa1cc4dd024a4c7c36c7da8610847fec28647d5f0806548f385257170d76cf6e890f7467ef33b36dcc5b9be1b15d0fdb267aa2fce057cc81a0e2397f5a32ffd8637753f7d0c0c0b7289b002dc3f056e6d627301019a1b7069e8aa71015a15925595589999890dba42cb87dae2b5aabfee791bad47d380392c626eef34701ea3a95d7dd4fc891759cb375991aacbc1ee3663742289f800",
				fmt.Sprintf("0x%x", bu.Proof),
			)
			r.StopReceiveLoop()
		}, func() {})

		assert.NoError(t, err)
	})

	// t.Run("should call bmc.parseMessage when Parachain emits EVM Log")
	// t.Run("should build StateProof when EVM Log events contains BMC SendMessage Event")
}
