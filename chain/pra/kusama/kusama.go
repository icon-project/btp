package kusama

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain/pra/relaychain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
)

// EventSystemRemarked is emitted on on-chain remark happened.
type EventSystemRemarked struct {
	Phase      types.Phase
	Origin     types.AccountID
	RemarkHash types.Hash
	Topics     []types.Hash
}

type EventSocietyMemberSuspended = types.EventSocietyMemberSuspended
type EventSocietyChallenged = types.EventSocietyChallenged
type EventSocietyVote = types.EventSocietyVote
type EventSocietyDefenderVote = types.EventSocietyDefenderVote
type EventSocietyNewMaxMembers = types.EventSocietyNewMaxMembers
type EventSocietyUnfounded = types.EventSocietyUnfounded
type EventSocietyDeposit = types.EventSocietyDeposit

type EventRecoveryRecoveryCreated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRecoveryRecoveryInitiated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRecoveryRecoveryVouched struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRecoveryRecoveryClosed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRecoveryAccountRecovered struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRecoveryRecoveryRemoved struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventVestingVestingUpdated = types.EventVestingVestingUpdated
type EventVestingVestingCompleted = types.EventVestingVestingCompleted
type EventSchedulerScheduled = types.EventSchedulerScheduled
type EventSchedulerCanceled = types.EventSchedulerCanceled
type EventSchedulerDispatched = types.EventSchedulerDispatched
type EventProxyProxyExecuted = types.EventProxyProxyExecuted
type EventProxyAnonymousCreated = types.EventProxyAnonymousCreated
type EventProxyAnnounced = types.EventProxyAnnounced
type EventMultisigNewMultisig = types.EventMultisigNewMultisig

type EventMultisigMultisigApproval = types.EventMultisigApproval
type EventMultisigMultisigExecuted = types.EventMultisigExecuted
type EventMultisigMultisigCancelled = types.EventMultisigCancelled

type EventBountiesBountyProposed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyRejected struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyBecameActive struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyAwarded struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyClaimed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyCanceled struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventBountiesBountyExtended struct {
	Phase  types.Phase
	Topics []types.Hash
}

type EventTipsNewTip struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventTipsTipClosing struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventTipsTipClosed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventTipsTipRetracted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventTipsTipSlashed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseSolutionStored struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseElectionFinalized struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseRewarded struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseSlashed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseSignedPhaseStarted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventElectionProviderMultiPhaseUnsignedPhaseStarted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventGiltBidPlaced struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventGiltBidRetracted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventGiltGiltIssued struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventGiltGiltThawed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasCurrentCodeUpdated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasCurrentHeadUpdated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasCodeUpgradeScheduled struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasNewHeadNoted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasActionQueued struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasUmpInvalidFormat struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasUmpUnsupportedVersion struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasUmpExecutedUpward struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasUmpWeightExhausted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasUmpUpwardMessagesReceived struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasHrmpOpenChannelRequested struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasHrmpOpenChannelAccepted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParasHrmpChannelClosed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRegistrarRegistered struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRegistrarDeregistered struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventRegistrarReserved struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventSlotsNewLeasePeriod struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventSlotsLeased struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsAuctionStarted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsAuctionClosed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsReserved struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsUnreserved struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsReserveConfiscated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsBidAccepted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuctionsWinningOffset struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanCreated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanContributed struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanWithdrew struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanPartiallyRefunded struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanAllRefunded struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanDissolved struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanHandleBidResult struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanEdited struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanMemoUpdated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanAddedToNewRaise struct {
	Phase  types.Phase
	Topics []types.Hash
}

// type EventXcmPalletAttempted struct {
// 	Phase  types.Phase
// 	Outcome
// 	Topics []types.Hash
// }

// type EventXcmPalletSent struct {
// 	Phase  types.Phase
// 	MultiLocation
// 	MultiLocation
// 	Xcm
// 	Topics []types.Hash
// }

func NewKusamaRecord(sdr *substrate.SubstrateStorageDataRaw, meta *substrate.SubstrateMetaData) *KusamaEventRecord {
	records := &KusamaEventRecord{}
	if err := substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		log.Debugf("NewKusamaRecord decode fails: %v", err)
		return nil
	}

	return records
}

type KusamaEventRecord struct {
	relaychain.RelayChainEventRecord
	Society_MemberSuspended                         []EventSocietyMemberSuspended
	Society_Challenged                              []EventSocietyChallenged
	Society_Vote                                    []EventSocietyVote
	Society_DefenderVote                            []EventSocietyDefenderVote
	Society_NewMaxMembers                           []EventSocietyNewMaxMembers
	Society_Unfounded                               []EventSocietyUnfounded
	Society_Deposit                                 []EventSocietyDeposit
	Recovery_RecoveryCreated                        []EventRecoveryRecoveryCreated
	Recovery_RecoveryInitiated                      []EventRecoveryRecoveryInitiated
	Recovery_RecoveryVouched                        []EventRecoveryRecoveryVouched
	Recovery_RecoveryClosed                         []EventRecoveryRecoveryClosed
	Recovery_AccountRecovered                       []EventRecoveryAccountRecovered
	Recovery_RecoveryRemoved                        []EventRecoveryRecoveryRemoved
	Vesting_VestingUpdated                          []EventVestingVestingUpdated
	Vesting_VestingCompleted                        []EventVestingVestingCompleted
	Scheduler_Scheduled                             []EventSchedulerScheduled
	Scheduler_Canceled                              []EventSchedulerCanceled
	Scheduler_Dispatched                            []EventSchedulerDispatched
	Proxy_ProxyExecuted                             []EventProxyProxyExecuted
	Proxy_AnonymousCreated                          []EventProxyAnonymousCreated
	Proxy_Announced                                 []EventProxyAnnounced
	Multisig_NewMultisig                            []EventMultisigNewMultisig
	Multisig_MultisigApproval                       []EventMultisigMultisigApproval
	Multisig_MultisigExecuted                       []EventMultisigMultisigExecuted
	Multisig_MultisigCancelled                      []EventMultisigMultisigCancelled
	Bounties_BountyProposed                         []EventBountiesBountyProposed
	Bounties_BountyRejected                         []EventBountiesBountyRejected
	Bounties_BountyBecameActive                     []EventBountiesBountyBecameActive
	Bounties_BountyAwarded                          []EventBountiesBountyAwarded
	Bounties_BountyClaimed                          []EventBountiesBountyClaimed
	Bounties_BountyCanceled                         []EventBountiesBountyCanceled
	Bounties_BountyExtended                         []EventBountiesBountyExtended
	Tips_NewTip                                     []EventTipsNewTip
	Tips_TipClosing                                 []EventTipsTipClosing
	Tips_TipClosed                                  []EventTipsTipClosed
	Tips_TipRetracted                               []EventTipsTipRetracted
	Tips_TipSlashed                                 []EventTipsTipSlashed
	ElectionProviderMultiPhase_SolutionStored       []EventElectionProviderMultiPhaseSolutionStored
	ElectionProviderMultiPhase_ElectionFinalized    []EventElectionProviderMultiPhaseElectionFinalized
	ElectionProviderMultiPhase_Rewarded             []EventElectionProviderMultiPhaseRewarded
	ElectionProviderMultiPhase_Slashed              []EventElectionProviderMultiPhaseSlashed
	ElectionProviderMultiPhase_SignedPhaseStarted   []EventElectionProviderMultiPhaseSignedPhaseStarted
	ElectionProviderMultiPhase_UnsignedPhaseStarted []EventElectionProviderMultiPhaseUnsignedPhaseStarted
	Gilt_BidPlaced                                  []EventGiltBidPlaced
	Gilt_BidRetracted                               []EventGiltBidRetracted
	Gilt_GiltIssued                                 []EventGiltGiltIssued
	Gilt_GiltThawed                                 []EventGiltGiltThawed
	Paras_CurrentCodeUpdated                        []EventParasCurrentCodeUpdated
	Paras_CurrentHeadUpdated                        []EventParasCurrentHeadUpdated
	Paras_CodeUpgradeScheduled                      []EventParasCodeUpgradeScheduled
	Paras_NewHeadNoted                              []EventParasNewHeadNoted
	Paras_ActionQueued                              []EventParasActionQueued
	ParasUmp_InvalidFormat                          []EventParasUmpInvalidFormat
	ParasUmp_UnsupportedVersion                     []EventParasUmpUnsupportedVersion
	ParasUmp_ExecutedUpward                         []EventParasUmpExecutedUpward
	ParasUmp_WeightExhausted                        []EventParasUmpWeightExhausted
	ParasUmp_UpwardMessagesReceived                 []EventParasUmpUpwardMessagesReceived
	ParasHrmp_OpenChannelRequested                  []EventParasHrmpOpenChannelRequested
	ParasHrmp_OpenChannelAccepted                   []EventParasHrmpOpenChannelAccepted
	ParasHrmp_ChannelClosed                         []EventParasHrmpChannelClosed
	Registrar_Registered                            []EventRegistrarRegistered
	Registrar_Deregistered                          []EventRegistrarDeregistered
	Registrar_Reserved                              []EventRegistrarReserved
	Slots_NewLeasePeriod                            []EventSlotsNewLeasePeriod
	Slots_Leased                                    []EventSlotsLeased
	Auctions_AuctionStarted                         []EventAuctionsAuctionStarted
	Auctions_AuctionClosed                          []EventAuctionsAuctionClosed
	Auctions_Reserved                               []EventAuctionsReserved
	Auctions_Unreserved                             []EventAuctionsUnreserved
	Auctions_ReserveConfiscated                     []EventAuctionsReserveConfiscated
	Auctions_BidAccepted                            []EventAuctionsBidAccepted
	Auctions_WinningOffset                          []EventAuctionsWinningOffset
	Crowdloan_Created                               []EventCrowdloanCreated
	Crowdloan_Contributed                           []EventCrowdloanContributed
	Crowdloan_Withdrew                              []EventCrowdloanWithdrew
	Crowdloan_PartiallyRefunded                     []EventCrowdloanPartiallyRefunded
	Crowdloan_AllRefunded                           []EventCrowdloanAllRefunded
	Crowdloan_Dissolved                             []EventCrowdloanDissolved
	Crowdloan_HandleBidResult                       []EventCrowdloanHandleBidResult
	Crowdloan_Edited                                []EventCrowdloanEdited
	Crowdloan_MemoUpdated                           []EventCrowdloanMemoUpdated
	Crowdloan_AddedToNewRaise                       []EventCrowdloanAddedToNewRaise
	// XcmPallet_Attempted     []EventXcmPalletAttempted
	// XcmPallet_Sent          []EventXcmPalletSent
	System_CodeUpdated      []types.EventSystemCodeUpdated
	System_ExtrinsicFailed  []types.EventSystemExtrinsicFailed
	System_ExtrinsicSuccess []types.EventSystemExtrinsicSuccess
	System_KilledAccount    []types.EventSystemKilledAccount
	System_NewAccount       []types.EventSystemNewAccount
	System_Remarked         []EventSystemRemarked
}
