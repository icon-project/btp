package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/types"
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
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

// EventMultiAccountRemoved is emitted when a multi account has been removed. First param is the multisig account.
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

type EventFungibleTransfer struct {
	Phase        types.Phase
	Destination  types.U8
	DepositNonce types.U64
	ResourceId   types.Bytes32
	Amount       types.U256
	Recipient    types.Bytes
	Topics       []types.Hash
}

type EventNonFungibleTransfer struct {
	Phase        types.Phase
	Destination  types.U8
	DepositNonce types.U64
	ResourceId   types.Bytes32
	TokenId      types.Bytes
	Recipient    types.Bytes
	Metadata     types.Bytes
	Topics       []types.Hash
}

type EventGenericTransfer struct {
	Phase        types.Phase
	Destination  types.U8
	DepositNonce types.U64
	ResourceId   types.Bytes32
	Metadata     types.Bytes
	Topics       []types.Hash
}

type EventRelayerThresholdChanged struct {
	Phase     types.Phase
	Threshold types.U32
	Topics    []types.Hash
}

type EventChainWhitelisted struct {
	Phase   types.Phase
	ChainId types.U8
	Topics  []types.Hash
}

type EventRelayerAdded struct {
	Phase   types.Phase
	Relayer types.AccountID
	Topics  []types.Hash
}

type EventRelayerRemoved struct {
	Phase   types.Phase
	Relayer types.AccountID
	Topics  []types.Hash
}

type EventVoteFor struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Voter        types.AccountID
	Topics       []types.Hash
}

type EventVoteAgainst struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Voter        types.AccountID
	Topics       []types.Hash
}

type EventProposalApproved struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Topics       []types.Hash
}

type EventProposalRejected struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Topics       []types.Hash
}

type EventProposalSucceeded struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Topics       []types.Hash
}

type EventProposalFailed struct {
	Phase        types.Phase
	SourceId     types.U8
	DepositNonce types.U64
	Topics       []types.Hash
}

// Edgeware specs in https://github.com/hicommonwealth/edgeware-node-types/tree/master/src/interfaces
// Rust implementation of module_event in https://github.com/hicommonwealth/edgeware-node/tree/master/modules
// Original go events for ChainBridge https://github.com/ChainSafe/ChainBridge/blob/main/shared/substrate/events.go

type SubstateWithFrontierEventRecord struct {
	types.EventRecords
	EVM_Log                             []EventEVMLog
	Council_Proposed                    []types.EventCollectiveProposed       //nolint:stylecheck,golint
	Council_Voted                       []types.EventCollectiveVoted          //nolint:stylecheck,golint
	Council_Approved                    []types.EventCollectiveApproved       //nolint:stylecheck,golint
	Council_Disapproved                 []types.EventCollectiveDisapproved    //nolint:stylecheck,golint
	Council_Executed                    []types.EventCollectiveExecuted       //nolint:stylecheck,golint
	Council_MemberExecuted              []types.EventCollectiveMemberExecuted //nolint:stylecheck,golint
	Council_Closed                      []types.EventCollectiveClosed         //nolint:stylecheck,golint
	ChainBridge_FungibleTransfer        []EventFungibleTransfer               //nolint:stylecheck,golint
	ChainBridge_NonFungibleTransfer     []EventNonFungibleTransfer            //nolint:stylecheck,golint
	ChainBridge_GenericTransfer         []EventGenericTransfer                //nolint:stylecheck,golint
	ChainBridge_RelayerThresholdChanged []EventRelayerThresholdChanged        //nolint:stylecheck,golint
	ChainBridge_ChainWhitelisted        []EventChainWhitelisted               //nolint:stylecheck,golint
	ChainBridge_RelayerAdded            []EventRelayerAdded                   //nolint:stylecheck,golint
	ChainBridge_RelayerRemoved          []EventRelayerRemoved                 //nolint:stylecheck,golint
	ChainBridge_VoteFor                 []EventVoteFor                        //nolint:stylecheck,golint
	ChainBridge_VoteAgainst             []EventVoteAgainst                    //nolint:stylecheck,golint
	ChainBridge_ProposalApproved        []EventProposalApproved               //nolint:stylecheck,golint
	ChainBridge_ProposalRejected        []EventProposalRejected               //nolint:stylecheck,golint
	ChainBridge_ProposalSucceeded       []EventProposalSucceeded              //nolint:stylecheck,golint
	ChainBridge_ProposalFailed          []EventProposalFailed                 //nolint:stylecheck,golint
	MultiAccount_NewMultiAccount        []EventNewMultiAccount                //nolint:stylecheck,golint
	MultiAccount_MultiAccountUpdated    []EventMultiAccountUpdated            //nolint:stylecheck,golint
	MultiAccount_MultiAccountRemoved    []EventMultiAccountRemoved            //nolint:stylecheck,golint
	MultiAccount_NewMultisig            []EventNewMultisig                    //nolint:stylecheck,golint
	MultiAccount_MultisigApproval       []EventMultisigApproval               //nolint:stylecheck,golint
	MultiAccount_MultisigExecuted       []EventMultisigExecuted               //nolint:stylecheck,golint
	MultiAccount_MultisigCancelled      []EventMultisigCancelled              //nolint:stylecheck,golint
	TreasuryReward_TreasuryMinting      []EventTreasuryMinting                //nolint:stylecheck,golint
}
