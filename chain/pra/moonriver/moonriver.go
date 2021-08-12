package moonriver

import (
	"fmt"
	"reflect"

	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain/pra/frontier"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
)

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

// https://github.com/PureStake/moonbeam/blob/master/moonbeam-types-bundle/index.ts
type EthereumAccountId [20]byte
type AccountId EthereumAccountId
type Address AccountId
type Balance = types.U128
type RefCount = types.U8
type LookupSource AccountId
type AuthorId = types.AccountID
type RoundIndex = types.U32
type Percent = types.U8
type Perbill = types.U8

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
type AccountID = AccountId
type RelaychainAccountId = types.AccountID
type BalanceStatus = types.BalanceStatus
type AuthorityID = AccountID
type UCompact = types.UCompact
type AccountIndex = types.AccountIndex
type ElectionCompute = types.ElectionCompute
type Weight = types.Weight
type DispatchError = types.DispatchError
type OptionBytes = types.OptionBytes
type BlockNumber = types.U32

func (ea *EthereumAccountId) Hex() string {
	return fmt.Sprintf("%#x", *ea)
}

// EventSystemRemarked is emitted on on-chain remark happened.
type EventSystemRemarked struct {
	Phase      types.Phase
	Origin     types.AccountID
	RemarkHash types.Hash
	Topics     []types.Hash
}

// COPY FROM GSRPC
// EventClaimsClaimed is emitted when an account claims some DOTs
type EventClaimsClaimed struct {
	Phase           Phase
	Who             AccountID
	EthereumAddress H160
	Amount          U128
	Topics          []Hash
}

// EventBalancesEndowed is emitted when an account is created with some free balance
type EventBalancesEndowed struct {
	Phase   Phase
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventDustLost is emitted when an account is removed with a balance that is
// non-zero but below ExistentialDeposit, resulting in a loss.
type EventBalancesDustLost struct {
	Phase   Phase
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventBalancesTransfer is emitted when a transfer succeeded (from, to, value)
type EventBalancesTransfer struct {
	Phase  Phase
	From   AccountID
	To     AccountID
	Value  U128
	Topics []Hash
}

// EventBalanceSet is emitted when a balance is set by root
type EventBalancesBalanceSet struct {
	Phase    Phase
	Who      AccountID
	Free     U128
	Reserved U128
	Topics   []Hash
}

// EventDeposit is emitted when an account receives some free balance
type EventBalancesDeposit struct {
	Phase   Phase
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventBalancesReserved is emitted when some balance was reserved (moved from free to reserved)
type EventBalancesReserved struct {
	Phase   Phase
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventBalancesUnreserved is emitted when some balance was unreserved (moved from reserved to free)
type EventBalancesUnreserved struct {
	Phase   Phase
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventBalancesReserveRepatriated is emitted when some balance was moved from the reserve of the first account to the
// second account.
type EventBalancesReserveRepatriated struct {
	Phase             Phase
	From              AccountID
	To                AccountID
	Balance           U128
	DestinationStatus BalanceStatus
	Topics            []Hash
}

// EventGrandpaNewAuthorities is emitted when a new authority set has been applied
type EventGrandpaNewAuthorities struct {
	Phase          Phase
	NewAuthorities []struct {
		AuthorityID     AuthorityID
		AuthorityWeight U64
	}
	Topics []Hash
}

// EventGrandpaPaused is emitted when the current authority set has been paused
type EventGrandpaPaused struct {
	Phase  Phase
	Topics []Hash
}

// EventGrandpaResumed is emitted when the current authority set has been resumed
type EventGrandpaResumed struct {
	Phase  Phase
	Topics []Hash
}

// EventImOnlineHeartbeatReceived is emitted when a new heartbeat was received from AuthorityId
type EventImOnlineHeartbeatReceived struct {
	Phase       Phase
	AuthorityID AuthorityID
	Topics      []Hash
}

// EventImOnlineAllGood is emitted when at the end of the session, no offence was committed
type EventImOnlineAllGood struct {
	Phase  Phase
	Topics []Hash
}

// Exposure lists the own and nominated stake of a validator
type Exposure struct {
	Total  UCompact
	Own    UCompact
	Others []IndividualExposure
}

// IndividualExposure contains the nominated stake by one specific third party
type IndividualExposure struct {
	Who   AccountID
	Value UCompact
}

// EventImOnlineSomeOffline is emitted when the end of the session, at least once validator was found to be offline
type EventImOnlineSomeOffline struct {
	Phase                Phase
	IdentificationTuples []struct {
		ValidatorID        AccountID
		FullIdentification Exposure
	}
	Topics []Hash
}

// EventIndicesIndexAssigned is emitted when an index is assigned to an AccountID.
type EventIndicesIndexAssigned struct {
	Phase        Phase
	AccountID    AccountID
	AccountIndex AccountIndex
	Topics       []Hash
}

// EventIndicesIndexFreed is emitted when an index is unassigned.
type EventIndicesIndexFreed struct {
	Phase        Phase
	AccountIndex AccountIndex
	Topics       []Hash
}

// EventIndicesIndexFrozen is emitted when an index is frozen to its current account ID.
type EventIndicesIndexFrozen struct {
	Phase        Phase
	AccountIndex AccountIndex
	AccountID    AccountID
	Topics       []Hash
}

// EventOffencesOffence is emitted when there is an offence reported of the given kind happened at the session_index
// and (kind-specific) time slot. This event is not deposited for duplicate slashes
type EventOffencesOffence struct {
	Phase          Phase
	Kind           Bytes16
	OpaqueTimeSlot Bytes
	Topics         []Hash
}

// EventSessionNewSession is emitted when a new session has happened. Note that the argument is the session index,
// not the block number as the type might suggest
type EventSessionNewSession struct {
	Phase        Phase
	SessionIndex U32
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
	Phase   Phase
	Compute ElectionCompute
	Topics  []Hash
}

// EventStakingSolutionStored is emitted when a new solution for the upcoming election has been stored
type EventStakingSolutionStored struct {
	Phase   Phase
	Compute ElectionCompute
	Topics  []Hash
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

// EventSystemExtrinsicSuccessV8 is emitted when an extrinsic completed successfully
//
// Deprecated: EventSystemExtrinsicSuccessV8 exists to allow users to simply implement their own EventRecords struct if
// they are on metadata version 8 or below. Use EventSystemExtrinsicSuccess otherwise
type EventSystemExtrinsicSuccessV8 struct {
	Phase  Phase
	Topics []Hash
}

// EventSystemExtrinsicSuccess is emitted when an extrinsic completed successfully
type EventSystemExtrinsicSuccess struct {
	Phase        Phase
	DispatchInfo DispatchInfo
	Topics       []Hash
}

type Pays struct {
	IsYes bool
	IsNo  bool
}

func (p *Pays) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	switch b {
	case 0:
		p.IsYes = true
	case 1:
		p.IsNo = true
	default:
		return fmt.Errorf("unknown DispatchClass enum: %v", b)
	}
	return err
}

func (p Pays) Encode(encoder scale.Encoder) error {
	var err error
	if p.IsYes {
		err = encoder.PushByte(0)
	} else if p.IsNo {
		err = encoder.PushByte(1)
	}
	return err
}

// DispatchInfo contains a bundle of static information collected from the `#[weight = $x]` attributes.
type DispatchInfo struct {
	// Weight of this transaction
	Weight Weight
	// Class of this transaction
	Class DispatchClass
	// PaysFee indicates whether this transaction pays fees
	PaysFee Pays
}

// DispatchClass is a generalized group of dispatch types. This is only distinguishing normal, user-triggered
// transactions (`Normal`) and anything beyond which serves a higher purpose to the system (`Operational`).
type DispatchClass struct {
	// A normal dispatch
	IsNormal bool
	// An operational dispatch
	IsOperational bool
	// A mandatory dispatch
	IsMandatory bool
}

func (d *DispatchClass) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	switch b {
	case 0:
		d.IsNormal = true
	case 1:
		d.IsOperational = true
	case 2:
		d.IsMandatory = true
	default:
		return fmt.Errorf("unknown DispatchClass enum: %v", b)
	}
	return err
}

//nolint:gocritic,golint
func (d DispatchClass) Encode(encoder scale.Encoder) error {
	var err error
	if d.IsNormal {
		err = encoder.PushByte(0)
	} else if d.IsOperational {
		err = encoder.PushByte(1)
	} else if d.IsMandatory {
		err = encoder.PushByte(2)
	}
	return err
}

// EventSystemExtrinsicFailedV8 is emitted when an extrinsic failed
//
// Deprecated: EventSystemExtrinsicFailedV8 exists to allow users to simply implement their own EventRecords struct if
// they are on metadata version 8 or below. Use EventSystemExtrinsicFailed otherwise
type EventSystemExtrinsicFailedV8 struct {
	Phase         Phase
	DispatchError DispatchError
	Topics        []Hash
}

// EventSystemExtrinsicFailed is emitted when an extrinsic failed
type EventSystemExtrinsicFailed struct {
	Phase         Phase
	DispatchError DispatchError
	DispatchInfo  DispatchInfo
	Topics        []Hash
}

// EventSystemCodeUpdated is emitted when the runtime code (`:code`) is updated
type EventSystemCodeUpdated struct {
	Phase  Phase
	Topics []Hash
}

// EventSystemNewAccount is emitted when a new account was created
type EventSystemNewAccount struct {
	Phase  Phase
	Who    AccountID
	Topics []Hash
}

// EventSystemKilledAccount is emitted when an account is reaped
type EventSystemKilledAccount struct {
	Phase  Phase
	Who    AccountID
	Topics []Hash
}

// EventAssetIssued is emitted when an asset is issued.
type EventAssetIssued struct {
	Phase   Phase
	AssetID U32
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventAssetTransferred is emitted when an asset is transferred.
type EventAssetTransferred struct {
	Phase   Phase
	AssetID U32
	To      AccountID
	From    AccountID
	Balance U128
	Topics  []Hash
}

// EventAssetDestroyed is emitted when an asset is destroyed.
type EventAssetDestroyed struct {
	Phase   Phase
	AssetID U32
	Who     AccountID
	Balance U128
	Topics  []Hash
}

// EventDemocracyProposed is emitted when a motion has been proposed by a public account.
type EventDemocracyProposed struct {
	Phase         Phase
	ProposalIndex U32
	Balance       U128
	Topics        []Hash
}

// EventDemocracyTabled is emitted when a public proposal has been tabled for referendum vote.
type EventDemocracyTabled struct {
	Phase         Phase
	ProposalIndex U32
	Balance       U128
	Accounts      []AccountID
	Topics        []Hash
}

// EventDemocracyExternalTabled is emitted when an external proposal has been tabled.
type EventDemocracyExternalTabled struct {
	Phase  Phase
	Topics []Hash
}

// VoteThreshold is a means of determining if a vote is past pass threshold.
type VoteThreshold byte

const (
	// SuperMajorityApprove require super majority of approvals is needed to pass this vote.
	SuperMajorityApprove VoteThreshold = 0
	// SuperMajorityAgainst require super majority of rejects is needed to fail this vote.
	SuperMajorityAgainst VoteThreshold = 1
	// SimpleMajority require simple majority of approvals is needed to pass this vote.
	SimpleMajority VoteThreshold = 2
)

func (v *VoteThreshold) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	vb := VoteThreshold(b)
	switch vb {
	case SuperMajorityApprove, SuperMajorityAgainst, SimpleMajority:
		*v = vb
	default:
		return fmt.Errorf("unknown VoteThreshold enum: %v", vb)
	}
	return err
}

func (v VoteThreshold) Encode(encoder scale.Encoder) error {
	return encoder.PushByte(byte(v))
}

// EventDemocracyStarted is emitted when a referendum has begun.
type EventDemocracyStarted struct {
	Phase           Phase
	ReferendumIndex U32
	VoteThreshold   VoteThreshold
	Topics          []Hash
}

// EventDemocracyPassed is emitted when a proposal has been approved by referendum.
type EventDemocracyPassed struct {
	Phase           Phase
	ReferendumIndex U32
	Topics          []Hash
}

// EventDemocracyNotPassed is emitted when a proposal has been rejected by referendum.
type EventDemocracyNotPassed struct {
	Phase           Phase
	ReferendumIndex U32
	Topics          []Hash
}

// EventDemocracyCancelled is emitted when a referendum has been cancelled.
type EventDemocracyCancelled struct {
	Phase           Phase
	ReferendumIndex U32
	Topics          []Hash
}

// EventDemocracyExecuted is emitted when a proposal has been enacted.
type EventDemocracyExecuted struct {
	Phase           Phase
	ReferendumIndex U32
	Result          bool
	Topics          []Hash
}

// EventDemocracyDelegated is emitted when an account has delegated their vote to another account.
type EventDemocracyDelegated struct {
	Phase  Phase
	Who    AccountID
	Target AccountID
	Topics []Hash
}

// EventDemocracyUndelegated is emitted when an account has cancelled a previous delegation operation.
type EventDemocracyUndelegated struct {
	Phase  Phase
	Target AccountID
	Topics []Hash
}

// EventDemocracyVetoed is emitted when an external proposal has been vetoed.
type EventDemocracyVetoed struct {
	Phase       Phase
	Who         AccountID
	Hash        Hash
	BlockNumber BlockNumber
	Topics      []Hash
}

// EventDemocracyPreimageNoted is emitted when a proposal's preimage was noted, and the deposit taken.
type EventDemocracyPreimageNoted struct {
	Phase     Phase
	Hash      Hash
	AccountID AccountID
	Balance   U128
	Topics    []Hash
}

// EventDemocracyPreimageUsed is emitted when a proposal preimage was removed and used (the deposit was returned).
type EventDemocracyPreimageUsed struct {
	Phase     Phase
	Hash      Hash
	AccountID AccountID
	Balance   U128
	Topics    []Hash
}

// EventDemocracyPreimageInvalid is emitted when a proposal could not be executed because its preimage was invalid.
type EventDemocracyPreimageInvalid struct {
	Phase           Phase
	Hash            Hash
	ReferendumIndex U32
	Topics          []Hash
}

// EventDemocracyPreimageMissing is emitted when a proposal could not be executed because its preimage was missing.
type EventDemocracyPreimageMissing struct {
	Phase           Phase
	Hash            Hash
	ReferendumIndex U32
	Topics          []Hash
}

// EventDemocracyPreimageReaped is emitted when a registered preimage was removed
// and the deposit collected by the reaper (last item).
type EventDemocracyPreimageReaped struct {
	Phase    Phase
	Hash     Hash
	Provider AccountID
	Balance  U128
	Who      AccountID
	Topics   []Hash
}

// EventDemocracyUnlocked is emitted when an account has been unlocked successfully.
type EventDemocracyUnlocked struct {
	Phase     Phase
	AccountID AccountID
	Topics    []Hash
}

// EventDemocracyBlacklisted is emitted when A proposal has been blacklisted permanently
type EventDemocracyBlacklisted struct {
	Phase  Phase
	Hash   Hash
	Topics []Hash
}

// EventCollectiveProposed is emitted when a motion (given hash) has been proposed (by given account)
// with a threshold (given `MemberCount`).
type EventCollectiveProposed struct {
	Phase         Phase
	Who           AccountID
	ProposalIndex U32
	Proposal      Hash
	MemberCount   U32
	Topics        []Hash
}

// EventCollectiveVote is emitted when a motion (given hash) has been voted on by given account, leaving
// a tally (yes votes and no votes given respectively as `MemberCount`).
type EventCollectiveVoted struct {
	Phase    Phase
	Who      AccountID
	Proposal Hash
	Approve  bool
	YesCount U32
	NoCount  U32
	Topics   []Hash
}

// EventCollectiveApproved is emitted when a motion was approved by the required threshold.
type EventCollectiveApproved struct {
	Phase    Phase
	Proposal Hash
	Topics   []Hash
}

// EventCollectiveDisapproved is emitted when a motion was not approved by the required threshold.
type EventCollectiveDisapproved struct {
	Phase    Phase
	Proposal Hash
	Topics   []Hash
}

// EventCollectiveExecuted is emitted when a motion was executed; `result` is true if returned without error.
type EventCollectiveExecuted struct {
	Phase    Phase
	Proposal Hash
	Result   DispatchResult
	Topics   []Hash
}

// EventCollectiveMemberExecuted is emitted when a single member did some action;
// `result` is true if returned without error.
type EventCollectiveMemberExecuted struct {
	Phase    Phase
	Proposal Hash
	Result   DispatchResult
	Topics   []Hash
}

// EventCollectiveClosed is emitted when a proposal was closed after its duration was up.
type EventCollectiveClosed struct {
	Phase    Phase
	Proposal Hash
	YesCount U32
	NoCount  U32
	Topics   []Hash
}

// EventTechnicalCommitteeProposed is emitted when a motion (given hash) has been proposed (by given account)
// with a threshold (given, `MemberCount`)
type EventTechnicalCommitteeProposed struct {
	Phase         Phase
	Account       AccountID
	ProposalIndex U32
	Proposal      Hash
	Threshold     U32
	Topics        []Hash
}

// EventTechnicalCommitteeVoted is emitted when a motion (given hash) has been voted on by given account, leaving,
// a tally (yes votes and no votes given respectively as `MemberCount`).
type EventTechnicalCommitteeVoted struct {
	Phase    Phase
	Account  AccountID
	Proposal Hash
	Voted    bool
	YesCount U32
	NoCount  U32
	Topics   []Hash
}

// EventTechnicalCommitteeApproved is emitted when a motion was approved by the required threshold.
type EventTechnicalCommitteeApproved struct {
	Phase    Phase
	Proposal Hash
	Topics   []Hash
}

// EventTechnicalCommitteeDisapproved is emitted when a motion was not approved by the required threshold.
type EventTechnicalCommitteeDisapproved struct {
	Phase    Phase
	Proposal Hash
	Topics   []Hash
}

// EventTechnicalCommitteeExecuted is emitted when a motion was executed;
// result will be `Ok` if it returned without error.
type EventTechnicalCommitteeExecuted struct {
	Phase    Phase
	Proposal Hash
	Result   DispatchResult
	Topics   []Hash
}

// EventTechnicalCommitteeMemberExecuted is emitted when a single member did some action;
// result will be `Ok` if it returned without error
type EventTechnicalCommitteeMemberExecuted struct {
	Phase    Phase
	Proposal Hash
	Result   DispatchResult
	Topics   []Hash
}

// EventTechnicalCommitteeClosed is emitted when A proposal was closed because its threshold was reached
// or after its duration was up
type EventTechnicalCommitteeClosed struct {
	Phase    Phase
	Proposal Hash
	YesCount U32
	NoCount  U32
	Topics   []Hash
}

// EventTechnicalMembershipMemberAdded is emitted when the given member was added; see the transaction for who
type EventTechnicalMembershipMemberAdded struct {
	Phase  Phase
	Topics []Hash
}

// EventTechnicalMembershipMemberRemoved is emitted when the given member was removed; see the transaction for who
type EventTechnicalMembershipMemberRemoved struct {
	Phase  Phase
	Topics []Hash
}

// EventTechnicalMembershipMembersSwapped is emitted when two members were swapped;; see the transaction for who
type EventTechnicalMembershipMembersSwapped struct {
	Phase  Phase
	Topics []Hash
}

// EventTechnicalMembershipMembersReset is emitted when the membership was reset;
// see the transaction for who the new set is.
type EventTechnicalMembershipMembersReset struct {
	Phase  Phase
	Topics []Hash
}

// EventTechnicalMembershipKeyChanged is emitted when one of the members' keys changed.
type EventTechnicalMembershipKeyChanged struct {
	Phase  Phase
	Topics []Hash
}

// EventTechnicalMembershipKeyChanged is emitted when - phantom member, never used.
type EventTechnicalMembershipDummy struct {
	Phase  Phase
	Topics []Hash
}

// EventElectionsNewTerm is emitted when a new term with new members.
// This indicates that enough candidates existed, not that enough have has been elected.
// The inner value must be examined for this purpose.
type EventElectionsNewTerm struct {
	Phase      Phase
	NewMembers []struct {
		Member  AccountID
		Balance U128
	}
	Topics []Hash
}

// EventElectionsEmpty is emitted when No (or not enough) candidates existed for this round.
type EventElectionsEmptyTerm struct {
	Phase  Phase
	Topics []Hash
}

// EventElectionsElectionError is emitted when an internal error happened while trying to perform election
type EventElectionsElectionError struct {
	Phase  Phase
	Topics []Hash
}

// EventElectionsMemberKicked is emitted when a member has been removed.
// This should always be followed by either `NewTerm` or `EmptyTerm`.
type EventElectionsMemberKicked struct {
	Phase  Phase
	Member AccountID
	Topics []Hash
}

// EventElectionsMemberRenounced is emitted when a member has renounced their candidacy.
type EventElectionsMemberRenounced struct {
	Phase  Phase
	Member AccountID
	Topics []Hash
}

// EventElectionsVoterReported is emitted when a voter (first element) was reported (by the second element)
// with the the report being successful or not (third element).
type EventElectionsVoterReported struct {
	Phase            Phase
	Target, Reporter AccountID
	Valid            bool
	Topics           []Hash
}

// A name was set or reset (which will remove all judgements).
type EventIdentitySet struct {
	Phase    Phase
	Identity AccountID
	Topics   []Hash
}

// A name was cleared, and the given balance returned.
type EventIdentityCleared struct {
	Phase    Phase
	Identity AccountID
	Balance  U128
	Topics   []Hash
}

// A name was removed and the given balance slashed.
type EventIdentityKilled struct {
	Phase    Phase
	Identity AccountID
	Balance  U128
	Topics   []Hash
}

// A judgement was asked from a registrar.
type EventIdentityJudgementRequested struct {
	Phase          Phase
	Sender         AccountID
	RegistrarIndex U32
	Topics         []Hash
}

// A judgement request was retracted.
type EventIdentityJudgementUnrequested struct {
	Phase          Phase
	Sender         AccountID
	RegistrarIndex U32
	Topics         []Hash
}

// A judgement was given by a registrar.
type EventIdentityJudgementGiven struct {
	Phase          Phase
	Target         AccountID
	RegistrarIndex U32
	Topics         []Hash
}

// A registrar was added.
type EventIdentityRegistrarAdded struct {
	Phase          Phase
	RegistrarIndex U32
	Topics         []Hash
}

// EventIdentitySubIdentityAdded is emitted when a sub-identity was added to an identity and the deposit paid
type EventIdentitySubIdentityAdded struct {
	Phase   Phase
	Sub     AccountID
	Main    AccountID
	Deposit U128
	Topics  []Hash
}

// EventIdentitySubIdentityRemoved is emitted when a sub-identity was removed from an identity and the deposit freed
type EventIdentitySubIdentityRemoved struct {
	Phase   Phase
	Sub     AccountID
	Main    AccountID
	Deposit U128
	Topics  []Hash
}

// EventIdentitySubIdentityRevoked is emitted when a sub-identity was cleared, and the given deposit repatriated from
// the main identity account to the sub-identity account.
type EventIdentitySubIdentityRevoked struct {
	Phase   Phase
	Sub     AccountID
	Main    AccountID
	Deposit U128
	Topics  []Hash
}

// EventSocietyFounded is emitted when the society is founded by the given identity
type EventSocietyFounded struct {
	Phase   Phase
	Founder AccountID
	Topics  []Hash
}

// EventSocietyBid is emitted when a membership bid just happened. The given account is the candidate's ID
// and their offer is the second
type EventSocietyBid struct {
	Phase     Phase
	Candidate AccountID
	Offer     U128
	Topics    []Hash
}

// EventSocietyVouch is emitted when a membership bid just happened by vouching.
// The given account is the candidate's ID and, their offer is the second. The vouching party is the third.
type EventSocietyVouch struct {
	Phase     Phase
	Candidate AccountID
	Offer     U128
	Vouching  AccountID
	Topics    []Hash
}

// EventSocietyAutoUnbid is emitted when a [candidate] was dropped (due to an excess of bids in the system)
type EventSocietyAutoUnbid struct {
	Phase     Phase
	Candidate AccountID
	Topics    []Hash
}

// EventSocietyUnbid is emitted when a [candidate] was dropped (by their request)
type EventSocietyUnbid struct {
	Phase     Phase
	Candidate AccountID
	Topics    []Hash
}

// EventSocietyUnvouch is emitted when a [candidate] was dropped (by request of who vouched for them)
type EventSocietyUnvouch struct {
	Phase     Phase
	Candidate AccountID
	Topics    []Hash
}

// EventSocietyInducted is emitted when a group of candidates have been inducted.
// The batch's primary is the first value, the batch in full is the second.
type EventSocietyInducted struct {
	Phase      Phase
	Primary    AccountID
	Candidates []AccountID
	Topics     []Hash
}

// EventSocietySuspendedMemberJudgement is emitted when a suspended member has been judged
type EventSocietySuspendedMemberJudgement struct {
	Phase  Phase
	Who    AccountID
	Judged bool
	Topics []Hash
}

// EventSocietyCandidateSuspended is emitted when a [candidate] has been suspended
type EventSocietyCandidateSuspended struct {
	Phase     Phase
	Candidate AccountID
	Topics    []Hash
}

// EventSocietyMemberSuspended is emitted when a [member] has been suspended
type EventSocietyMemberSuspended struct {
	Phase  Phase
	Member AccountID
	Topics []Hash
}

// EventSocietyChallenged is emitted when a [member] has been challenged
type EventSocietyChallenged struct {
	Phase  Phase
	Member AccountID
	Topics []Hash
}

// EventSocietyVote is emitted when a vote has been placed
type EventSocietyVote struct {
	Phase     Phase
	Candidate AccountID
	Voter     AccountID
	Vote      bool
	Topics    []Hash
}

// EventSocietyDefenderVote is emitted when a vote has been placed for a defending member
type EventSocietyDefenderVote struct {
	Phase  Phase
	Voter  AccountID
	Vote   bool
	Topics []Hash
}

// EventSocietyNewMaxMembers is emitted when a new [max] member count has been set
type EventSocietyNewMaxMembers struct {
	Phase  Phase
	Max    U32
	Topics []Hash
}

// EventSocietyUnfounded is emitted when society is unfounded
type EventSocietyUnfounded struct {
	Phase   Phase
	Founder AccountID
	Topics  []Hash
}

// EventSocietyDeposit is emitted when some funds were deposited into the society account
type EventSocietyDeposit struct {
	Phase  Phase
	Value  U128
	Topics []Hash
}

// EventRecoveryCreated is emitted when a recovery process has been set up for an account
type EventRecoveryCreated struct {
	Phase  Phase
	Who    AccountID
	Topics []Hash
}

// EventRecoveryInitiated is emitted when a recovery process has been initiated for account_1 by account_2
type EventRecoveryInitiated struct {
	Phase   Phase
	Account AccountID
	Who     AccountID
	Topics  []Hash
}

// EventRecoveryVouched is emitted when a recovery process for account_1 by account_2 has been vouched for by account_3
type EventRecoveryVouched struct {
	Phase   Phase
	Lost    AccountID
	Rescuer AccountID
	Who     AccountID
	Topics  []Hash
}

// EventRecoveryClosed is emitted when a recovery process for account_1 by account_2 has been closed
type EventRecoveryClosed struct {
	Phase   Phase
	Who     AccountID
	Rescuer AccountID
	Topics  []Hash
}

// EventRecoveryAccountRecovered is emitted when account_1 has been successfully recovered by account_2
type EventRecoveryAccountRecovered struct {
	Phase   Phase
	Who     AccountID
	Rescuer AccountID
	Topics  []Hash
}

// EventRecoveryRemoved is emitted when a recovery process has been removed for an account
type EventRecoveryRemoved struct {
	Phase  Phase
	Who    AccountID
	Topics []Hash
}

// EventVestingVestingUpdated is emitted when the amount vested has been updated.
// This could indicate more funds are available.
// The balance given is the amount which is left unvested (and thus locked)
type EventVestingVestingUpdated struct {
	Phase    Phase
	Account  AccountID
	Unvested U128
	Topics   []Hash
}

// EventVestingVestingCompleted is emitted when an [account] has become fully vested. No further vesting can happen
type EventVestingVestingCompleted struct {
	Phase   Phase
	Account AccountID
	Topics  []Hash
}

// EventSchedulerScheduled is emitted when scheduled some task
type EventSchedulerScheduled struct {
	Phase  Phase
	When   BlockNumber
	Index  U32
	Topics []Hash
}

// EventSchedulerCanceled is emitted when canceled some task
type EventSchedulerCanceled struct {
	Phase  Phase
	When   BlockNumber
	Index  U32
	Topics []Hash
}

// EventSchedulerDispatched is emitted when dispatched some task
type EventSchedulerDispatched struct {
	Phase  Phase
	Task   TaskAddress
	ID     OptionBytes
	Result DispatchResult
	Topics []Hash
}

type ProxyType byte

const (
	Any         ProxyType = 0
	NonTransfer ProxyType = 1
	Governance  ProxyType = 2
	Staking     ProxyType = 3
)

func (pt *ProxyType) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	vb := ProxyType(b)
	switch vb {
	case Any, NonTransfer, Governance, Staking:
		*pt = vb
	default:
		return fmt.Errorf("unknown ProxyType enum: %v", vb)
	}
	return err
}

func (pt ProxyType) Encode(encoder scale.Encoder) error {
	return encoder.PushByte(byte(pt))
}

// EventProxyProxyExecuted is emitted when a proxy was executed correctly, with the given [result]
type EventProxyProxyExecuted struct {
	Phase  Phase
	Result DispatchResult
	Topics []Hash
}

// EventProxyAnonymousCreated is emitted when an anonymous account has been created by new proxy with given,
// disambiguation index and proxy type.
type EventProxyAnonymousCreated struct {
	Phase               Phase
	Anonymous           AccountID
	Who                 AccountID
	ProxyType           ProxyType
	DisambiguationIndex U16
	Topics              []Hash
}

// EventProxyAnnounced is emitted when an announcement was placed to make a call in the future
type EventProxyAnnounced struct {
	Phase    Phase
	Real     AccountID
	Proxy    AccountID
	CallHash Hash
	Topics   []Hash
}

// EventSudoSudid is emitted when a sudo just took place.
type EventSudoSudid struct {
	Phase  Phase
	Result DispatchResult
	Topics []Hash
}

// EventSudoKeyChanged is emitted when the sudoer just switched identity; the old key is supplied.
type EventSudoKeyChanged struct {
	Phase     Phase
	AccountID AccountID
	Topics    []Hash
}

// A sudo just took place.
type EventSudoAsDone struct {
	Phase  Phase
	Done   bool
	Topics []Hash
}

// EventTreasuryProposed is emitted when New proposal.
type EventTreasuryProposed struct {
	Phase         Phase
	ProposalIndex U32
	Topics        []Hash
}

// EventTreasurySpending is emitted when we have ended a spend period and will now allocate funds.
type EventTreasurySpending struct {
	Phase           Phase
	BudgetRemaining U128
	Topics          []Hash
}

// EventTreasuryAwarded is emitted when some funds have been allocated.
type EventTreasuryAwarded struct {
	Phase         Phase
	ProposalIndex U32
	Amount        U128
	Beneficiary   AccountID
	Topics        []Hash
}

// EventTreasuryRejected is emitted when s proposal was rejected; funds were slashed.
type EventTreasuryRejected struct {
	Phase         Phase
	ProposalIndex U32
	Amount        U128
	Topics        []Hash
}

// EventTreasuryBurnt is emitted when some of our funds have been burnt.
type EventTreasuryBurnt struct {
	Phase  Phase
	Burn   U128
	Topics []Hash
}

// EventTreasuryRollover is emitted when spending has finished; this is the amount that rolls over until next spend.
type EventTreasuryRollover struct {
	Phase           Phase
	BudgetRemaining U128
	Topics          []Hash
}

// EventTreasuryDeposit is emitted when some funds have been deposited.
type EventTreasuryDeposit struct {
	Phase     Phase
	Deposited U128
	Topics    []Hash
}

// EventTreasuryNewTip is emitted when a new tip suggestion has been opened.
type EventTreasuryNewTip struct {
	Phase  Phase
	Hash   Hash
	Topics []Hash
}

// EventTreasuryTipClosing is emitted when a tip suggestion has reached threshold and is closing.
type EventTreasuryTipClosing struct {
	Phase  Phase
	Hash   Hash
	Topics []Hash
}

// EventTreasuryTipClosed is emitted when a tip suggestion has been closed.
type EventTreasuryTipClosed struct {
	Phase     Phase
	Hash      Hash
	AccountID AccountID
	Balance   U128
	Topics    []Hash
}

// EventTreasuryTipRetracted is emitted when a tip suggestion has been retracted.
type EventTreasuryTipRetracted struct {
	Phase  Phase
	Hash   Hash
	Topics []Hash
}

type BountyIndex U32

// EventTreasuryBountyProposed is emitted for a new bounty proposal.
type EventTreasuryBountyProposed struct {
	Phase  Phase
	Index  BountyIndex
	Topics []Hash
}

// EventTreasuryBountyRejected is emitted when a bounty proposal was rejected; funds were slashed.
type EventTreasuryBountyRejected struct {
	Phase  Phase
	Index  BountyIndex
	Bond   U128
	Topics []Hash
}

// EventTreasuryBountyBecameActive is emitted when a bounty proposal is funded and became active
type EventTreasuryBountyBecameActive struct {
	Phase  Phase
	Index  BountyIndex
	Topics []Hash
}

// EventTreasuryBountyAwarded is emitted when a bounty is awarded to a beneficiary
type EventTreasuryBountyAwarded struct {
	Phase       Phase
	Index       BountyIndex
	Beneficiary AccountID
	Topics      []Hash
}

// EventTreasuryBountyClaimed is emitted when A bounty is claimed by beneficiary
type EventTreasuryBountyClaimed struct {
	Phase       Phase
	Index       BountyIndex
	Payout      U128
	Beneficiary AccountID
	Topics      []Hash
}

// EventTreasuryBountyCanceled is emitted when a bounty is cancelled.
type EventTreasuryBountyCanceled struct {
	Phase  Phase
	Index  BountyIndex
	Topics []Hash
}

// EventTreasuryBountyExtended is emitted when a bounty is extended.
type EventTreasuryBountyExtended struct {
	Phase  Phase
	Index  BountyIndex
	Topics []Hash
}

// EventContractsInstantiated is emitted when a contract is deployed by address at the specified address
type EventContractsInstantiated struct {
	Phase    Phase
	Owner    AccountID
	Contract AccountID
	Topics   []Hash
}

// EventContractsEvicted is emitted when a contract has been evicted and is now in tombstone state.
type EventContractsEvicted struct {
	Phase     Phase
	Contract  AccountID
	Tombstone bool
	Topics    []Hash
}

// EventContractsRestored is emitted when a restoration for a contract has been successful.
type EventContractsRestored struct {
	Phase         Phase
	Donor         AccountID
	Destination   AccountID
	CodeHash      Hash
	RentAllowance U128
	Topics        []Hash
}

// EventContractsCodeStored is emitted when code with the specified hash has been stored
type EventContractsCodeStored struct {
	Phase    Phase
	CodeHash Hash
	Topics   []Hash
}

// EventContractsScheduleUpdated is triggered when the current [schedule] is updated
type EventContractsScheduleUpdated struct {
	Phase    Phase
	Schedule U32
	Topics   []Hash
}

// EventContractsContractExecution is triggered when an event deposited upon execution of a contract from the account
type EventContractsContractExecution struct {
	Phase   Phase
	Account AccountID
	Data    Bytes
	Topics  []Hash
}

// EventUtilityBatchInterrupted is emitted when a batch of dispatches did not complete fully.
// Index of first failing dispatch given, as well as the error.
type EventUtilityBatchInterrupted struct {
	Phase         Phase
	Index         U32
	DispatchError DispatchError
	Topics        []Hash
}

// EventUtilityBatchCompleted is emitted when a batch of dispatches completed fully with no error.
type EventUtilityBatchCompleted struct {
	Phase  Phase
	Topics []Hash
}

// EventUtilityNewMultisig is emitted when a new multisig operation has begun.
// First param is the account that is approving, second is the multisig account, third is hash of the call.
type EventMultisigNewMultisig struct {
	Phase    Phase
	Who, ID  AccountID
	CallHash Hash
	Topics   []Hash
}

// TimePoint is a global extrinsic index, formed as the extrinsic index within a block,
// together with that block's height.
type TimePoint struct {
	Height U32
	Index  U32
}

// TaskAddress holds the location of a scheduled task that can be used to remove it
type TaskAddress struct {
	When  BlockNumber
	Index U32
}

// EventUtility is emitted when a multisig operation has been approved by someone. First param is the account that is
// approving, third is the multisig account, fourth is hash of the call.
type EventMultisigApproval struct {
	Phase     Phase
	Who       AccountID
	TimePoint TimePoint
	ID        AccountID
	CallHash  Hash
	Topics    []Hash
}

// DispatchResult can be returned from dispatchable functions
type DispatchResult struct {
	Ok    bool
	Error DispatchError
}

func (d *DispatchResult) Decode(decoder scale.Decoder) error {
	b, err := decoder.ReadOneByte()
	if err != nil {
		return err
	}

	switch b {
	case 0:
		d.Ok = true
		return nil
	default:
		derr := DispatchError{}
		err = decoder.Decode(&derr)
		if err != nil {
			return err
		}
		d.Error = derr
		return nil
	}
}

func (d DispatchResult) Encode(encoder scale.Encoder) error {
	if d.Ok {
		return encoder.PushByte(0)
	}
	return d.Error.Encode(encoder)
}

// EventUtility is emitted when a multisig operation has been executed. First param is the account that is
// approving, third is the multisig account, fourth is hash of the call to be executed.
type EventMultisigExecuted struct {
	Phase     Phase
	Who       AccountID
	TimePoint TimePoint
	ID        AccountID
	CallHash  Hash
	Result    DispatchResult
	Topics    []Hash
}

// EventUtility is emitted when a multisig operation has been cancelled. First param is the account that is
// cancelling, third is the multisig account, fourth is hash of the call.
type EventMultisigCancelled struct {
	Phase     Phase
	Who       AccountID
	TimePoint TimePoint
	ID        AccountID
	CallHash  Hash
	Topics    []Hash
}

// COPY FROM GSRPC END

type EventAuthorFilterEligibleUpdated struct {
	Phase  types.Phase
	Amount Percent
	Topics []types.Hash
}

type EventAuthorMappingAuthorDeRegistered struct {
	Phase    types.Phase
	AuthorId AuthorId
	Topics   []types.Hash
}
type EventAuthorMappingAuthorRegistered struct {
	Phase     types.Phase
	AuthorId  AuthorId
	AccountId AccountID
	Topics    []types.Hash
}
type EventAuthorMappingAuthorRotated struct {
	Phase     types.Phase
	AuthorId  AuthorId
	AccountId AccountID
	Topics    []types.Hash
}
type EventAuthorMappingDefunctAuthorBusted struct {
	Phase  types.Phase
	Topics []types.Hash
}

type EventCrowdloanRewardsInitialPaymentMade struct {
	Phase     types.Phase
	AccountId AccountID
	Amount    Balance
	Topics    []types.Hash
}
type EventCrowdloanRewardsInitializedAccountWithNotEnoughContribution struct {
	Phase               types.Phase
	RelaychainAccountId RelaychainAccountId
	NativeAccount       AccountID
	Rewards             Balance
	Topics              []types.Hash
}
type EventCrowdloanRewardsInitializedAlreadyInitializedAccount struct {
	Phase               types.Phase
	RelaychainAccountId RelaychainAccountId
	NativeAccount       AccountID
	Rewards             Balance
	Topics              []types.Hash
}
type EventCrowdloanRewardsNativeIdentityAssociated struct {
	Phase          types.Phase
	RelayAccountId AccountID
	NativeAccount  AccountID
	Rewards        Balance
	Topics         []types.Hash
}
type EventCrowdloanRewardsRewardAddressUpdated struct {
	Phase      types.Phase
	OldAccount AccountID
	NewAccount AccountID
	Topics     []types.Hash
}
type EventCrowdloanRewardsRewardsPaid struct {
	Phase   types.Phase
	Account AccountID
	Rewards Balance
	Topics  []types.Hash
}
type EventParachainStakingBlocksPerRoundSet struct {
	Phase                 types.Phase
	CurrentRound          RoundIndex
	FirstBlock            BlockNumber
	Old                   types.U32
	New                   types.U32
	NewPerRoundInflation1 Perbill
	NewPerRoundInflation2 Perbill
	NewPerRoundInflation3 Perbill
	Topics                []types.Hash
}
type EventParachainStakingCollatorBackOnline struct {
	Phase    types.Phase
	Round    RoundIndex
	Collator AccountID
	Topics   []types.Hash
}
type EventParachainStakingCollatorBondedLess struct {
	Phase           types.Phase
	CollatorAccount AccountID
	OldBond         Balance
	NewBond         Balance
	Topics          []types.Hash
}
type EventParachainStakingCollatorBondedMore struct {
	Phase           types.Phase
	CollatorAccount AccountID
	OldBond         Balance
	NewBond         Balance
	Topics          []types.Hash
}
type EventParachainStakingCollatorChosen struct {
	Phase              types.Phase
	Round              RoundIndex
	CollatorAccount    AccountID
	TotalExposedAmount Balance
	Topics             []types.Hash
}
type EventParachainStakingCollatorCommissionSet struct {
	Phase  types.Phase
	Old    Perbill
	New    Perbill
	Topics []types.Hash
}
type EventParachainStakingCollatorLeft struct {
	Phase             types.Phase
	Account           AccountID
	Unlocked          Balance
	NewTotalAmtLocked Balance
	Topics            []types.Hash
}
type EventParachainStakingCollatorScheduledExit struct {
	Phase           types.Phase
	Round           RoundIndex
	CollatorAccount Balance
	ScheduledExit   RoundIndex
	Topics          []types.Hash
}
type EventParachainStakingCollatorWentOffline struct {
	Phase    types.Phase
	Round    RoundIndex
	Collator AccountID
	Topics   []types.Hash
}
type EventParachainStakingInflationSet struct {
	Phase                 types.Phase
	AnnualInflationInput1 Perbill
	AnnualInflationInput2 Perbill
	AnnualInflationInput3 Perbill
	AnnualInflationInput4 Perbill
	AnnualInflationInput5 Perbill
	AnnualInflationInput6 Perbill
	Topics                []types.Hash
}
type EventParachainStakingJoinedCollatorCandidates struct {
	Phase             types.Phase
	Account           AccountID
	AmountLocked      Balance
	NewTotalAmtLocked Balance
	Topics            []types.Hash
}
type EventParachainStakingNewRound struct {
	Phase                     types.Phase
	StartBlock                BlockNumber
	Round                     RoundIndex
	NumberOfCollatorsSelected types.U32
	TotalBalance              Balance
	Topics                    []types.Hash
}
type EventParachainStakingNomination struct {
	Phase                      types.Phase
	Nominator                  AccountID
	AmountLocked               Balance
	Collator                   AccountID
	NewTotalAmtBackingCollator Balance
	Topics                     []types.Hash
}
type EventParachainStakingNominationDecreased struct {
	Phase         types.Phase
	Nominator     AccountID
	Collator      AccountID
	OldNomination Balance
	Bool          bool
	NewNomination Balance
	Topics        []types.Hash
}
type EventParachainStakingNominationIncreased struct {
	Phase         types.Phase
	Nominator     AccountID
	Collator      AccountID
	OldNomination Balance
	Bool          bool
	NewNomination Balance
	Topics        []types.Hash
}
type EventParachainStakingNominatorLeft struct {
	Phase          types.Phase
	Nominator      AccountID
	AmountUnstaked Balance
	Topics         []types.Hash
}
type EventParachainStakingNominatorLeftCollator struct {
	Phase                        types.Phase
	Nominator                    AccountID
	Collator                     AccountID
	AmountUnstaked               Balance
	NewTotalAmtStakedForCollator Balance
	Topics                       []types.Hash
}
type EventParachainStakingParachainBondAccountSet struct {
	Phase  types.Phase
	Old    AccountID
	New    AccountID
	Topics []types.Hash
}
type EventParachainStakingParachainBondReservePercentSet struct {
	Phase  types.Phase
	Old    Percent
	New    Percent
	Topics []types.Hash
}
type EventParachainStakingReservedForParachainBond struct {
	Phase   types.Phase
	Account AccountID
	Amount  Balance
	Topics  []types.Hash
}
type EventParachainStakingRewarded struct {
	Phase   types.Phase
	Account AccountID
	Amount  Balance
	Topics  []types.Hash
}
type EventParachainStakingStakeExpectationsSet struct {
	Phase  types.Phase
	Arg1   Balance
	Arg2   Balance
	Arg3   Balance
	Topics []types.Hash
}
type EventParachainStakingTotalSelectedSet struct {
	Phase  types.Phase
	Old    types.U32
	New    types.U32
	Topics []types.Hash
}

type EventParachainStakingNominatorExitScheduled struct {
	Phase         Phase
	Round         RoundIndex
	Nominator     AccountID
	ScheduledExit RoundIndex
	Topics        []Hash
}

type EventParachainStakingNominationRevocationScheduled struct {
	Phase         Phase
	Round         RoundIndex
	Nominator     AccountID
	Collator      AccountID
	ScheduledExit RoundIndex
	Topics        []Hash
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

func NewMoonRiverEventRecord(sdr *substrate.SubstrateStorageDataRaw, meta *substrate.SubstrateMetaData) *MoonriverEventRecord {
	records := &MoonriverEventRecord{}
	if err := substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		log.Debugf("NewMoonRiverEventRecord decode fails: %v", err)
		return nil
	}

	return records
}

type MoonriverEventRecord struct {
	frontier.FrontierEventRecord
	AuthorFilter_EligibleUpdated                                 []EventAuthorFilterEligibleUpdated
	AuthorMapping_AuthorDeRegistered                             []EventAuthorMappingAuthorDeRegistered
	AuthorMapping_AuthorRegistered                               []EventAuthorMappingAuthorRegistered
	AuthorMapping_AuthorRotated                                  []EventAuthorMappingAuthorRotated
	AuthorMapping_DefunctAuthorBusted                            []EventAuthorMappingDefunctAuthorBusted
	Balances_BalanceSet                                          []EventBalancesBalanceSet
	Balances_Deposit                                             []EventBalancesDeposit
	Balances_DustLost                                            []EventBalancesDustLost
	Balances_Endowed                                             []EventBalancesEndowed
	Balances_ReserveRepatriated                                  []EventBalancesReserveRepatriated
	Balances_Reserved                                            []EventBalancesReserved
	Balances_Transfer                                            []EventBalancesTransfer
	Balances_Unreserved                                          []EventBalancesUnreserved
	CouncilCollective_Approved                                   []EventCouncilCollectiveApproved
	CouncilCollective_Closed                                     []EventCouncilCollectiveClosed
	CouncilCollective_Disapproved                                []EventCouncilCollectiveDisapproved
	CouncilCollective_Executed                                   []EventCouncilCollectiveExecuted
	CouncilCollective_MemberExecuted                             []EventCouncilCollectiveMemberExecuted
	CouncilCollective_Proposed                                   []EventCouncilCollectiveProposed
	CouncilCollective_Voted                                      []EventCouncilCollectiveVoted
	CrowdloanRewards_InitialPaymentMade                          []EventCrowdloanRewardsInitialPaymentMade
	CrowdloanRewards_InitializedAccountWithNotEnoughContribution []EventCrowdloanRewardsInitializedAccountWithNotEnoughContribution
	CrowdloanRewards_InitializedAlreadyInitializedAccount        []EventCrowdloanRewardsInitializedAlreadyInitializedAccount
	CrowdloanRewards_NativeIdentityAssociated                    []EventCrowdloanRewardsNativeIdentityAssociated
	CrowdloanRewards_RewardAddressUpdated                        []EventCrowdloanRewardsRewardAddressUpdated
	CrowdloanRewards_RewardsPaid                                 []EventCrowdloanRewardsRewardsPaid
	Democracy_Blacklisted                                        []EventDemocracyBlacklisted
	Democracy_Cancelled                                          []EventDemocracyCancelled
	Democracy_Delegated                                          []EventDemocracyDelegated
	Democracy_Executed                                           []EventDemocracyExecuted
	Democracy_ExternalTabled                                     []EventDemocracyExternalTabled
	Democracy_NotPassed                                          []EventDemocracyNotPassed
	Democracy_Passed                                             []EventDemocracyPassed
	Democracy_PreimageInvalid                                    []EventDemocracyPreimageInvalid
	Democracy_PreimageMissing                                    []EventDemocracyPreimageMissing
	Democracy_PreimageNoted                                      []EventDemocracyPreimageNoted
	Democracy_PreimageReaped                                     []EventDemocracyPreimageReaped
	Democracy_PreimageUsed                                       []EventDemocracyPreimageUsed
	Democracy_Proposed                                           []EventDemocracyProposed
	Democracy_Started                                            []EventDemocracyStarted
	Democracy_Tabled                                             []EventDemocracyTabled
	Democracy_Undelegated                                        []EventDemocracyUndelegated
	Democracy_Unlocked                                           []EventDemocracyUnlocked
	Democracy_Vetoed                                             []EventDemocracyVetoed
	ParachainStaking_BlocksPerRoundSet                           []EventParachainStakingBlocksPerRoundSet
	ParachainStaking_CollatorBackOnline                          []EventParachainStakingCollatorBackOnline
	ParachainStaking_CollatorBondedLess                          []EventParachainStakingCollatorBondedLess
	ParachainStaking_CollatorBondedMore                          []EventParachainStakingCollatorBondedMore
	ParachainStaking_CollatorChosen                              []EventParachainStakingCollatorChosen
	ParachainStaking_CollatorCommissionSet                       []EventParachainStakingCollatorCommissionSet
	ParachainStaking_CollatorLeft                                []EventParachainStakingCollatorLeft
	ParachainStaking_CollatorScheduledExit                       []EventParachainStakingCollatorScheduledExit
	ParachainStaking_CollatorWentOffline                         []EventParachainStakingCollatorWentOffline
	ParachainStaking_InflationSet                                []EventParachainStakingInflationSet
	ParachainStaking_JoinedCollatorCandidates                    []EventParachainStakingJoinedCollatorCandidates
	ParachainStaking_NewRound                                    []EventParachainStakingNewRound
	ParachainStaking_Nomination                                  []EventParachainStakingNomination
	ParachainStaking_NominationDecreased                         []EventParachainStakingNominationDecreased
	ParachainStaking_NominationIncreased                         []EventParachainStakingNominationIncreased
	ParachainStaking_NominatorExitScheduled                      []EventParachainStakingNominatorExitScheduled
	ParachainStaking_NominationRevocationScheduled               []EventParachainStakingNominationRevocationScheduled
	ParachainStaking_NominatorLeft                               []EventParachainStakingNominatorLeft
	ParachainStaking_NominatorLeftCollator                       []EventParachainStakingNominatorLeftCollator
	ParachainStaking_ParachainBondAccountSet                     []EventParachainStakingParachainBondAccountSet
	ParachainStaking_ParachainBondReservePercentSet              []EventParachainStakingParachainBondReservePercentSet
	ParachainStaking_ReservedForParachainBond                    []EventParachainStakingReservedForParachainBond
	ParachainStaking_Rewarded                                    []EventParachainStakingRewarded
	ParachainStaking_StakeExpectationsSet                        []EventParachainStakingStakeExpectationsSet
	ParachainStaking_TotalSelectedSet                            []EventParachainStakingTotalSelectedSet
	ParachainSystem_DownwardMessagesProcessed                    []EventParachainSystemDownwardMessagesProcessed
	ParachainSystem_DownwardMessagesReceived                     []EventParachainSystemDownwardMessagesReceived
	ParachainSystem_UpgradeAuthorized                            []EventParachainSystemUpgradeAuthorized
	ParachainSystem_ValidationFunctionApplied                    []EventParachainSystemValidationFunctionApplied
	ParachainSystem_ValidationFunctionStored                     []EventParachainSystemValidationFunctionStored
	Proxy_Announced                                              []EventProxyAnnounced
	Proxy_AnonymousCreated                                       []EventProxyAnonymousCreated
	Proxy_ProxyExecuted                                          []EventProxyProxyExecuted
	Scheduler_Canceled                                           []EventSchedulerCanceled
	Scheduler_Dispatched                                         []EventSchedulerDispatched
	Scheduler_Scheduled                                          []EventSchedulerScheduled
	Sudo_KeyChanged                                              []EventSudoKeyChanged
	Sudo_Sudid                                                   []EventSudoSudid
	Sudo_SudoAsDone                                              []EventSudoAsDone
	System_CodeUpdated                                           []EventSystemCodeUpdated
	System_ExtrinsicFailed                                       []EventSystemExtrinsicFailed
	System_ExtrinsicSuccess                                      []EventSystemExtrinsicSuccess
	System_KilledAccount                                         []EventSystemKilledAccount
	System_NewAccount                                            []EventSystemNewAccount
	System_Remarked                                              []EventSystemRemarked
	TechComitteeCollective_Approved                              []EventTechComitteeCollectiveApproved
	TechComitteeCollective_Closed                                []EventTechComitteeCollectiveClosed
	TechComitteeCollective_Disapproved                           []EventTechComitteeCollectiveDisapproved
	TechComitteeCollective_Executed                              []EventTechComitteeCollectiveExecuted
	TechComitteeCollective_MemberExecuted                        []EventTechComitteeCollectiveMemberExecuted
	TechComitteeCollective_Proposed                              []EventTechComitteeCollectiveProposed
	TechComitteeCollective_Voted                                 []EventTechComitteeCollectiveVoted
	Treasury_Awarded                                             []EventTreasuryAwarded
	Treasury_Burnt                                               []EventTreasuryBurnt
	Treasury_Deposit                                             []EventTreasuryDeposit
	Treasury_Proposed                                            []EventTreasuryProposed
	Treasury_Rejected                                            []EventTreasuryRejected
	Treasury_Rollover                                            []EventTreasuryRollover
	Treasury_Spending                                            []EventTreasurySpending
	Utility_BatchCompleted                                       []EventUtilityBatchCompleted
	Utility_BatchInterrupted                                     []EventUtilityBatchInterrupted
}

func (me *MoonriverEventRecord) Len() int {
	len := 0
	v := reflect.ValueOf(*me)
	for i := 0; i < v.NumField(); i++ {
		switch v.Field(i).Kind() {
		case reflect.Slice:
			len += v.Field(i).Len()
		}
	}
	return len
}
