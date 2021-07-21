package kusama

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type EventSocietyMemberSuspended = types.EventSocietyMemberSuspended
type EventSocietyChallenged = types.EventSocietyChallenged
type EventSocietyVote = types.EventSocietyVote
type EventSocietyDefenderVote = types.EventSocietyDefenderVote
type EventSocietyNewMaxMembers = types.EventSocietyNewMaxMembers
type EventSocietyUnfounded = types.EventSocietyUnfounded
type EventSocietyDeposit = types.EventSocietyDeposit

// type EventRecoveryRecoveryCreated = types.EventRecoveryRecoveryCreated
// type EventRecoveryRecoveryInitiated = types.EventRecoveryRecoveryInitiated
// type EventRecoveryRecoveryVouched = types.EventRecoveryRecoveryVouched
// type EventRecoveryRecoveryClosed = types.EventRecoveryRecoveryClosed
// type EventRecoveryAccountRecovered = types.EventRecoveryAccountRecovered
// type EventRecoveryRecoveryRemoved = types.EventRecoveryRecoveryRemoved
type EventVestingVestingUpdated = types.EventVestingVestingUpdated
type EventVestingVestingCompleted = types.EventVestingVestingCompleted
type EventSchedulerScheduled = types.EventSchedulerScheduled
type EventSchedulerCanceled = types.EventSchedulerCanceled
type EventSchedulerDispatched = types.EventSchedulerDispatched
type EventProxyProxyExecuted = types.EventProxyProxyExecuted
type EventProxyAnonymousCreated = types.EventProxyAnonymousCreated
type EventProxyAnnounced = types.EventProxyAnnounced
type EventMultisigNewMultisig = types.EventMultisigNewMultisig

// type EventMultisigMultisigApproval = types.EventMultisigMultisigApproval
// type EventMultisigMultisigExecuted = types.EventMultisigMultisigExecuted
// type EventMultisigMultisigCancelled = types.EventMultisigMultisigCancelled
// type EventBountiesBountyProposed = types.EventBountiesBountyProposed
// type EventBountiesBountyRejected = types.EventBountiesBountyRejected
// type EventBountiesBountyBecameActive = types.EventBountiesBountyBecameActive
// type EventBountiesBountyAwarded = types.EventBountiesBountyAwarded
// type EventBountiesBountyClaimed = types.EventBountiesBountyClaimed
// type EventBountiesBountyCanceled = types.EventBountiesBountyCanceled
// type EventBountiesBountyExtended = types.EventBountiesBountyExtended
// type EventTipsNewTip = types.EventTipsNewTip
// type EventTipsTipClosing = types.EventTipsTipClosing
// type EventTipsTipClosed = types.EventTipsTipClosed
// type EventTipsTipRetracted = types.EventTipsTipRetracted
// type EventTipsTipSlashed = types.EventTipsTipSlashed
// type EventElectionProviderMultiPhaseSolutionStored = types.EventElectionProviderMultiPhaseSolutionStored
// type EventElectionProviderMultiPhaseElectionFinalized = types.EventElectionProviderMultiPhaseElectionFinalized
// type EventElectionProviderMultiPhaseRewarded = types.EventElectionProviderMultiPhaseRewarded
// type EventElectionProviderMultiPhaseSlashed = types.EventElectionProviderMultiPhaseSlashed
// type EventElectionProviderMultiPhaseSignedPhaseStarted = types.EventElectionProviderMultiPhaseSignedPhaseStarted
// type EventElectionProviderMultiPhaseUnsignedPhaseStarted = types.EventElectionProviderMultiPhaseUnsignedPhaseStarted
// type EventGiltBidPlaced = types.EventGiltBidPlaced
// type EventGiltBidRetracted = types.EventGiltBidRetracted
// type EventGiltGiltIssued = types.EventGiltGiltIssued
// type EventGiltGiltThawed = types.EventGiltGiltThawed
// type EventParasInclusionCandidateBacked = types.EventParasInclusionCandidateBacked
// type EventParasInclusionCandidateIncluded = types.EventParasInclusionCandidateIncluded
// type EventParasInclusionCandidateTimedOut = types.EventParasInclusionCandidateTimedOut
// type EventParasCurrentCodeUpdated = types.EventParasCurrentCodeUpdated
// type EventParasCurrentHeadUpdated = types.EventParasCurrentHeadUpdated
// type EventParasCodeUpgradeScheduled = types.EventParasCodeUpgradeScheduled
// type EventParasNewHeadNoted = types.EventParasNewHeadNoted
// type EventParasActionQueued = types.EventParasActionQueued
// type EventParasUmpInvalidFormat = types.EventParasUmpInvalidFormat
// type EventParasUmpUnsupportedVersion = types.EventParasUmpUnsupportedVersion
// type EventParasUmpExecutedUpward = types.EventParasUmpExecutedUpward
// type EventParasUmpWeightExhausted = types.EventParasUmpWeightExhausted
// type EventParasUmpUpwardMessagesReceived = types.EventParasUmpUpwardMessagesReceived
// type EventParasHrmpOpenChannelRequested = types.EventParasHrmpOpenChannelRequested
// type EventParasHrmpOpenChannelAccepted = types.EventParasHrmpOpenChannelAccepted
// type EventParasHrmpChannelClosed = types.EventParasHrmpChannelClosed
// type EventRegistrarRegistered = types.EventRegistrarRegistered
// type EventRegistrarDeregistered = types.EventRegistrarDeregistered
// type EventRegistrarReserved = types.EventRegistrarReserved
// type EventSlotsNewLeasePeriod = types.EventSlotsNewLeasePeriod
// type EventSlotsLeased = types.EventSlotsLeased
// type EventAuctionsAuctionStarted = types.EventAuctionsAuctionStarted
// type EventAuctionsAuctionClosed = types.EventAuctionsAuctionClosed
// type EventAuctionsReserved = types.EventAuctionsReserved
// type EventAuctionsUnreserved = types.EventAuctionsUnreserved
// type EventAuctionsReserveConfiscated = types.EventAuctionsReserveConfiscated
// type EventAuctionsBidAccepted = types.EventAuctionsBidAccepted
// type EventAuctionsWinningOffset = types.EventAuctionsWinningOffset
// type EventCrowdloanCreated = types.EventCrowdloanCreated
// type EventCrowdloanContributed = types.EventCrowdloanContributed
// type EventCrowdloanWithdrew = types.EventCrowdloanWithdrew
// type EventCrowdloanPartiallyRefunded = types.EventCrowdloanPartiallyRefunded
// type EventCrowdloanAllRefunded = types.EventCrowdloanAllRefunded
// type EventCrowdloanDissolved = types.EventCrowdloanDissolved
// type EventCrowdloanHandleBidResult = types.EventCrowdloanHandleBidResult
// type EventCrowdloanEdited = types.EventCrowdloanEdited
// type EventCrowdloanMemoUpdated = types.EventCrowdloanMemoUpdated
// type EventCrowdloanAddedToNewRaise = types.EventCrowdloanAddedToNewRaise
// type EventXcmPalletAttempted = types.EventXcmPalletAttempted
// type EventXcmPalletSent = types.EventXcmPalletSent

type KusamaEventRecord struct {
	Society_MemberSuspended []EventSocietyMemberSuspended
	Society_Challenged      []EventSocietyChallenged
	Society_Vote            []EventSocietyVote
	Society_DefenderVote    []EventSocietyDefenderVote
	Society_NewMaxMembers   []EventSocietyNewMaxMembers
	Society_Unfounded       []EventSocietyUnfounded
	Society_Deposit         []EventSocietyDeposit
	// Recovery_RecoveryCreated   []EventRecoveryRecoveryCreated
	// Recovery_RecoveryInitiated []EventRecoveryRecoveryInitiated
	// Recovery_RecoveryVouched   []EventRecoveryRecoveryVouched
	// Recovery_RecoveryClosed    []EventRecoveryRecoveryClosed
	// Recovery_AccountRecovered  []EventRecoveryAccountRecovered
	// Recovery_RecoveryRemoved   []EventRecoveryRecoveryRemoved
	Vesting_VestingUpdated   []EventVestingVestingUpdated
	Vesting_VestingCompleted []EventVestingVestingCompleted
	Scheduler_Scheduled      []EventSchedulerScheduled
	Scheduler_Canceled       []EventSchedulerCanceled
	Scheduler_Dispatched     []EventSchedulerDispatched
	Proxy_ProxyExecuted      []EventProxyProxyExecuted
	Proxy_AnonymousCreated   []EventProxyAnonymousCreated
	Proxy_Announced          []EventProxyAnnounced
	Multisig_NewMultisig     []EventMultisigNewMultisig
	// 	Multisig_MultisigApproval                       []EventMultisigMultisigApproval
	// 	Multisig_MultisigExecuted                       []EventMultisigMultisigExecuted
	// 	Multisig_MultisigCancelled                      []EventMultisigMultisigCancelled
	// 	Bounties_BountyProposed                         []EventBountiesBountyProposed
	// 	Bounties_BountyRejected                         []EventBountiesBountyRejected
	// 	Bounties_BountyBecameActive                     []EventBountiesBountyBecameActive
	// 	Bounties_BountyAwarded                          []EventBountiesBountyAwarded
	// 	Bounties_BountyClaimed                          []EventBountiesBountyClaimed
	// 	Bounties_BountyCanceled                         []EventBountiesBountyCanceled
	// 	Bounties_BountyExtended                         []EventBountiesBountyExtended
	// 	Tips_NewTip                                     []EventTipsNewTip
	// 	Tips_TipClosing                                 []EventTipsTipClosing
	// 	Tips_TipClosed                                  []EventTipsTipClosed
	// 	Tips_TipRetracted                               []EventTipsTipRetracted
	// 	Tips_TipSlashed                                 []EventTipsTipSlashed
	// 	ElectionProviderMultiPhase_SolutionStored       []EventElectionProviderMultiPhaseSolutionStored
	// 	ElectionProviderMultiPhase_ElectionFinalized    []EventElectionProviderMultiPhaseElectionFinalized
	// 	ElectionProviderMultiPhase_Rewarded             []EventElectionProviderMultiPhaseRewarded
	// 	ElectionProviderMultiPhase_Slashed              []EventElectionProviderMultiPhaseSlashed
	// 	ElectionProviderMultiPhase_SignedPhaseStarted   []EventElectionProviderMultiPhaseSignedPhaseStarted
	// 	ElectionProviderMultiPhase_UnsignedPhaseStarted []EventElectionProviderMultiPhaseUnsignedPhaseStarted
	// 	Gilt_BidPlaced                                  []EventGiltBidPlaced
	// 	Gilt_BidRetracted                               []EventGiltBidRetracted
	// 	Gilt_GiltIssued                                 []EventGiltGiltIssued
	// 	Gilt_GiltThawed                                 []EventGiltGiltThawed
	// 	ParasInclusion_CandidateBacked                  []EventParasInclusionCandidateBacked
	// 	ParasInclusion_CandidateIncluded                []EventParasInclusionCandidateIncluded
	// 	ParasInclusion_CandidateTimedOut                []EventParasInclusionCandidateTimedOut
	// 	Paras_CurrentCodeUpdated                        []EventParasCurrentCodeUpdated
	// 	Paras_CurrentHeadUpdated                        []EventParasCurrentHeadUpdated
	// 	Paras_CodeUpgradeScheduled                      []EventParasCodeUpgradeScheduled
	// 	Paras_NewHeadNoted                              []EventParasNewHeadNoted
	// 	Paras_ActionQueued                              []EventParasActionQueued
	// 	ParasUmp_InvalidFormat                          []EventParasUmpInvalidFormat
	// 	ParasUmp_UnsupportedVersion                     []EventParasUmpUnsupportedVersion
	// 	ParasUmp_ExecutedUpward                         []EventParasUmpExecutedUpward
	// 	ParasUmp_WeightExhausted                        []EventParasUmpWeightExhausted
	// 	ParasUmp_UpwardMessagesReceived                 []EventParasUmpUpwardMessagesReceived
	// 	ParasHrmp_OpenChannelRequested                  []EventParasHrmpOpenChannelRequested
	// 	ParasHrmp_OpenChannelAccepted                   []EventParasHrmpOpenChannelAccepted
	// 	ParasHrmp_ChannelClosed                         []EventParasHrmpChannelClosed
	// 	Registrar_Registered                            []EventRegistrarRegistered
	// 	Registrar_Deregistered                          []EventRegistrarDeregistered
	// 	Registrar_Reserved                              []EventRegistrarReserved
	// 	Slots_NewLeasePeriod                            []EventSlotsNewLeasePeriod
	// 	Slots_Leased                                    []EventSlotsLeased
	// 	Auctions_AuctionStarted                         []EventAuctionsAuctionStarted
	// 	Auctions_AuctionClosed                          []EventAuctionsAuctionClosed
	// 	Auctions_Reserved                               []EventAuctionsReserved
	// 	Auctions_Unreserved                             []EventAuctionsUnreserved
	// 	Auctions_ReserveConfiscated                     []EventAuctionsReserveConfiscated
	// 	Auctions_BidAccepted                            []EventAuctionsBidAccepted
	// 	Auctions_WinningOffset                          []EventAuctionsWinningOffset
	// 	Crowdloan_Created                               []EventCrowdloanCreated
	// 	Crowdloan_Contributed                           []EventCrowdloanContributed
	// 	Crowdloan_Withdrew                              []EventCrowdloanWithdrew
	// 	Crowdloan_PartiallyRefunded                     []EventCrowdloanPartiallyRefunded
	// 	Crowdloan_AllRefunded                           []EventCrowdloanAllRefunded
	// 	Crowdloan_Dissolved                             []EventCrowdloanDissolved
	// 	Crowdloan_HandleBidResult                       []EventCrowdloanHandleBidResult
	// 	Crowdloan_Edited                                []EventCrowdloanEdited
	// 	Crowdloan_MemoUpdated                           []EventCrowdloanMemoUpdated
	// 	Crowdloan_AddedToNewRaise                       []EventCrowdloanAddedToNewRaise
	// 	XcmPallet_Attempted                             []EventXcmPalletAttempted
	// 	XcmPallet_Sent                                  []EventXcmPalletSent
}
