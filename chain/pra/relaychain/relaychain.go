package relaychain

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type CandidateDescriptor struct {
	ParaId                      types.U32
	RelayParent                 Hash
	Collator                    types.AccountID
	PersistedValidationDataHash Hash
	PovHash                     Hash
	ErasureRoot                 Hash
	Signature                   types.Signature
	ParaHead                    Hash
	ValidationCodeHash          Hash
}
type CandidateReceipt struct {
	Descriptor      CandidateDescriptor
	CommitmentsHash Hash
}

type HeadData types.Bytes
type CoreIndex = types.U32
type GroupIndex = types.U32
type Phase = types.Phase
type Hash = types.Hash
type ParaId = types.U32
type AccountID = types.AccountID
type Balance = types.U128
type DispatchResult = []byte // FIXME
type Bytes = types.Bytes

func (cr *CandidateReceipt) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&cr.Descriptor)
	if err != nil {
		return err
	}

	err = decoder.Decode(&cr.CommitmentsHash)
	return err
}

type EventParasInclusionCandidateBacked struct {
	Phase            types.Phase
	CandidateReceipt CandidateReceipt
	HeadData         HeadData
	CoreIndex        CoreIndex
	GroupIndex       GroupIndex
	Topics           []Hash
}

type EventParasInclusionCandidateIncluded struct {
	Phase            types.Phase
	CandidateReceipt CandidateReceipt
	HeadData         HeadData
	CoreIndex        CoreIndex
	GroupIndex       GroupIndex
	Topics           []Hash
}

type EventParasInclusionCandidateTimedOut struct {
	Phase            types.Phase
	CandidateReceipt CandidateReceipt
	HeadData         HeadData
	CoreIndex        CoreIndex
	Topics           []Hash
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

type EventGrandpaNewAuthorities = types.EventGrandpaNewAuthorities
type EventGrandpaPaused = types.EventGrandpaPaused
type EventGrandpaResumed = types.EventGrandpaResumed

type RelayChainEventRecord struct {
	ParasInclusion_CandidateBacked   []EventParasInclusionCandidateBacked
	ParasInclusion_CandidateIncluded []EventParasInclusionCandidateIncluded
	ParasInclusion_CandidateTimedOut []EventParasInclusionCandidateTimedOut
	Grandpa_NewAuthorities           []EventGrandpaNewAuthorities
	Grandpa_Paused                   []EventGrandpaPaused
	Grandpa_Resumed                  []EventGrandpaResumed
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
	// Slots_NewLeasePeriod                            []EventSlotsNewLeasePeriod
	// Slots_Leased                                    []EventSlotsLeased
	// Auctions_AuctionStarted                         []EventAuctionsAuctionStarted
	// Auctions_AuctionClosed                          []EventAuctionsAuctionClosed
	// Auctions_Reserved                               []EventAuctionsReserved
	// Auctions_Unreserved                             []EventAuctionsUnreserved
	// Auctions_ReserveConfiscated                     []EventAuctionsReserveConfiscated
	// Auctions_BidAccepted                            []EventAuctionsBidAccepted
	// Auctions_WinningOffset                          []EventAuctionsWinningOffset
	Crowdloan_Created           []EventCrowdloanCreated
	Crowdloan_Contributed       []EventCrowdloanContributed
	Crowdloan_Withdrew          []EventCrowdloanWithdrew
	Crowdloan_PartiallyRefunded []EventCrowdloanPartiallyRefunded
	Crowdloan_AllRefunded       []EventCrowdloanAllRefunded
	Crowdloan_Dissolved         []EventCrowdloanDissolved
	Crowdloan_DeployDataFixed   []EventCrowdloanDeployDataFixed
	Crowdloan_Onboarded         []EventCrowdloanOnboarded
	Crowdloan_HandleBidResult   []EventCrowdloanHandleBidResult
	Crowdloan_Edited            []EventCrowdloanEdited
	Crowdloan_MemoUpdated       []EventCrowdloanMemoUpdated
	Crowdloan_AddedToNewRaise   []EventCrowdloanAddedToNewRaise
}
