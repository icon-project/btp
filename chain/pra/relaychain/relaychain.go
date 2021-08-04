package relaychain

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type CandidateDescriptor struct {
	ParaId                      types.U32
	RelayParent                 types.Hash
	Collator                    types.AccountID
	PersistedValidationDataHash types.Hash
	PovHash                     types.Hash
	ErasureRoot                 types.Hash
	Signature                   types.Signature
	ParaHead                    types.Hash
	ValidationCodeHash          types.Hash
}
type CandidateReceipt struct {
	Descriptor      CandidateDescriptor
	CommitmentsHash types.Hash
}

type HeadData types.Bytes
type CoreIndex = types.U32
type GroupIndex = types.U32

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
	Topics           []types.Hash
}

type EventParasInclusionCandidateIncluded struct {
	Phase            types.Phase
	CandidateReceipt CandidateReceipt
	HeadData         HeadData
	CoreIndex        CoreIndex
	GroupIndex       GroupIndex
	Topics           []types.Hash
}

type EventParasInclusionCandidateTimedOut struct {
	Phase            types.Phase
	CandidateReceipt CandidateReceipt
	HeadData         HeadData
	CoreIndex        CoreIndex
	Topics           []types.Hash
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
}
