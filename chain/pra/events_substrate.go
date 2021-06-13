package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

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

// SYSTEM EVENT DECLARATIONS
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

type EventTreasuryMinting struct {
	Phase  types.Phase
	Who    types.AccountID
	Topics []types.Hash
}

type EventNewMultiAccount struct {
	Phase   types.Phase
	Who, ID types.AccountID
	Topics  []types.Hash
}

type EventMultiAccountUpdated struct {
	Phase  types.Phase
	Who    types.AccountID
	Topics []types.Hash
}

type EventMultiAccountRemoved struct {
	Phase  types.Phase
	Who    types.AccountID
	Topics []types.Hash
}

type EventNewMultisig struct {
	Phase   types.Phase
	Who, ID types.AccountID
	Topics  []types.Hash
}

type EventMultisigApproval struct {
	Phase     types.Phase
	Who       types.AccountID
	TimePoint types.TimePoint
	ID        types.AccountID
	Topics    []types.Hash
}

type EventMultisigExecuted struct {
	Phase     types.Phase
	Who       types.AccountID
	TimePoint types.TimePoint
	ID        types.AccountID
	Result    types.DispatchResult
	Topics    []types.Hash
}

type EventMultisigCancelled struct {
	Phase     types.Phase
	Who       types.AccountID
	TimePoint types.TimePoint
	ID        types.AccountID
	Topics    []types.Hash
}

type SubstateWithFrontierEventRecord struct {
	types.EventRecords
	EVM_Log                          []EventEVMLog
	MultiAccount_NewMultiAccount     []EventNewMultiAccount     //nolint:stylecheck,golint
	MultiAccount_MultiAccountUpdated []EventMultiAccountUpdated //nolint:stylecheck,golint
	MultiAccount_MultiAccountRemoved []EventMultiAccountRemoved //nolint:stylecheck,golint
	MultiAccount_NewMultisig         []EventNewMultisig         //nolint:stylecheck,golint
	MultiAccount_MultisigApproval    []EventMultisigApproval    //nolint:stylecheck,golint
	MultiAccount_MultisigExecuted    []EventMultisigExecuted    //nolint:stylecheck,golint
	MultiAccount_MultisigCancelled   []EventMultisigCancelled   //nolint:stylecheck,golint
	TreasuryReward_TreasuryMinting   []EventTreasuryMinting     //nolint:stylecheck,golint
}
