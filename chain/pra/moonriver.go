package pra

import "github.com/centrifuge/go-substrate-rpc-client/v3/types"

type EventCouncilCollectiveApproved = types.EventCollectiveApproved
type EventCouncilCollectiveClosed = types.EventCollectiveClosed
type EventCouncilCollectiveDisapproved = types.EventCollectiveDisapproved
type EventCouncilCollectiveExecuted = types.EventCollectiveExecuted
type EventCouncilCollectiveMemberExecuted = types.EventCollectiveMemberExecuted
type EventCouncilCollectiveProposed = types.EventCollectiveProposed
type EventCouncilCollectiveVoted = types.EventCollectiveVoted

type EventTechComitteeCollectiveApproved = types.EventTechnicalCommitteeApproved
type EventTechComitteeCollectiveClosed = types.EventTechnicalCommitteeClosed
type EventTechComitteeCollectiveDisapproved = types.EventTechnicalCommitteeDisapproved
type EventTechComitteeCollectiveExecuted = types.EventTechnicalCommitteeExecuted
type EventTechComitteeCollectiveMemberExecuted = types.EventTechnicalCommitteeMemberExecuted
type EventTechComitteeCollectiveProposed = types.EventTechnicalCommitteeProposed
type EventTechComitteeCollectiveVoted = types.EventTechnicalCommitteeVoted

type RoundIndex types.U32
type AuthorID types.AccountID
type EventAuthorFilterEligibleUpdated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventAuthorMappingAuthorDeRegistered struct {
	Phase    types.Phase
	AuthorId AuthorID
	Topics   []types.Hash
}
type EventAuthorMappingAuthorRegistered struct {
	Phase     types.Phase
	AuthorId  AuthorID
	AccountId types.AccountID
	Topics    []types.Hash
}
type EventAuthorMappingAuthorRotated struct {
	Phase     types.Phase
	AuthorId  AuthorID
	AccountId types.AccountID
	Topics    []types.Hash
}
type EventAuthorMappingDefunctAuthorBusted struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsInitialPaymentMade struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsInitializedAccountWithNotEnoughContribution struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsInitializedAlreadyInitializedAccount struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsNativeIdentityAssociated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsRewardAddressUpdated struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventCrowdloanRewardsRewardsPaid struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingBlocksPerRoundSet struct {
	Phase        types.Phase
	CurrentRound RoundIndex
	FirstBlock   BlockNumber
	Old          types.U32
	New          types.U32
	Topics       []types.Hash
}
type EventParachainStakingCollatorBackOnline struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorBondedLess struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorBondedMore struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorChosen struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorCommissionSet struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorLeft struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorScheduledExit struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingCollatorWentOffline struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingInflationSet struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingJoinedCollatorCandidates struct {
	Phase             types.Phase
	Account           types.AccountID
	AmountLocked      types.U128
	NewTotalAmtLocked types.U128
	Topics            []types.Hash
}
type EventParachainStakingNewRound struct {
	Phase                     types.Phase
	StartBlock                BlockNumber
	Round                     RoundIndex
	NumberOfCollatorsSelected types.U32
	TotalBalance              types.U128
	Topics                    []types.Hash
}
type EventParachainStakingNomination struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingNominationDecreased struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingNominationIncreased struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingNominatorLeft struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingNominatorLeftCollator struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingParachainBondAccountSet struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingParachainBondReservePercentSet struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingReservedForParachainBond struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingRewarded struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingStakeExpectationsSet struct {
	Phase  types.Phase
	Topics []types.Hash
}
type EventParachainStakingTotalSelectedSet struct {
	Phase  types.Phase
	Old    types.U32
	New    types.U32
	Topics []types.Hash
}

type EventParachainSystemDownwardMessagesProcessed struct {
	Phase         types.Phase
	WeightUsed    types.U64
	ResultMqcHead types.Hash
	Topics        []types.Hash
}
type EventParachainSystemDownwardMessagesReceived struct {
	Phase  types.Phase
	Count  types.U32
	Topics []types.Hash
}
type EventParachainSystemUpgradeAuthorized struct {
	Phase  types.Phase
	Hash   types.Hash
	Topics []types.Hash
}
type EventParachainSystemValidationFunctionApplied struct {
	Phase                 types.Phase
	RelayChainBlockNumber BlockNumber
	Topics                []types.Hash
}
type EventParachainSystemValidationFunctionStored struct {
	Phase                 types.Phase
	RelayChainBlockNumber BlockNumber
	Topics                []types.Hash
}

type MoonRiverEventRecord struct {
	AuthorFilter_EligibleUpdated                                 []EventAuthorFilterEligibleUpdated
	AuthorMapping_AuthorDeRegistered                             []EventAuthorMappingAuthorDeRegistered
	AuthorMapping_AuthorRegistered                               []EventAuthorMappingAuthorRegistered
	AuthorMapping_AuthorRotated                                  []EventAuthorMappingAuthorRotated
	AuthorMapping_DefunctAuthorBusted                            []EventAuthorMappingDefunctAuthorBusted
	Balances_BalanceSet                                          types.EventBalancesBalanceSet
	Balances_Deposit                                             types.EventBalancesDeposit
	Balances_DustLost                                            types.EventBalancesDustLost
	Balances_Endowed                                             types.EventBalancesEndowed
	Balances_ReserveRepatriated                                  types.EventBalancesReserveRepatriated
	Balances_Reserved                                            types.EventBalancesReserved
	Balances_Transfer                                            types.EventBalancesTransfer
	Balances_Unreserved                                          types.EventBalancesUnreserved
	CouncilCollective_Approved                                   EventCouncilCollectiveApproved
	CouncilCollective_Closed                                     EventCouncilCollectiveClosed
	CouncilCollective_Disapproved                                EventCouncilCollectiveDisapproved
	CouncilCollective_Executed                                   EventCouncilCollectiveExecuted
	CouncilCollective_MemberExecuted                             EventCouncilCollectiveMemberExecuted
	CouncilCollective_Proposed                                   EventCouncilCollectiveProposed
	CouncilCollective_Voted                                      EventCouncilCollectiveVoted
	CrowdloanRewards_InitialPaymentMade                          []EventCrowdloanRewardsInitialPaymentMade
	CrowdloanRewards_InitializedAccountWithNotEnoughContribution []EventCrowdloanRewardsInitializedAccountWithNotEnoughContribution
	CrowdloanRewards_InitializedAlreadyInitializedAccount        []EventCrowdloanRewardsInitializedAlreadyInitializedAccount
	CrowdloanRewards_NativeIdentityAssociated                    []EventCrowdloanRewardsNativeIdentityAssociated
	CrowdloanRewards_RewardAddressUpdated                        []EventCrowdloanRewardsRewardAddressUpdated
	CrowdloanRewards_RewardsPaid                                 []EventCrowdloanRewardsRewardsPaid
	Democracy_Blacklisted                                        types.EventDemocracyBlacklisted
	Democracy_Cancelled                                          types.EventDemocracyCancelled
	Democracy_Delegated                                          types.EventDemocracyDelegated
	Democracy_Executed                                           types.EventDemocracyExecuted
	Democracy_ExternalTabled                                     types.EventDemocracyExternalTabled
	Democracy_NotPassed                                          types.EventDemocracyNotPassed
	Democracy_Passed                                             types.EventDemocracyPassed
	Democracy_PreimageInvalid                                    types.EventDemocracyPreimageInvalid
	Democracy_PreimageMissing                                    types.EventDemocracyPreimageMissing
	Democracy_PreimageNoted                                      types.EventDemocracyPreimageNoted
	Democracy_PreimageReaped                                     types.EventDemocracyPreimageReaped
	Democracy_PreimageUsed                                       types.EventDemocracyPreimageUsed
	Democracy_Proposed                                           types.EventDemocracyProposed
	Democracy_Started                                            types.EventDemocracyStarted
	Democracy_Tabled                                             types.EventDemocracyTabled
	Democracy_Undelegated                                        types.EventDemocracyUndelegated
	Democracy_Unlocked                                           types.EventDemocracyUnlocked
	Democracy_Vetoed                                             types.EventDemocracyVetoed
	FrontierEventRecord
	// EVM_BalanceDeposit
	// EVM_BalanceWithdraw
	// EVM_Created
	// EVM_CreatedFailed
	// EVM_Executed
	// EVM_ExecutedFailed
	// EVM_Log
	// Ethereum_Executed
	ParachainStaking_BlocksPerRoundSet              []EventParachainStakingBlocksPerRoundSet
	ParachainStaking_CollatorBackOnline             []EventParachainStakingCollatorBackOnline
	ParachainStaking_CollatorBondedLess             []EventParachainStakingCollatorBondedLess
	ParachainStaking_CollatorBondedMore             []EventParachainStakingCollatorBondedMore
	ParachainStaking_CollatorChosen                 []EventParachainStakingCollatorChosen
	ParachainStaking_CollatorCommissionSet          []EventParachainStakingCollatorCommissionSet
	ParachainStaking_CollatorLeft                   []EventParachainStakingCollatorLeft
	ParachainStaking_CollatorScheduledExit          []EventParachainStakingCollatorScheduledExit
	ParachainStaking_CollatorWentOffline            []EventParachainStakingCollatorWentOffline
	ParachainStaking_InflationSet                   []EventParachainStakingInflationSet
	ParachainStaking_JoinedCollatorCandidates       []EventParachainStakingJoinedCollatorCandidates
	ParachainStaking_NewRound                       []EventParachainStakingNewRound
	ParachainStaking_Nomination                     []EventParachainStakingNomination
	ParachainStaking_NominationDecreased            []EventParachainStakingNominationDecreased
	ParachainStaking_NominationIncreased            []EventParachainStakingNominationIncreased
	ParachainStaking_NominatorLeft                  []EventParachainStakingNominatorLeft
	ParachainStaking_NominatorLeftCollator          []EventParachainStakingNominatorLeftCollator
	ParachainStaking_ParachainBondAccountSet        []EventParachainStakingParachainBondAccountSet
	ParachainStaking_ParachainBondReservePercentSet []EventParachainStakingParachainBondReservePercentSet
	ParachainStaking_ReservedForParachainBond       []EventParachainStakingReservedForParachainBond
	ParachainStaking_Rewarded                       []EventParachainStakingRewarded
	ParachainStaking_StakeExpectationsSet           []EventParachainStakingStakeExpectationsSet
	ParachainStaking_TotalSelectedSet               []EventParachainStakingTotalSelectedSet
	ParachainSystem_DownwardMessagesProcessed       []EventParachainSystemDownwardMessagesProcessed
	ParachainSystem_DownwardMessagesReceived        []EventParachainSystemDownwardMessagesReceived
	ParachainSystem_UpgradeAuthorized               []EventParachainSystemUpgradeAuthorized
	ParachainSystem_ValidationFunctionApplied       []EventParachainSystemValidationFunctionApplied
	ParachainSystem_ValidationFunctionStored        []EventParachainSystemValidationFunctionStored
	Proxy_Announced                                 types.EventProxyAnnounced
	Proxy_AnonymousCreated                          types.EventProxyAnonymousCreated
	Proxy_ProxyExecuted                             types.EventProxyProxyExecuted
	Scheduler_Canceled                              types.EventSchedulerCanceled
	Scheduler_Dispatched                            types.EventSchedulerDispatched
	Scheduler_Scheduled                             types.EventSchedulerScheduled
	Sudo_KeyChanged                                 types.EventSudoKeyChanged
	Sudo_Sudid                                      types.EventSudoSudid
	Sudo_SudoAsDone                                 types.EventSudoAsDone
	System_CodeUpdated                              types.EventSystemCodeUpdated
	System_ExtrinsicFailed                          types.EventSystemExtrinsicFailed
	System_ExtrinsicSuccess                         types.EventSystemExtrinsicSuccess
	System_KilledAccount                            types.EventSystemKilledAccount
	System_NewAccount                               types.EventSystemNewAccount
	System_Remarked                                 EventSystemRemarked
	TechComitteeCollective_Approved                 EventTechComitteeCollectiveApproved
	TechComitteeCollective_Closed                   EventTechComitteeCollectiveClosed
	TechComitteeCollective_Disapproved              EventTechComitteeCollectiveDisapproved
	TechComitteeCollective_Executed                 EventTechComitteeCollectiveExecuted
	TechComitteeCollective_MemberExecuted           EventTechComitteeCollectiveMemberExecuted
	TechComitteeCollective_Proposed                 EventTechComitteeCollectiveProposed
	TechComitteeCollective_Voted                    EventTechComitteeCollectiveVoted
	Treasury_Awarded                                types.EventTreasuryAwarded
	Treasury_Burnt                                  types.EventTreasuryBurnt
	Treasury_Deposit                                types.EventTreasuryDeposit
	Treasury_Proposed                               types.EventTreasuryProposed
	Treasury_Rejected                               types.EventTreasuryRejected
	Treasury_Rollover                               types.EventTreasuryRollover
	Treasury_Spending                               types.EventTreasurySpending
	Utility_BatchCompleted                          types.EventUtilityBatchCompleted
	Utility_BatchInterrupted                        types.EventUtilityBatchInterrupted
}
