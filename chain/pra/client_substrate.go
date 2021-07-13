package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/client"
	"github.com/centrifuge/go-substrate-rpc-client/v3/rpc"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type SubstrateAPI struct {
	RPC *rpc.RPC
	client.Client
}

func NewSubstrateClient(url string) (*SubstrateAPI, error) {
	cl, err := client.Connect(url)
	if err != nil {
		return nil, err
	}

	newRPC, err := rpc.NewRPC(cl)
	if err != nil {
		return nil, err
	}

	return &SubstrateAPI{
		RPC:    newRPC,
		Client: cl,
	}, nil
}

func (c *SubstrateAPI) GetMetadata(blockHash SubstrateHash) (*SubstrateMetaData, error) {
	return c.RPC.State.GetMetadata(blockHash)
}

func (c *SubstrateAPI) GetFinalizedHead() (types.Hash, error) {
	return c.RPC.Chain.GetFinalizedHead()
}

func (c *SubstrateAPI) GetHeader(blockHash SubstrateHash) (*SubstrateHeader, error) {
	return c.RPC.Chain.GetHeader(blockHash)
}

func (c *SubstrateAPI) GetHeaderLatest() (*types.Header, error) {
	return c.RPC.Chain.GetHeaderLatest()
}

func (c *SubstrateAPI) GetBlockHash(blockNumber uint64) (SubstrateHash, error) {
	return c.RPC.Chain.GetBlockHash(blockNumber)
}

func (c *SubstrateAPI) GetStorageRaw(key SubstrateStorageKey, blockHash SubstrateHash) (*SubstrateStorageDataRaw, error) {
	return c.RPC.State.GetStorageRaw(key, blockHash)
}

func (c *SubstrateAPI) GetBlockHashLatest() (SubstrateHash, error) {
	return c.RPC.Chain.GetBlockHashLatest()
}

func (c *SubstrateAPI) GetReadProof(key SubstrateStorageKey, hash SubstrateHash) (ReadProof, error) {
	var res ReadProof
	err := c.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}
