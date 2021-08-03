package substrate

import (
	"sync"
	"time"

	"github.com/centrifuge/go-substrate-rpc-client/v3/client"
	"github.com/centrifuge/go-substrate-rpc-client/v3/rpc"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/common/log"
)

const (
	Westend   = "Westend"
	Kusama    = "Kusama"
	Moonriver = "Moonriver"
	Moonbase  = "Moonbase"
)

type SubstrateAPI struct {
	RPC      *rpc.RPC
	metadata *SubstrateMetaData
	metaLock sync.RWMutex // Lock metadata for updates, allows concurrent reads
	client.Client
	specName                  string
	keepSubscribeFinalizeHead chan bool
	blockInterval             time.Duration
	log                       log.Logger
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

	metadata, err := newRPC.State.GetMetadataLatest()
	if err != nil {
		return nil, err
	}

	rtv, err := newRPC.State.GetRuntimeVersionLatest()
	if err != nil {
		return nil, err
	}

	return &SubstrateAPI{
		RPC:                       newRPC,
		metadata:                  metadata,
		Client:                    cl,
		keepSubscribeFinalizeHead: make(chan bool),
		specName:                  rtv.SpecName,
		blockInterval:             time.Second * 3,
	}, nil
}

func (c *SubstrateAPI) GetMetadata(blockHash SubstrateHash) (*SubstrateMetaData, error) {
	return c.RPC.State.GetMetadata(blockHash)
}

func (c *SubstrateAPI) GetMetadataLatest() *SubstrateMetaData {
	c.metaLock.RLock()
	metadata := c.metadata
	c.metaLock.RUnlock()
	return metadata
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

func (c *SubstrateAPI) GetSpecName() string {
	return c.specName
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

func (c *SubstrateAPI) GetSystemEventStorageKey(blockhash SubstrateHash) (SubstrateStorageKey, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}
	return SubstrateStorageKey(key), nil
}

func (c *SubstrateAPI) GetGrandpaCurrentSetId(blockHash SubstrateHash) (*types.U64, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "Grandpa", "CurrentSetId", nil, nil)
	if err != nil {
		return nil, err
	}

	storageRaw, err := c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	var setId *types.U64
	err = types.DecodeFromBytes(*storageRaw, setId)

	return setId, err
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
	meta := c.GetMetadataLatest()
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

func (c *SubstrateAPI) SubcribeFinalizedHeadAt(height uint64, cb func(*SubstrateHash)) error {
	current := height

	for {
		select {
		case <-c.keepSubscribeFinalizeHead:
			return nil
		default:
			finalizedHeadHash, err := c.GetFinalizedHead()
			if err != nil {
				return err
			}

			finalizedHead, err := c.GetHeader(finalizedHeadHash)
			if err != nil {
				return err
			}

			if current > uint64(finalizedHead.Number) {
				c.log.Tracef("block not yet finalized target:%v latest:%v", current, finalizedHead.Number)
				<-time.After(c.blockInterval * (time.Duration(current) - time.Duration(finalizedHead.Number)))
				continue
			}

			if current == uint64(finalizedHead.Number) {
				cb(&finalizedHeadHash)
			}

			if current < uint64(finalizedHead.Number) {
				currentHash, err := c.GetBlockHash(current)
				if err != nil {
					return err
				}

				cb(&currentHash)
			}
		}
	}
}
