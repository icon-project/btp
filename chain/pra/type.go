package pra

import (
	stypes "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain"
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

type RelayMessageParam struct {
	Prev string `json:"_prev"`
	Msg  string `json:"_msg"`
}

type StateProof struct {
	Key   []byte
	Value []string
}

type BlockWitness struct {
	Height  uint64
	Witness [][]byte
}

type BlockProof struct {
	Header       []byte
	BlockWitness *BlockWitness
}

type DecodedRelayMessage struct {
	BlockUpdates []BlockUpdate
	BlockProof   *BlockProof
	StateProof   *StateProof
}

type PraRelayMessage struct {
	BlockUpdates [][]byte
	BlockProof   []byte
	StateProof   []byte
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

func (rm RelayMessage) Size() int {
	size := len(rm.BlockProof)
	for _, blockUpdate := range rm.BlockUpdates {
		size += len(blockUpdate)
	}
	for _, receiptProof := range rm.ReceiptProofs {
		size += len(receiptProof)
	}
	return size
}

type SignedHeader struct {
	stypes.Header
	stypes.Justification
}

type BlockNotification struct {
	Header *stypes.Header
	Hash   SubstrateHash
	Height uint64
	Events *MoonriverEventRecord
}

type ReadProof struct {
	At    SubstrateHash `json:"at"`
	Proof []string      `json:"proof"`
}

type TransactionHashParam struct {
	From  EvmAddress
	Tx    *EvmTransaction
	Param *RelayMessageParam `json:"rm"`
}

func (thp TransactionHashParam) Hash() string {
	return thp.Tx.Hash().Hex()
}

type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*chain.EventProof
}

type BlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	FinalityProof           *[]byte
}

type PrachainFinalityProof struct{}
