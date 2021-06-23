package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

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

// EventSystemRemarked is emitted on on-chain remark happened.
type EventSystemRemarked struct {
	Phase      types.Phase
	Origin     types.AccountID
	RemarkHash types.Hash
	Topics     []types.Hash
}

type EventEthereumExecuted struct {
	Phase           types.Phase
	From            types.H160
	To              types.H160
	TransactionHash types.H256
	// 	/// Exit reason.
	// #[derive(Clone, Debug, Eq, PartialEq)]
	// #[cfg_attr(feature = "with-codec", derive(codec::Encode, codec::Decode))]
	// #[cfg_attr(feature = "with-serde", derive(serde::Serialize, serde::Deserialize))]
	// pub enum ExitReason {
	// 	/// Machine has succeeded.
	// 	Succeed(ExitSucceed),
	// 	/// Machine returns a normal EVM error.
	// 	Error(ExitError),
	// 	/// Machine encountered an explict revert.
	// 	Revert(ExitRevert),
	// 	/// Machine encountered an error that is not supposed to be normal EVM
	// 	/// errors, such as requiring too much memory to execute.
	// 	Fatal(ExitFatal),
	// }
	ExitReason types.ExampleEnum
	Topics     []types.Hash
}

type EventEVMLog struct {
	Phase  types.Phase
	Log    EthereumLog
	Topics []types.Hash
}

type FrontierEventRecord struct {
	// EVM_BalanceDeposit
	// EVM_BalanceWithdraw
	// EVM_Created
	// EVM_CreatedFailed
	// EVM_Executed
	// EVM_ExecutedFailed
	EVM_Log           []EventEVMLog
	Ethereum_Executed []EventEthereumExecuted
}

type SubstateWithFrontierEventRecord struct {
	types.EventRecords
	FrontierEventRecord
}
