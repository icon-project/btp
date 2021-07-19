package pra

import (
	stypes "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/substrate"
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
	Value [][]byte
}

type DecodedRelayMessage struct {
	BlockUpdates []BlockUpdate
	BlockProof   *chain.BlockProof
	StateProof   *[]StateProof
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
	Header *substrate.SubstrateHeader
	Hash   substrate.SubstrateHash
	Height uint64
}

type TransactionHashParam struct {
	From  EvmAddress
	Tx    *EvmTransaction
	Param *RelayMessageParam `json:"rm"`
}

func (thp TransactionHashParam) Hash() string {
	return thp.Tx.Hash().Hex()
}

func (thp TransactionHashParam) String() string {
	return thp.Tx.Hash().Hex()
}

type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*chain.EventProof
}

type BlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	FinalityProof           []byte
}

type PrachainFinalityProof struct{}
