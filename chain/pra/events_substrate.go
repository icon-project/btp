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

func (e *EventEVMLog) EvmLog() EvmLog {
	topics := []EvmHash{}

	for _, t := range e.Log.Topics {
		topics = append(topics, EvmHexToHash(t.Hex()))
	}

	return EvmLog{
		Address: EvmAddress(e.Log.Address),
		Topics:  topics,
		Data:    []byte(e.Log.Data),
	}
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

type ExitReason struct {
	IsSucceed     bool
	AsExitSucceed ExitSucceed
	IsError       bool
	AsExitError   ExitError
	IsRevert      bool
	AsExitRevert  ExitRevert
	IsFatal       bool
	AsExitFatal   ExitFatal
}

const (
	Succeed = iota
	Error
	Revert
	Fatal
)

type ExitError byte

const (
	StackUnderflow ExitError = iota
	StackOverflow
	InvalidJump
	InvalidRange
	DesignatedInvalid
	CallTooDeep
	CreateCollision
	CreateContractLimit
	OutOfOffset
	OutOfGas
	OutOfFund
	PCUnderflow
	CreateEmpty
	OtherError
)

type ExitSucceed byte

const (
	Stopped ExitSucceed = iota
	Returned
	Suicided
)

type ExitRevert byte

const (
	Reverted ExitRevert = iota
)

type ExitFatal byte

const (
	NotSupported ExitFatal = iota
	UnhandledInterrupt
	CallErrorAsFatal
	OtherFatal
)

func (ere *ExitReason) Decode(decoder scale.Decoder) error {
	fb, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	sb, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	switch fb {
	case byte(Succeed):
		ere.IsSucceed = true
		ere.AsExitSucceed = ExitSucceed(sb)
	case byte(Error):
		ere.IsError = true
		ere.AsExitError = ExitError(sb)
	case byte(Revert):
		ere.IsRevert = true
		ere.AsExitRevert = ExitRevert(sb)
	case byte(Fatal):
		ere.IsFatal = true
		ere.AsExitFatal = ExitFatal(sb)
	default:
		return fmt.Errorf("unknown ExitReason: %v, or ExitMessage index: %v", fb, sb)
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
