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

func NewRelayReceiver(relayEndpoint string, iconEndpoint string, bmvAddress string, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log}
	rC.c, _ = substrate.NewSubstrateClient(relayEndpoint)
	config := NewRelayStoreConfig("/Users/trmaphi/sources/btp/.config", "westend")
	rC.pC = NewPraBmvClient(iconEndpoint, log, bmvAddress, config)

	rC.pC.prepareDatabase()
	rC.syncBlock()
	return rC
}

func (rc *relayReceiver) syncBlock() {
	mtaHeight := rc.pC.getRelayMtaHeight()
	errCh := make(chan error)

	go func() {
		err := rc.c.SubcribeFinalizedHeadAt(uint64(mtaHeight), func(sh *substrate.SubstrateHash) {
			next := rc.pC.store.Height() + 1
			if next < mtaHeight {
				rc.log.Fatalf("found missing block next:%d bu:%d", next, mtaHeight)
				return
			}
			if next == mtaHeight {
				rc.pC.store.AddHash(sh[:])
				if err := rc.pC.store.Flush(); err != nil {
					//TODO MTA Flush error handling
					rc.log.Fatalf("fail to MTA Flush err:%+v", err)
				}
			}
		})

		select {
		case errCh <- err:
		default:
		}
	}()

	for {
		select {
		case err := <-errCh:
			if err != nil {
				rc.log.Panicf("fail to subscribe mta block")
			}
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

func (r *relayReceiver) newBlockUpdate(header substrate.SubstrateHeader, justifications *substrate.GrandpaJustification) ([]byte, error) {
	var err error
	var update RelayBlockUpdate
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(header); err != nil {
		return nil, err
	}

	bu, err := codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}

	return bu, nil
}

func (r *relayReceiver) pullBlockUpdatesAndStateProofs(vd *substrate.PersistedValidationData, from uint64) ([][]byte, [][]byte, error) {
	bus := make([][]byte, 0)
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

		bu, err := r.newBlockUpdate(*bheader, nil)
		if err != nil {
			return nil, nil, err
		}

		bus = append(bus, bu)

		_, _, err = r.getGrandpaNewAuthorityAndParasInclusionCandidateIncluded(bh)
		if err != nil {
			return nil, nil, err
		}

		foundEvent = true
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

		_, _, err = r.getGrandpaNewAuthorityAndParasInclusionCandidateIncluded(bh)
		if err != nil {
			return nil, nil, err
		}

		foundEvent = true
	}

	return bp, sps, nil
}

func (r *relayReceiver) getGrandpaNewAuthorityAndParasInclusionCandidateIncluded(blockHash substrate.SubstrateHash) ([]relaychain.EventGrandpaNewAuthorities, []relaychain.EventParasInclusionCandidateIncluded, error) {
	meta, err := r.c.GetMetadata(blockHash)
	if err != nil {
		return nil, nil, err
	}

	key, err := r.c.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, nil, err
	}

	sdr, err := r.c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, nil, err
	}

	// Build relay adapter here
	records := &kusama.KusamaEventRecord{}
	if err = substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		return nil, nil, err
	}

	return records.Grandpa_NewAuthorities, records.ParasInclusion_CandidateIncluded, nil
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData) ([]byte, error) {
	var from uint64
	mtaHeight := r.pC.getRelayMtaHeight()

	behindMtaHeight := false
	if mtaHeight <= int64(vd.RelayParentNumber) {
		from = uint64(mtaHeight)
	} else {
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
