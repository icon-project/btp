package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
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

func NewStateProof(key substrate.SubstrateStorageKey, rp *substrate.SubstrateReadProof) *StateProof {
	proofs := [][]byte{}
	for _, p := range rp.Proof {
		// TODO move this function to substrate package
		if bp, err := types.HexDecodeString(p); err != nil {
			return nil
		} else {
			proofs = append(proofs, bp)
		}
	}

	return &StateProof{
		Key:   key,
		Value: proofs,
	}
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

type Votes struct {
	VoteMessage []byte
	Signatures  [][]byte
}

type RelayBlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	Votes                   []byte
}

type BlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	FinalityProof           []byte
}

type PrachainFinalityProof struct{}
