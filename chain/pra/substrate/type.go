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
type SubstrateParachainId = U32
type RelaychainAccountId = types.AccountID
type Signature = types.Signature
type RelaychainSignature = types.Signature
type RuntimeVersion = types.RuntimeVersion
type SetId = types.U64
type U8 = types.U8
type U32 = types.U32
type U64 = types.U64
type HexString = string
type HeadData = types.Bytes
type CoreIndex = U32
type GroupIndex = U32
type Phase = types.Phase
type GrandpaBlockNumber = U32
type Round = types.U64

type SubstrateReadProof struct {
	At    SubstrateHash `json:"at"`
	Proof []HexString   `json:"proof"`
}

type GrandpaPrecommit struct {
	TargetHash   SubstrateHash
	TargetNumber GrandpaBlockNumber
}

type GrandpaSignedPrecommit struct {
	Precommit GrandpaPrecommit
	Signature Signature
	Id        types.AccountID
}

type GrandpaCommit struct {
	TargetHash   SubstrateHash
	TargetNumber GrandpaBlockNumber
	Precommits   []GrandpaSignedPrecommit
}

type GrandpaJustification struct {
	Round           Round
	Commit          GrandpaCommit
	VotesAncestries []SubstrateHeader
}

type Justification struct {
	ConsensusEngineId    [4]U8
	EncodedJustification GrandpaJustification
}

type WestendJustification struct {
	ConsensusEngineId    [2]U8
	EncodedJustification GrandpaJustification
}

type WestendFinalityProof struct {
	Block          SubstrateHash
	Justification  WestendJustification
	UnknownHeaders []SubstrateHeader
}

type FinalityProof struct {
	Block          SubstrateHash
	Justification  Justification
	UnknownHeaders []SubstrateHeader
}

type PersistedValidationData struct {
	ParentHead              types.Bytes
	RelayParentNumber       U32
	RelayParentStorageRoots SubstrateHash
	MaxPovSize              U32
}

type SignedMessage struct {
	TargetHash   SubstrateHash
	TargetNumber U32
}

type SignedMessageEnum struct {
	IsPrevote        bool
	AsPrevote        SignedMessage
	IsPrecommit      bool
	AsPrecommit      SignedMessage
	IsPrimaryPropose bool
	AsPrimaryPropose SignedMessage
}

type VoteMessage struct {
	Message SignedMessageEnum
	Round   U64
	SetId   U64
}

type CandidateDescriptor struct {
	ParaId                      U32       `json:"para_id"`
	RelayParent                 HexString `json:"relay_parent"`
	Collator                    HexString `json:"collator"`
	PersistedValidationDataHash HexString `json:"persisted_validation_data_hash"`
	PovHash                     HexString `json:"pov_hash"`
	ErasureRoot                 HexString `json:"erasure_root"`
	Signature                   HexString `json:"signature"`
	ParaHead                    HexString `json:"para_head"`
	ValidationCodeHash          HexString `json:"validation_code_hash"`
}

type CandidateReceipt struct {
	Descriptor      CandidateDescriptor `json:"descriptor"`
	CommitmentsHash HexString           `json:"commitments_hash"`
}

type ParasInclusionCandidateIncludedParams struct {
	CandidateReceipt CandidateReceipt `json:"polkadot_primitives:v1:CandidateReceipt@86"`
	HeadData         HeadData
	CoreIndex        CoreIndex
	GroupIndex       GroupIndex
}

type EthereumLog struct {
	Address string   `json:"address"`
	Topics  []string `json:"topics"`
	Data    string   `json:"data"`
}

type EVMLogParams struct {
	Log EthereumLog `json:"log"`
}

type SubstrateClient interface {
	Init()
	Call(result interface{}, method string, args ...interface{}) error
	GetFinalizedHead() (SubstrateHash, error)
	GetHeader(blockHash SubstrateHash) (*SubstrateHeader, error)
	GetHeaderLatest() (*SubstrateHeader, error)
	GetBlockHash(blockNumber uint64) (SubstrateHash, error)
	GetStorageRaw(key SubstrateStorageKey, blockHash SubstrateHash) (*SubstrateStorageDataRaw, error)
	GetBlockHashLatest() (SubstrateHash, error)
	GetSpecName() string
	GetReadProof(key SubstrateStorageKey, blockHash SubstrateHash) (SubstrateReadProof, error)
	CreateStorageKey(meta *SubstrateMetaData, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error)
	GetJustificationsAndUnknownHeaders(blockNumber SubstrateBlockNumber) (*GrandpaJustification, []SubstrateHeader, error)
	GetGrandpaCurrentSetId(blockHash SubstrateHash) (SetId, error)
	GetValidationData(blockHash SubstrateHash) (*PersistedValidationData, error)
	GetParachainId() (*SubstrateParachainId, error)
	SubcribeFinalizedHeadAt(height uint64, cb func(*SubstrateHash)) error
	GetBlockHeaderByBlockNumbers(blockNumbers []SubstrateBlockNumber) ([]SubstrateHeader, error)
	GetSystemEventStorageKey(blockhash SubstrateHash) (SubstrateStorageKey, error)
	GetSystemEvents(blockHash SubstrateHash, section string, method string) ([]map[string]interface{}, error)
}
