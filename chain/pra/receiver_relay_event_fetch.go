package pra

import (
	"github.com/icon-project/btp/chain/pra/substrate"
)

func (r *relayReceiver) getParasInclusionCandidateIncluded(blockHash substrate.SubstrateHash) ([]substrate.EventParasInclusionCandidateIncluded, error) {
	events, err := r.c.GetSystemEvents(blockHash, "ParaInclusion", "CandidateIncluded")
	r.log.Warnf("getParasInclusionCandidateIncluded blockHash:%s, events:%v, err:%v", blockHash, events, err)
	paraIncEvents := make([]substrate.EventParasInclusionCandidateIncluded, 0)
	for _, event := range events {
		paraIncEvents = append(paraIncEvents, substrate.NewEventParaInclusionCandidateIncluded(event))
	}
	return paraIncEvents, err
}
