package relaychain

import "github.com/centrifuge/go-substrate-rpc-client/v3/types"

type EventParasInclusionCandidateBacked struct {
	Phase  types.Phase
	Topics []types.Hash
}

type EventParasInclusionCandidateIncluded struct {
	Phase  types.Phase
	Topics []types.Hash
}

type EventParasInclusionCandidateTimedOut struct {
	Phase  types.Phase
	Topics []types.Hash
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
