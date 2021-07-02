package pra

import (
	"testing"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
)

func TestEncodeHeader(t *testing.T) {
	api, err := gsrpc.NewSubstrateAPI("wss://wss.testnet.moonbeam.network")
	assert.NoError(t, err)

	hash, err := api.RPC.Chain.GetBlockHash(244537)
	assert.NoError(t, err)

	header, err := api.RPC.Chain.GetHeader(hash)
	assert.NoError(t, err)

	bytes, err := types.EncodeToBytes(header)
	assert.NoError(t, err)
	assert.NotEmpty(t, bytes)
	assert.Equal(t, bytes, types.MustHexDecodeString("0x661416628af29f05ff417f5dcc9aab4e051450f618b0698e15b9d97c86e59573e6ec0e007772a7e9a2569213fdf165da52c56f43448f6d1429d473bd387bb45c3f865bf6aabd6d24cc9a4885549b63ed43bf8fa1ab48f6708154338df575d15563b50cde0c046e6d627380a6676c0a755ca91225ba0bba3dd4e43a82aa3aaf516286361420ae50f790f2330466726f6e8801224a7f1fe922c176c91292b0726e1c9fc8d8d82146525d4f70acdaaa37a1d1f600056e6d6273010144b14164e5f0f9d56f3f54222eda7d4cf03fb868df7fba94d1b3f72ca50c862e395aeaa6b851e1fa4af048d7bbc71daf92d76de9ff2df33da64eee9dca53bf88"))
}
