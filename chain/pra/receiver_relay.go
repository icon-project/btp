package pra

import (
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/kusama"
	"github.com/icon-project/btp/chain/pra/relaychain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
)

type relayReceiver struct {
	c   substrate.SubstrateClient
	pC  praBmvClient
	log log.Logger
}

func NewRelayReceiver(opt receiverOptions, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log}
	rC.c, _ = substrate.NewSubstrateClient(opt.RelayEndpoint)
	rC.pC = NewPraBmvClient(
		opt.IconEndpoint, log, opt.PraBmvAddress,
		NewRelayStoreConfig("/Users/trmaphi/sources/btp/.config", "westend"),
	)

	rC.pC.prepareDatabase(opt.RelayOffSet)
	return rC
}

func (r *relayReceiver) syncBlock(mtaHeight int64) {
	next := r.pC.store.Height() + 1
	r.log.Debugf("syncBlock: with remote MTA %d, store  MTA %d", mtaHeight, next-1)

	if next < mtaHeight {
		r.log.Fatalf("found missing block next:%d bu:%d", next, mtaHeight)
		return
	}
	if next == mtaHeight {
		sh, err := r.c.GetBlockHash(uint64(mtaHeight))
		if err != nil {
			r.log.Fatalf("Fail to sync block %d", mtaHeight)
		}

		r.pC.store.AddHash(sh[:])
		if err := r.pC.store.Flush(); err != nil {
			r.log.Fatalf("fail to MTA Flush err:%+v", err)
		}
	}
}

// newBlockProof creates a new BlockProof
func (r *relayReceiver) newBlockProof(height int64, header []byte) (*chain.BlockProof, error) {
	mtaHeight := r.pC.getRelayMtaHeight()
	mtaOffset := r.pC.getRelayMtaOffset()

	at, w, err := r.pC.store.WitnessForAt(height, mtaHeight, mtaOffset)
	if err != nil {
		return nil, err
	}

	bp := &chain.BlockProof{
		Header: header,
		BlockWitness: &chain.BlockWitness{
			Height:  at,
			Witness: mta.WitnessesToHashes(w),
		},
	}

	r.log.Debugf("newBlockProof height:%d, at:%d, w:%x", height, at, bp.BlockWitness.Witness)
	return bp, nil
}

func (r *relayReceiver) newVotes(justifications *substrate.GrandpaJustification) ([]byte, error) {
	v := Votes{
		VoteMessage: make([]byte, 0),
		Signatures:  make([][]byte, 0),
	}

	setId, err := r.c.GetGrandpaCurrentSetId(justifications.Commit.TargetHash)
	if err != nil {
		return nil, err
	}

	vm := substrate.NewVoteMessage(justifications, *setId)
	v.VoteMessage, err = substrate.NewEncodedVoteMessage(vm)
	if err != nil {
		return nil, err
	}

	for _, precommit := range justifications.Commit.Precommits {
		v.Signatures = append(v.Signatures, precommit.Signature[:])
	}

	b, err := codec.RLP.MarshalToBytes(&v)
	if err != nil {
		return nil, err
	}

	return b, nil
}

func (r *relayReceiver) newBlockUpdate(header substrate.SubstrateHeader, justifications *substrate.GrandpaJustification) ([]byte, error) {
	var err error
	var update RelayBlockUpdate
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(header); err != nil {
		return nil, err
	}

	if justifications != nil {
		if update.Votes, err = r.newVotes(justifications); err != nil {
			return nil, err
		}
	}

	bu, err := codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}

	return bu, nil
}

func (r *relayReceiver) newStateProof(blockHash *substrate.SubstrateHash) ([]byte, error) {
	sp := make([]byte, 0)
	key, err := r.c.GetSystemEventStorageKey(*blockHash)
	if err != nil {
		return nil, err
	}

	readProof, err := r.c.GetReadProof(key, *blockHash)
	if err != nil {
		return nil, err
	}

	if sp, err = codec.RLP.MarshalToBytes(NewStateProof(key, &readProof)); err != nil {
		return nil, err
	}

	return sp, nil
}

func (r *relayReceiver) pullBlockUpdatesAndStateProofs(vd *substrate.PersistedValidationData, from uint64) ([][]byte, [][]byte, error) {
	bus := make([][]byte, 0)
	sps := make([][]byte, 0)

	foundEvent := false
	fp, err := r.c.GetFinalitiyProof(vd.RelayParentNumber)
	if err != nil {
		return nil, nil, err
	}

	for !foundEvent {
		bh, err := r.c.GetBlockHash(from)
		if err != nil {
			return nil, nil, err
		}

		bheader, err := r.c.GetHeader(bh)
		if err != nil {
			return nil, nil, err
		}

		bu, err := r.newBlockUpdate(*bheader, nil)
		if err != nil {
			return nil, nil, err
		}

		bus = append(bus, bu)

		if bh == fp.Justification.EncodedJustification.Commit.TargetHash {
			sp, err := r.newStateProof(&fp.Justification.EncodedJustification.Commit.TargetHash)
			if err != nil {
				return nil, nil, err
			}
			sps = append(sps, sp)
			foundEvent = true
		}
	}

	return bus, sps, nil
}

func (r *relayReceiver) pullBlockProofAndStateProofs(vd *substrate.PersistedValidationData, from uint64) ([]byte, [][]byte, error) {
	bp := []byte{}
	sps := make([][]byte, 0)

	foundEvent := false
	for !foundEvent {
		bh, err := r.c.GetBlockHash(from)
		if err != nil {
			return nil, nil, err
		}

		bheader, err := r.c.GetHeader(bh)
		if err != nil {
			return nil, nil, err
		}

		encodeHeader, err := substrate.NewEncodedSubstrateHeader(*bheader)
		if err != nil {
			return nil, nil, err
		}

		blockProof, err := r.newBlockProof(int64(bheader.Number), encodeHeader)
		if err != nil {
			return nil, nil, err
		}

		bp, err = codec.RLP.MarshalToBytes(blockProof)
		if err != nil {
			if err != nil {
				return nil, nil, err
			}
		}

		eventParasInclusionCandidateIncluded, err := r.getParasInclusionCandidateIncluded(bh)
		if err != nil {
			return nil, nil, err
		}

		if len(eventParasInclusionCandidateIncluded) > 0 {
			foundEvent = true
		}
	}

	return bp, sps, nil
}

func (r *relayReceiver) getParasInclusionCandidateIncluded(blockHash substrate.SubstrateHash) ([]relaychain.EventParasInclusionCandidateIncluded, error) {
	meta, err := r.c.GetMetadata(blockHash)
	if err != nil {
		return nil, err
	}

	key, err := r.c.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := r.c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	if r.c.GetSpecName() == substrate.Kusama {
		records := kusama.NewKusamaRecord(sdr, meta)
		return records.ParasInclusion_CandidateIncluded, nil
	}

	return nil, err
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData) ([]byte, error) {
	var from uint64
	mtaHeight := r.pC.getRelayMtaHeight()
	r.syncBlock(mtaHeight)

	behindMtaHeight := false
	if mtaHeight <= int64(vd.RelayParentNumber) {
		r.log.Debugf("over mta height %d, verifier.height %d", vd.RelayParentNumber, mtaHeight)
		from = uint64(mtaHeight)
	} else {
		r.log.Debugf("behind mta height %d, verifier.height %d", vd.RelayParentNumber, mtaHeight)
		behindMtaHeight = true
		from = uint64(vd.RelayParentNumber) + 1
	}

	bus := make([][]byte, 0)
	bp := []byte{}
	rps := make([][]byte, 0)

	if behindMtaHeight {
		var err error
		bp, rps, err = r.pullBlockProofAndStateProofs(vd, from)
		if err != nil {
			return nil, err
		}
	} else {
		var err error
		bus, rps, err = r.pullBlockUpdatesAndStateProofs(vd, from)
		if err != nil {
			return nil, err
		}
	}

	msg := &RelayMessage{
		BlockUpdates:  bus,
		BlockProof:    bp,
		ReceiptProofs: rps,
	}

	rmb, err := codec.RLP.MarshalToBytes(msg)
	if err != nil {
		return nil, err
	}

	return rmb, err
}
