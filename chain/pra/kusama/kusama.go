package kusama

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain/pra/relaychain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
)

type Balance = types.U128
type Phase = types.Phase
type U8 = types.U8
type U16 = types.U16
type U32 = types.U32
type U64 = types.U64
type U128 = types.U128
type H160 = types.H160
type Bytes = types.Bytes
type Bytes16 = types.Bytes16
type Hash = types.Hash
type AccountID = types.AccountID
type AccountIndex = types.AccountIndex
type Weight = types.Weight
type ParaId = types.U32
type DispatchResult = []byte
type DispatchError = []byte
type ProposalIndex = types.U32
type MemberCount = types.U32
type Bool = types.Bool
type EventIndicesIndexAssigned struct {
	Phase  Phase
	Who    AccountID
	Index  AccountIndex
	Topics []Hash
}

type EventIncidesIndexFreed struct {
	Phase  Phase
	Index  AccountIndex
	Topics []Hash
}

type EventIncidesIndexFrozen struct {
	Phase  Phase
	Index  AccountIndex
	Who    AccountID
	Topics []Hash
}

// EventSystemRemarked is emitted on on-chain remark happened.
type EventSystemRemarked struct {
	Phase      Phase
	Origin     AccountID
	RemarkHash Hash
	Topics     []Hash
}

type EventOffencesOffence = types.EventOffencesOffence
type EventSocietyMemberSuspended = types.EventSocietyMemberSuspended
type EventSocietyChallenged = types.EventSocietyChallenged
type EventSocietyVote = types.EventSocietyVote
type EventSocietyDefenderVote = types.EventSocietyDefenderVote
type EventSocietyNewMaxMembers = types.EventSocietyNewMaxMembers
type EventSocietyUnfounded = types.EventSocietyUnfounded
type EventSocietyDeposit = types.EventSocietyDeposit
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
type EventBalancesBalanceSet = types.EventBalancesBalanceSet
type EventBalancesDeposit = types.EventBalancesDeposit
type EventBalancesDustLost = types.EventBalancesDustLost
type EventBalancesEndowed = types.EventBalancesEndowed
type EventBalancesReserveRepatriated = types.EventBalancesReserveRepatriated
type EventBalancesReserved = types.EventBalancesReserved
type EventBalancesTransfer = types.EventBalancesTransfer
type EventBalancesUnreserved = types.EventBalancesUnreserved
type EventImOnlineHeartbeatReceived = types.EventImOnlineHeartbeatReceived
type EventImOnlineAllGood = types.EventImOnlineAllGood
type EventImOnlineSomeOffline = types.EventImOnlineSomeOffline
type EventTreasuryAwarded = types.EventTreasuryAwarded
type EventTreasuryBurnt = types.EventTreasuryBurnt
type EventTreasuryDeposit = types.EventTreasuryDeposit
type EventTreasuryProposed = types.EventTreasuryProposed
type EventTreasuryRejected = types.EventTreasuryRejected
type EventTreasuryRollover = types.EventTreasuryRollover
type EventTreasurySpending = types.EventTreasurySpending
type EventSessionNewSession = types.EventSessionNewSession
type EventSystemCodeUpdated = types.EventSystemCodeUpdated
type EventSystemExtrinsicFailed = types.EventSystemExtrinsicFailed
type EventSystemExtrinsicSuccess = types.EventSystemExtrinsicSuccess
type EventSystemKilledAccount = types.EventSystemKilledAccount
type EventSystemNewAccount = types.EventSystemNewAccount
type EventIdentitySet = types.EventIdentitySet
type EventIdentityCleared = types.EventIdentityCleared
type EventIdentityKilled = types.EventIdentityKilled
type EventIdentityJudgementRequested = types.EventIdentityJudgementRequested
type EventIdentityJudgementUnrequested = types.EventIdentityJudgementUnrequested
type EventIdentityJudgementGiven = types.EventIdentityJudgementGiven
type EventIdentityRegistrarAdded = types.EventIdentityRegistrarAdded
type EventIdentitySubIdentityAdded = types.EventIdentitySubIdentityAdded
type EventIdentitySubIdentityRemoved = types.EventIdentitySubIdentityRemoved
type EventIdentitySubIdentityRevoked = types.EventIdentitySubIdentityRevoked
type EventTechComitteeCollectiveApproved = types.EventTechnicalCommitteeApproved
type EventTechComitteeCollectiveClosed = types.EventTechnicalCommitteeClosed
type EventTechComitteeCollectiveDisapproved = types.EventTechnicalCommitteeDisapproved
type EventTechComitteeCollectiveExecuted = types.EventTechnicalCommitteeExecuted
type EventTechComitteeCollectiveMemberExecuted = types.EventTechnicalCommitteeMemberExecuted
type EventTechComitteeCollectiveProposed = types.EventTechnicalCommitteeProposed
type EventTechComitteeCollectiveVoted = types.EventTechnicalCommitteeVoted

type EventCouncilProposed struct {
	Phase         Phase
	Account       AccountID
	ProposalIndex ProposalIndex
	ProposalHash  Hash
	ThresHold     MemberCount
	Topics        []Hash
}

type EventCouncilVoted struct {
	Phase        Phase
	Account      AccountID
	ProposalHash Hash
	Voted        Bool
	Yes          MemberCount
	No           MemberCount
	Topics       []Hash
}

type EventCouncilApproved struct {
	Phase        Phase
	ProposalHash Hash
	Topics       []Hash
}

type EventCouncilDisapproved struct {
	Phase        Phase
	ProposalHash Hash
	Topics       []Hash
}

type EventCouncilExecuted struct {
	Phase        Phase
	ProposalHash Hash
	Result       DispatchResult
	Topics       []Hash
}

type EventCouncilMemberExecuted struct {
	Phase        Phase
	ProposalHash Hash
	Result       DispatchResult
	Topics       []Hash
}

type EventCouncilClosed struct {
	Phase        Phase
	ProposalHash Hash
	Yes          MemberCount
	No           MemberCount
	Topics       []Hash
}

// EventStakingEraPayout is emitted when the era payout has been set;
type EventStakingEraPayout struct {
	Phase           Phase
	EraIndex        U32
	ValidatorPayout U128
	Remainder       U128
	Topics          []Hash
}

// EventStakingReward is emitted when the staker has been rewarded by this amount.
type EventStakingReward struct {
	Phase  Phase
	Stash  AccountID
	Amount U128
	Topics []Hash
}

// EventStakingSlash is emitted when one validator (and its nominators) has been slashed by the given amount
type EventStakingSlash struct {
	Phase     Phase
	AccountID AccountID
	Balance   U128
	Topics    []Hash
}

// EventStakingOldSlashingReportDiscarded is emitted when an old slashing report from a prior era was discarded because
// it could not be processed
type EventStakingOldSlashingReportDiscarded struct {
	Phase        Phase
	SessionIndex U32
	Topics       []Hash
}

// EventStakingStakingElection is emitted when a new set of stakers was elected with the given
type EventStakingStakingElection struct {
	Phase  Phase
	Topics []Hash
}

// EventStakingBonded is emitted when an account has bonded this amount
type EventStakingBonded struct {
	Phase  Phase
	Stash  AccountID
	Amount U128
	Topics []Hash
}

// EventStakingUnbonded is emitted when an account has unbonded this amount
type EventStakingUnbonded struct {
	Phase  Phase
	Stash  AccountID
	Amount U128
	Topics []Hash
}

// EventStakingWithdrawn is emitted when an account has called `withdraw_unbonded` and removed unbonding chunks
// worth `Balance` from the unlocking queue.
type EventStakingWithdrawn struct {
	Phase  Phase
	Stash  AccountID
	Amount U128
	Topics []Hash
}

// EventStakingKicked is emitted when a nominator has been kicked from a validator
type EventStakingKicked struct {
	Phase     Phase
	Nominator AccountID
	Stash     AccountID
	Topics    []Hash
}

type EventStakingChilled struct {
	Phase  Phase
	Stash  AccountID
	Topics []Hash
}

type EventCrowdloanCreated struct {
	Phase   Phase
	Created ParaId
	Topics  []Hash
}
type EventCrowdloanContributed struct {
	Phase     Phase
	AccountId AccountID
	ParaId    ParaId
	Balance   Balance
	Topics    []Hash
}
type EventCrowdloanWithdrew struct {
	Phase     Phase
	AccountID AccountID
	ParaId    ParaId
	Balance   Balance
	Topics    []Hash
}
type EventCrowdloanPartiallyRefunded struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}
type EventCrowdloanAllRefunded struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}
type EventCrowdloanDissolved struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}

type EventCrowdloanDeployDataFixed struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}

type EventCrowdloanOnboarded struct {
	Phase     Phase
	FindIndex ParaId
	ParaId    ParaId
	Topics    []Hash
}

type EventCrowdloanHandleBidResult struct {
	Phase          Phase
	ParaId         ParaId
	DispatchResult DispatchResult
	Topics         []Hash
}
type EventCrowdloanEdited struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}
type EventCrowdloanMemoUpdated struct {
	Phase     Phase
	Who       AccountID
	FundIndex ParaId
	Memo      Bytes
	Topics    []Hash
}
type EventCrowdloanAddedToNewRaise struct {
	Phase  Phase
	ParaId ParaId
	Topics []Hash
}

type EventUtilityBatchInterrupted struct {
	Phase  Phase
	Index  types.U32
	Error  DispatchError
	Topics []Hash
}

type EventUtilityBatchCompleted struct {
	Phase  Phase
	Topics []Hash
}

type XcmErrorEnum byte

const (
	Undefined XcmErrorEnum = iota
	Overflow
	Unimplemented
	UnhandledXcmVersion
	UnhandledXcmMessage
	UnhandledEffect
	EscalationOfPrivilege
	UntrustedReserveLocation
	UntrustedTeleportLocation
	DestinationBufferOverflow
	SendFailed
	CannotReachDestination
	MultiLocationFull
	FailedToDecode
	BadOrigin
	ExceedsMaxMessageSize
	FailedToTransactAsset
	WeightLimitReached
	Wildcard
	TooMuchWeightRequired
	NotHoldingFees
	WeightNotComputable
	Barrier
	NotWithdrawable
	LocationCannotHold
	TooExpensive
)

func (e *XcmError) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	switch b {
	case 0:
		e.IsUndefined = true
	case 1:
		e.IsOverflow = true
	case 2:
		e.IsUnimplemented = true
	case 3:
		e.IsUnhandledXcmVersion = true
	case 4:
		e.IsUnhandledXcmMessage = true
	case 5:
		e.IsUnhandledEffect = true
	case 6:
		e.IsEscalationOfPrivilege = true
	case 7:
		e.IsUntrustedReserveLocation = true
	case 8:
		e.IsUntrustedTeleportLocation = true
	case 9:
		e.IsDestinationBufferOverflow = true
	case 10:
		e.IsSendFailed = true
	case 11:
		e.IsCannotReachDestination = true
		// err := decoder.Decoder(&e.AsCannotReachDestination) // FIXME
	case 12:
		e.IsMultiLocationFull = true
	case 13:
		e.IsFailedToDecode = true
	case 14:
		e.IsBadOrigin = true
	case 15:
		e.IsExceedsMaxMessageSize = true
	case 16:
		e.IsFailedToTransactAsset = true
	case 17:
		e.IsWeightLimitReached = true
		err = decoder.Decode(&e.AsWeightLimitReached)
	case 18:
		e.IsWildcard = true
	case 19:
		e.IsTooMuchWeightRequired = true
	case 20:
		e.IsNotHoldingFees = true
	case 21:
		e.IsWeightNotComputable = true
	case 22:
		e.IsBarrier = true
	case 23:
		e.IsNotWithdrawable = true
	case 24:
		e.IsLocationCannotHold = true
	case 25:
		e.IsTooExpensive = true
	}

	if err != nil {
		return err
	}

	return nil
}

type XcmError struct {
	IsUndefined                 bool
	IsOverflow                  bool
	IsUnimplemented             bool
	IsUnhandledXcmVersion       bool
	IsUnhandledXcmMessage       bool
	IsUnhandledEffect           bool
	IsEscalationOfPrivilege     bool
	IsUntrustedReserveLocation  bool
	IsUntrustedTeleportLocation bool
	IsDestinationBufferOverflow bool
	IsSendFailed                bool
	IsCannotReachDestination    bool
	AsCannotReachDestination    []byte // FIXME
	IsMultiLocationFull         bool
	IsFailedToDecode            bool
	IsBadOrigin                 bool
	IsExceedsMaxMessageSize     bool
	IsFailedToTransactAsset     bool
	IsWeightLimitReached        bool
	AsWeightLimitReached        Weight
	IsWildcard                  bool
	IsTooMuchWeightRequired     bool
	IsNotHoldingFees            bool
	IsWeightNotComputable       bool
	IsBarrier                   bool
	IsNotWithdrawable           bool
	IsLocationCannotHold        bool
	IsTooExpensive              bool
}

type OutcomeEnum byte

const (
	Complete OutcomeEnum = iota
	Incomplete
	Error
)

type WeightXcmErrorTuple struct {
	Weight   Weight
	XcmError XcmError
}

type Outcome struct {
	IsComplete   bool
	AsComplete   Weight
	IsIncomplete bool
	AsIncomplete WeightXcmErrorTuple
	IsError      bool
	AsError      XcmError
}

func (o *Outcome) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	switch b {
	case 0:
		o.IsComplete = true
		err = decoder.Decode(&o.AsComplete)
	case 1:
		o.IsIncomplete = true
		err = decoder.Decode(&o.AsIncomplete)
	case 2:
		o.IsError = true
		err = decoder.Decode(&o.AsError)
	}

	if err != nil {
		return err
	}

	return nil
}

type EventXcmPalletAttempted struct {
	Phase   types.Phase
	Outcome Outcome
	Topics  []types.Hash
}

// type EventXcmPalletSent struct {
// 	Phase         types.Phase
// 	MultiLocation MultiLocation
// 	MultiLocation MultiLocation
// 	// Xcm
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
	Indices_IndexAssigned                 []EventIndicesIndexAssigned
	Indices_IndexFreed                    []EventIncidesIndexFreed
	Indices_IndexFrozen                   []EventIncidesIndexFrozen
	Balances_BalanceSet                   []EventBalancesBalanceSet
	Balances_Deposit                      []EventBalancesDeposit
	Balances_DustLost                     []EventBalancesDustLost
	Balances_Endowed                      []EventBalancesEndowed
	Balances_ReserveRepatriated           []EventBalancesReserveRepatriated
	Balances_Reserved                     []EventBalancesReserved
	Balances_Transfer                     []EventBalancesTransfer
	Balances_Unreserved                   []EventBalancesUnreserved
	Offences_Offence                      []EventOffencesOffence
	Council_Proposed                      []EventCouncilProposed
	Council_Voted                         []EventCouncilVoted
	Council_Approved                      []EventCouncilApproved
	Council_Disapproved                   []EventCouncilDisapproved
	Council_Executed                      []EventCouncilExecuted
	Council_MemberExecuted                []EventCouncilMemberExecuted
	Council_Closed                        []EventCouncilClosed
	TechComitteeCollective_Approved       []EventTechComitteeCollectiveApproved
	TechComitteeCollective_Closed         []EventTechComitteeCollectiveClosed
	TechComitteeCollective_Disapproved    []EventTechComitteeCollectiveDisapproved
	TechComitteeCollective_Executed       []EventTechComitteeCollectiveExecuted
	TechComitteeCollective_MemberExecuted []EventTechComitteeCollectiveMemberExecuted
	TechComitteeCollective_Proposed       []EventTechComitteeCollectiveProposed
	TechComitteeCollective_Voted          []EventTechComitteeCollectiveVoted
	Society_MemberSuspended               []EventSocietyMemberSuspended
	Society_Challenged                    []EventSocietyChallenged
	Society_Vote                          []EventSocietyVote
	Society_DefenderVote                  []EventSocietyDefenderVote
	Society_NewMaxMembers                 []EventSocietyNewMaxMembers
	Society_Unfounded                     []EventSocietyUnfounded
	Society_Deposit                       []EventSocietyDeposit
	// Recovery_RecoveryCreated    []EventRecoveryRecoveryCreated
	// Recovery_RecoveryInitiated  []EventRecoveryRecoveryInitiated
	// Recovery_RecoveryVouched    []EventRecoveryRecoveryVouched
	// Recovery_RecoveryClosed     []EventRecoveryRecoveryClosed
	// Recovery_AccountRecovered   []EventRecoveryAccountRecovered
	// Recovery_RecoveryRemoved    []EventRecoveryRecoveryRemoved
	Vesting_VestingUpdated     []EventVestingVestingUpdated
	Vesting_VestingCompleted   []EventVestingVestingCompleted
	Scheduler_Scheduled        []EventSchedulerScheduled
	Scheduler_Canceled         []EventSchedulerCanceled
	Scheduler_Dispatched       []EventSchedulerDispatched
	Proxy_ProxyExecuted        []EventProxyProxyExecuted
	Proxy_AnonymousCreated     []EventProxyAnonymousCreated
	Proxy_Announced            []EventProxyAnnounced
	Multisig_NewMultisig       []EventMultisigNewMultisig
	Multisig_MultisigApproval  []EventMultisigMultisigApproval
	Multisig_MultisigExecuted  []EventMultisigMultisigExecuted
	Multisig_MultisigCancelled []EventMultisigMultisigCancelled
	// Bounties_BountyProposed                         []EventBountiesBountyProposed
	// Bounties_BountyRejected                         []EventBountiesBountyRejected
	// Bounties_BountyBecameActive                     []EventBountiesBountyBecameActive
	// Bounties_BountyAwarded                          []EventBountiesBountyAwarded
	// Bounties_BountyClaimed                          []EventBountiesBountyClaimed
	// Bounties_BountyCanceled                         []EventBountiesBountyCanceled
	// Bounties_BountyExtended                         []EventBountiesBountyExtended
	// Tips_NewTip                                     []EventTipsNewTip
	// Tips_TipClosing                                 []EventTipsTipClosing
	// Tips_TipClosed                                  []EventTipsTipClosed
	// Tips_TipRetracted                               []EventTipsTipRetracted
	// Tips_TipSlashed                                 []EventTipsTipSlashed
	// ElectionProviderMultiPhase_SolutionStored       []EventElectionProviderMultiPhaseSolutionStored
	// ElectionProviderMultiPhase_ElectionFinalized    []EventElectionProviderMultiPhaseElectionFinalized
	// ElectionProviderMultiPhase_Rewarded             []EventElectionProviderMultiPhaseRewarded
	// ElectionProviderMultiPhase_Slashed              []EventElectionProviderMultiPhaseSlashed
	// ElectionProviderMultiPhase_SignedPhaseStarted   []EventElectionProviderMultiPhaseSignedPhaseStarted
	// ElectionProviderMultiPhase_UnsignedPhaseStarted []EventElectionProviderMultiPhaseUnsignedPhaseStarted
	// Gilt_BidPlaced                                  []EventGiltBidPlaced
	// Gilt_BidRetracted                               []EventGiltBidRetracted
	// Gilt_GiltIssued                                 []EventGiltGiltIssued
	// Gilt_GiltThawed                                 []EventGiltGiltThawed
	// Paras_CurrentCodeUpdated                        []EventParasCurrentCodeUpdated
	// Paras_CurrentHeadUpdated                        []EventParasCurrentHeadUpdated
	// Paras_CodeUpgradeScheduled                      []EventParasCodeUpgradeScheduled
	// Paras_NewHeadNoted                              []EventParasNewHeadNoted
	// Paras_ActionQueued                              []EventParasActionQueued
	// ParasUmp_InvalidFormat                          []EventParasUmpInvalidFormat
	// ParasUmp_UnsupportedVersion                     []EventParasUmpUnsupportedVersion
	// ParasUmp_ExecutedUpward                         []EventParasUmpExecutedUpward
	// ParasUmp_WeightExhausted                        []EventParasUmpWeightExhausted
	// ParasUmp_UpwardMessagesReceived                 []EventParasUmpUpwardMessagesReceived
	// ParasHrmp_OpenChannelRequested                  []EventParasHrmpOpenChannelRequested
	// ParasHrmp_OpenChannelAccepted                   []EventParasHrmpOpenChannelAccepted
	// ParasHrmp_ChannelClosed                         []EventParasHrmpChannelClosed
	// Registrar_Registered                            []EventRegistrarRegistered
	// Registrar_Deregistered                          []EventRegistrarDeregistered
	// Registrar_Reserved                              []EventRegistrarReserved
	// Slots_NewLeasePeriod                            []EventSlotsNewLeasePeriod
	// Slots_Leased                                    []EventSlotsLeased
	// Auctions_AuctionStarted                         []EventAuctionsAuctionStarted
	// Auctions_AuctionClosed                          []EventAuctionsAuctionClosed
	// Auctions_Reserved                               []EventAuctionsReserved
	// Auctions_Unreserved                             []EventAuctionsUnreserved
	// Auctions_ReserveConfiscated                     []EventAuctionsReserveConfiscated
	// Auctions_BidAccepted                            []EventAuctionsBidAccepted
	// Auctions_WinningOffset                          []EventAuctionsWinningOffset
	Crowdloan_Created                  []EventCrowdloanCreated
	Crowdloan_Contributed              []EventCrowdloanContributed
	Crowdloan_Withdrew                 []EventCrowdloanWithdrew
	Crowdloan_PartiallyRefunded        []EventCrowdloanPartiallyRefunded
	Crowdloan_AllRefunded              []EventCrowdloanAllRefunded
	Crowdloan_Dissolved                []EventCrowdloanDissolved
	Crowdloan_DeployDataFixed          []EventCrowdloanDeployDataFixed
	Crowdloan_Onboarded                []EventCrowdloanOnboarded
	Crowdloan_HandleBidResult          []EventCrowdloanHandleBidResult
	Crowdloan_Edited                   []EventCrowdloanEdited
	Crowdloan_MemoUpdated              []EventCrowdloanMemoUpdated
	Crowdloan_AddedToNewRaise          []EventCrowdloanAddedToNewRaise
	Identity_IdentitySet               []EventIdentitySet
	Identity_IdentityCleared           []EventIdentityCleared
	Identity_IdentityKilled            []EventIdentityKilled
	Identity_JudgementRequested        []EventIdentityJudgementRequested
	Identity_JudgementUnrequested      []EventIdentityJudgementUnrequested
	Identity_JudgementGiven            []EventIdentityJudgementGiven
	Identity_RegistrarAdded            []EventIdentityRegistrarAdded
	Identity_SubIdentityAdded          []EventIdentitySubIdentityAdded
	Identity_SubIdentityRemoved        []EventIdentitySubIdentityRemoved
	Identity_SubIdentityRevoked        []EventIdentitySubIdentityRevoked
	ImOnline_HeartbeatReceived         []EventImOnlineHeartbeatReceived
	ImOnline_AllGood                   []EventImOnlineAllGood
	ImOnline_SomeOffline               []EventImOnlineSomeOffline
	Treasury_Awarded                   []EventTreasuryAwarded
	Treasury_Burnt                     []EventTreasuryBurnt
	Treasury_Deposit                   []EventTreasuryDeposit
	Treasury_Proposed                  []EventTreasuryProposed
	Treasury_Rejected                  []EventTreasuryRejected
	Treasury_Rollover                  []EventTreasuryRollover
	Treasury_Spending                  []EventTreasurySpending
	Utility_BatchInterrupted           []EventUtilityBatchInterrupted
	Utility_BatchCompleted             []EventUtilityBatchCompleted
	Staking_EraPayout                  []EventStakingEraPayout
	Staking_Reward                     []EventStakingReward
	Staking_Slash                      []EventStakingSlash
	Staking_OldSlashingReportDiscarded []EventStakingOldSlashingReportDiscarded
	Staking_StakingElection            []EventStakingStakingElection
	Staking_Bonded                     []EventStakingBonded
	Staking_Unbonded                   []EventStakingUnbonded
	Staking_Withdrawn                  []EventStakingWithdrawn
	Staking_Kicked                     []EventStakingKicked
	Staking_Chilled                    []EventStakingChilled
	XcmPallet_Attempted                []EventXcmPalletAttempted
	// XcmPallet_Sent                                  []EventXcmPalletSent
	System_CodeUpdated      []EventSystemCodeUpdated
	System_ExtrinsicFailed  []EventSystemExtrinsicFailed
	System_ExtrinsicSuccess []EventSystemExtrinsicSuccess
	System_KilledAccount    []EventSystemKilledAccount
	System_NewAccount       []EventSystemNewAccount
	System_Remarked         []EventSystemRemarked
	Session_NewSession      []EventSessionNewSession
}
