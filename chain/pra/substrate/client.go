package substrate

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

func (c *SubstrateAPI) GetReadProof(key SubstrateStorageKey, hash SubstrateHash) (SubstrateReadProof, error) {
	var res SubstrateReadProof
	err := c.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *SubstrateAPI) CreateStorageKey(meta *types.Metadata, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error) {
	key, err := types.CreateStorageKey(meta, prefix, method, arg, arg2)
	if err != nil {
		return nil, err
	}

	return SubstrateStorageKey(key), nil
}

func (c *SubstrateAPI) GetFinalitiyProof(blockNumber types.U32) (*FinalityProof, error) {
	var finalityProofHexstring string
	err := c.Call(&finalityProofHexstring, "grandpa_proveFinality", blockNumber)
	if err != nil {
		return nil, err
	}

	fp := &FinalityProof{}
	err = types.DecodeFromHexString(finalityProofHexstring, fp)
	if err != nil {
		return nil, err
	}

	return fp, err
}

func (c *SubstrateAPI) GetValidationData(blockHash SubstrateHash) (*PersistedValidationData, error) {
	meta, err := c.GetMetadata(blockHash)
	if err != nil {
		return nil, err
	}

	key, err := types.CreateStorageKey(meta, "ParachainSystem", "ValidationData", nil, nil)
	if err != nil {
		return nil, err
	}

	storageRaw, err := c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	pvd := &PersistedValidationData{}
	err = types.DecodeFromBytes(*storageRaw, pvd)

	return pvd, err
}
