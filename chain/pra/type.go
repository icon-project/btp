package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/scale"
	"github.com/centrifuge/go-substrate-rpc-client/types"
	"github.com/icon-project/btp/common/jsonrpc"
)

const (
	JsonrpcApiVersion                                = 3
	JsonrpcErrorCodeSystem         jsonrpc.ErrorCode = -31000
	JsonrpcErrorCodeTxPoolOverflow jsonrpc.ErrorCode = -31001
	JsonrpcErrorCodePending        jsonrpc.ErrorCode = -31002
	JsonrpcErrorCodeExecuting      jsonrpc.ErrorCode = -31003
	JsonrpcErrorCodeNotFound       jsonrpc.ErrorCode = -31004
	JsonrpcErrorLackOfResource     jsonrpc.ErrorCode = -31005
	JsonrpcErrorCodeTimeout        jsonrpc.ErrorCode = -31006
	JsonrpcErrorCodeSystemTimeout  jsonrpc.ErrorCode = -31007
	JsonrpcErrorCodeScore          jsonrpc.ErrorCode = -30000
)

const (
	DuplicateTransactionError = iota + 2000
	TransactionPoolOverflowError
	ExpiredTransactionError
	FutureTransactionError
	TransitionInterruptedError
	InvalidTransactionError
	InvalidQueryError
	InvalidResultError
	NoActiveContractError
	NotContractAddressError
	InvalidPatchDataError
	CommittedTransactionError
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}

type HandleRelayMessageParam struct {
	Prev string
	Msg  string
}

type RelayMessage struct {
	BlockUpdates  [][]byte
	BlockProof    []byte
	ReceiptProofs [][]byte
	//
	height              int64
	numberOfBlockUpdate int
	eventSequence       int64
	numberOfEvent       int
}

type BlockNotification struct {
	Header *types.Header
	Hash   types.Hash
	Height uint64
	Events types.EventRecordsRaw
}

type BlockUpdate struct {
	// header of updating block
	// SCALE encode of BlockHeader
	BlockHeader []byte

	// optional
	// signatures of validators with fields that included in vote
	// RLP encode of Votes
	Votes []byte
}

// validator's signatures to prove that block's confirmed
type Votes struct {
	// SCALE codec of VoteMessage
	VoteMessage []VoteMessage
	// list RLP of ValidatorSignature
	Signatures []byte
}

// validator will SCALE encode that message and then sign it.
type VoteMessage = []byte

type BlockProof struct {
	// SCALE codec of block header
	// Header of block contains event
	BlockHeader []byte
	Height      uint32 // Merkle tree accumulator height when get witness
	Witness     []byte // list of hash of leaf point to prove that block include in Merkle tree accumulator, low level leaf first
}

type StateProof struct {
	// key of storage
	Key []byte
	// get from api, https://polkadot.js.org/docs/substrate/rpc#getreadproofkeys-vecstoragekey-at-blockhash-readproof
	// data of MPT node (branch node, leaf node) to prove that storage, from top to bottom
	// result from api may not in top to bottom order, relay must reorder that
	Proofs []byte
}

type ValidatorSignature struct {
	// 64 bytes signature
	Signature []byte
	// 32 byte public key of validator
	Validator []byte
}

type EventEVMLog struct {
	Phase  types.Phase
	Log    EthereumLog
	Topics []types.Hash
}

type EthereumLog struct {
	Address types.H160
	Topics  []types.H256
	Data    []byte
}

// Scale decode for EthereumLog
func (el *EthereumLog) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&el.Address)
	if err != nil {
		return err
	}

	err = decoder.Decode(&el.Topics)
	if err != nil {
		return err
	}

	err = decoder.Decode(&el.Data)
	return err
}

type SubstateWithFrontierEventRecord struct {
	EVM_Log []EventEVMLog
	types.EventRecords
}
