package pra

import (
	"fmt"

	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

// EventSystemRemarked is emitted on on-chain remark happened.
type EventSystemRemarked struct {
	Phase      types.Phase
	Origin     types.AccountID
	RemarkHash types.Hash
	Topics     []types.Hash
}

type EthereumAddress = types.H160
type ContractAddress = EthereumAddress
type EthereumTransactionValue = types.U256

type EventEVMBalanceDeposit struct {
	Phase   types.Phase
	Sender  types.AccountID
	Address EthereumAddress
	Value   EthereumTransactionValue
	Topics  []types.Hash
}

type EventEVMBalanceWithdraw struct {
	Phase   types.Phase
	Sender  types.AccountID
	Address EthereumAddress
	Value   EthereumTransactionValue
	Topics  []types.Hash
}

type EventEVMCreated struct {
	Phase    types.Phase
	Contract ContractAddress
	Topics   []types.Hash
}

type EventEVMCreatedFailed struct {
	Phase    types.Phase
	Contract ContractAddress
	Topics   []types.Hash
}

type EventEVMExecuted struct {
	Phase    types.Phase
	Contract ContractAddress
	Topics   []types.Hash
}

type EventEVMExecutedFailed struct {
	Phase    types.Phase
	Contract ContractAddress
	Topics   []types.Hash
}

type EventEVMLog struct {
	Phase  types.Phase
	Log    EthereumLog
	Topics []types.Hash
}

type EventEthereumExecuted struct {
	Phase           types.Phase
	From            types.H160
	To              types.H160
	TransactionHash types.H256
	ExitReason      ExitReason
	Topics          []types.Hash
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

type ExitReason byte

type ExitReasonEnum struct {
	IsSucceed bool
	IsError   bool
	IsRevert  bool
	IsFatal   bool
}

const (
	// Machine has succeeded..
	Succeed ExitReason = 0
	// Machine returns a normal EVM error.
	Error ExitReason = 1
	// Machine encountered an explict revert
	Revert ExitReason = 2
	/// Machine encountered an error that is not supposed to be normal EVM
	/// errors, such as requiring too much memory to execute.
	Fatal ExitReason = 3
)

func (ere *ExitReasonEnum) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	switch b {
	case byte(Succeed):
		ere.IsSucceed = true
	case byte(Error):
		ere.IsError = true
	case byte(Revert):
		ere.IsRevert = true
	case byte(Fatal):
		ere.IsFatal = true
	default:
		return fmt.Errorf("unknown ExitReason enum: %v", b)
	}

	return err
}

type FrontierEventRecord struct {
	EVM_BalanceDeposit  []EventEVMBalanceDeposit
	EVM_BalanceWithdraw []EventEVMBalanceWithdraw
	EVM_Created         []EventEVMCreated
	EVM_CreatedFailed   []EventEVMCreatedFailed
	EVM_Executed        []EventEVMExecuted
	EVM_ExecutedFailed  []EventEVMExecutedFailed
	EVM_Log             []EventEVMLog
	Ethereum_Executed   []EventEthereumExecuted
}
