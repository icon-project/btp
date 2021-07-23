package substrate

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type SubstrateHash = types.Hash
type SubstrateHeader = types.Header
type SubstrateMetaData = types.Metadata
type SubstrateStorageKey = types.StorageKey
type SubstrateStorageDataRaw = types.StorageDataRaw
type SubstrateBlockNumber = types.BlockNumber
type SubstrateEventRecordsRaw types.EventRecordsRaw
type HexString = string
type SubstrateReadProof struct {
	At    SubstrateHash `json:"at"`
	Proof []HexString   `json:"proof"`
}

type GrandpaBlockNumber = types.U32
type Round = types.U64

type GrandpaPrecommit struct {
	TargetHash   types.Hash
	TargetNumber GrandpaBlockNumber
}

type GrandpaSignedPrecommit struct {
	Precommit GrandpaPrecommit
	Signature types.Signature
	Id        types.AccountID
}

type GrandpaCommit struct {
	TargetHash   types.Hash
	TargetNumber GrandpaBlockNumber
	Precommits   []GrandpaSignedPrecommit
}

type GrandpaJustification struct {
	Round           Round
	Commit          GrandpaCommit
	VotesAncestries []types.Header
}

type Justification struct {
	ConsensusEngineId    [4]types.U8
	EncodedJustification GrandpaJustification
}

type FinalityProof struct {
	Block          types.Hash
	Justification  Justification
	UnknownHeaders []types.Header
}

type PersistedValidationData struct {
	ParentHead              types.Bytes
	RelayParentNumber       types.U32
	RelayParentStorageRoots types.Hash
	MaxPovSize              types.U32
}

type SubstrateClient interface {
	Call(result interface{}, method string, args ...interface{}) error
	GetMetadata(blockHash SubstrateHash) (*SubstrateMetaData, error)
	GetFinalizedHead() (SubstrateHash, error)
	GetHeader(hash SubstrateHash) (*SubstrateHeader, error)
	GetHeaderLatest() (*SubstrateHeader, error)
	GetBlockHash(blockNumber uint64) (SubstrateHash, error)
	GetStorageRaw(key SubstrateStorageKey, blockHash SubstrateHash) (*SubstrateStorageDataRaw, error)
	GetBlockHashLatest() (SubstrateHash, error)
	GetReadProof(key SubstrateStorageKey, blockHash SubstrateHash) (SubstrateReadProof, error)
	CreateStorageKey(meta *types.Metadata, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error)
	GetFinalitiyProof(blockNumber types.U32) (*FinalityProof, error)
	GetValidationData(blockHash SubstrateHash) (*PersistedValidationData, error)
}
