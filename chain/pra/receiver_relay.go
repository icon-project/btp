package pra

import (
	"fmt"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/kusama"
	"github.com/icon-project/btp/chain/pra/relaychain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/chain/pra/westend"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
)

type relayReceiver struct {
	c                          substrate.SubstrateClient
	pC                         praBmvClient
	paraChainId                substrate.SubstrateParachainId
	lastJustificationCollected *substrate.SubstrateBlockNumber
	log                        log.Logger
}

func NewRelayReceiver(opt receiverOptions, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log, paraChainId: 1000}
	rC.c, _ = substrate.NewSubstrateClient(opt.RelayEndpoint)
	rC.pC = NewPraBmvClient(
		opt.IconEndpoint, log, opt.PraBmvAddress,
		NewRelayStoreConfig(opt.AbsBaseDir(), opt.RelayBtpAddress.NetworkAddress()),
	)

	rC.pC.prepareDatabase(opt.RelayOffSet)
	return rC
}

func (r *relayReceiver) syncBlocks(blockHeader *substrate.SubstrateHeader, gc *substrate.GrandpaCommit) {
	next := r.pC.store.Height() + 1

	if gc != nil {
		if next < int64(gc.TargetNumber) {
			r.log.Fatalf("syncBlocks: found missing block next:%d bu:%d", next, gc.TargetNumber-1)
			return
		}
		if next == int64(gc.TargetNumber) {
			r.log.Debugf("syncBlocks: sync %d", gc.TargetNumber)
			r.pC.store.AddHash(gc.TargetHash[:])
			if err := r.pC.store.Flush(); err != nil {
				r.log.Fatalf("fail to MTA Flush err:%+v", err)
			}
		}
	} else if blockHeader != nil {
		if next < int64(blockHeader.Number-1) {
			r.log.Fatalf("syncBlocks: found missing block next:%d bu:%d", next, blockHeader.Number-1)
			return
		}

		if next == int64(blockHeader.Number-1) {
			r.log.Debugf("syncBlocks: sync %d", blockHeader.Number-1)
			r.pC.store.AddHash(blockHeader.ParentHash[:])
			if err := r.pC.store.Flush(); err != nil {
				r.log.Fatalf("fail to MTA Flush err:%+v", err)
			}
		}
	} else {
		r.log.Fatalf("syncBlocks: don't have data to sync")
	}
}

// newBlockProof creates a new BlockProof
func (r *relayReceiver) newBlockProof(height int64, header []byte) ([]byte, error) {
	mtaHeight := r.pC.getRelayMtaHeight()
	mtaOffset := r.pC.getRelayMtaOffset()

	at, w, err := r.pC.store.WitnessForAt(height, int64(mtaHeight), int64(mtaOffset))
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

	b, err := codec.RLP.MarshalToBytes(bp)
	if err != nil {
		return nil, err
	}

	return b, nil
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

	vm := substrate.NewVoteMessage(justifications, setId)
	v.VoteMessage, err = substrate.NewEncodedVoteMessage(vm)
	if err != nil {
		return nil, err
	}

	// r.log.Tracef("newVotes: ScaleEncodedVoteMessage %x", v.VoteMessage)
	for _, precommit := range justifications.Commit.Precommits {
		vs := ValidatorSignature{
			Signature: precommit.Signature[:],
			Id:        precommit.Id[:],
		}

		bvs, err := codec.RLP.MarshalToBytes(&vs)
		if err != nil {
			return nil, err
		}

		v.Signatures = append(v.Signatures, bvs)
	}

	b, err := codec.RLP.MarshalToBytes(&v)
	if err != nil {
		return nil, err
	}

	r.log.Debugf("newVotes: at %s", justifications.Commit.TargetHash.Hex())
	return b, nil
}

func (r *relayReceiver) newBlockUpdate(header substrate.SubstrateHeader, justifications *substrate.GrandpaJustification) ([]byte, error) {
	var err error
	var update RelayBlockUpdate
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(header); err != nil {
		return nil, err
	}

	if justifications != nil {
		r.log.Debugf("newBlockUpdate: BlockUpdate with justification %d", header.Number)
		if update.Votes, err = r.newVotes(justifications); err != nil {
			return nil, err
		}
	}

	bu, err := codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}

	// r.log.Tracef("newBlockUpdate: at %d RLPEncodedBlockUpdate %x", header.Number, bu)
	return bu, nil
}

func (r *relayReceiver) newStateProof(blockHash substrate.SubstrateHash) ([]byte, error) {
	r.log.Debugf("newStateProof: at %s", blockHash.Hex())
	sp := make([]byte, 0)
	key, err := r.c.GetSystemEventStorageKey(blockHash)
	if err != nil {
		return nil, err
	}

	readProof, err := r.c.GetReadProof(key, blockHash)
	if err != nil {
		return nil, err
	}

	if sp, err = codec.RLP.MarshalToBytes(NewStateProof(key, &readProof)); err != nil {
		return nil, err
	}

	return sp, nil
}

func (r *relayReceiver) pullBlockHeaders(gj *substrate.GrandpaJustification, hds []substrate.SubstrateHeader) ([]substrate.SubstrateHeader, error) {
	bus := make([]substrate.SubstrateHeader, 0)

	from := hds[len(hds)-1].Number
	to := gj.Commit.TargetNumber

	r.log.Debugf("pullBlockHeaders: missing [%d ~ %d]", from, to)
	missingBlockNumbers := make([]substrate.SubstrateBlockNumber, 0)
	for i := from; i < substrate.NewBlockNumber(uint64(to)); i++ {
		missingBlockNumbers = append(missingBlockNumbers, i)
	}

	missingBlockHeaders, err := r.c.GetBlockHeaderByBlockNumbers(missingBlockNumbers)
	if err != nil {
		return nil, err
	}

	bus = append(bus, hds...)
	bus = append(bus, missingBlockHeaders...)

	r.log.Debugf("pullBlockHeaders: blockUpdates %d ~ %d", bus[0].Number, bus[len(bus)-1].Number)
	return bus, nil
}

func (r *relayReceiver) findParasInclusionCandidateIncludedHead(from uint64, paraHead substrate.SubstrateHash) (*substrate.SubstrateHeader, *substrate.SubstrateHash) {
	r.log.Debugf("findParasInclusionCandidateIncludedHead: from %d paraHead: %s", from, paraHead.Hex())
	foundEvent := false
	blockNumber := from
	for !foundEvent {
		blockHash, err := r.c.GetBlockHash(blockNumber)
		if err != nil {
			return nil, nil
		}

		eventParasInclusionCandidateIncluded, err := r.getParasInclusionCandidateIncluded(blockHash)
		if err != nil {
			return nil, nil
		}

		if len(eventParasInclusionCandidateIncluded) > 0 {
			for _, event := range eventParasInclusionCandidateIncluded {
				// parachain include must match id and parahead
				if event.CandidateReceipt.Descriptor.ParaId == r.paraChainId &&
					event.CandidateReceipt.Descriptor.ParaHead == paraHead {

					header, err := r.c.GetHeader(blockHash)
					if err != nil {
						return nil, nil
					}

					r.log.Debugf("findParasInclusionCandidateIncludedHead: found relayblock %d", header.Number)

					foundEvent = true
					return header, &blockHash
				}
			}
		}

		blockNumber++
	}

	return nil, nil
}

func (r *relayReceiver) getParasInclusionCandidateIncluded(blockHash substrate.SubstrateHash) ([]relaychain.EventParasInclusionCandidateIncluded, error) {
	meta := r.c.GetMetadataLatest()
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

	if r.c.GetSpecName() == substrate.Westend {
		records := westend.NewWestendEventRecord(sdr, meta)
		return records.ParasInclusion_CandidateIncluded, nil
	}

	return nil, err
}

func (r *relayReceiver) getGrandpaNewAuthorities(blockHash substrate.SubstrateHash) ([]relaychain.EventGrandpaNewAuthorities, error) {
	r.log.Debugf("getGrandpaNewAuthorities: %s", blockHash.Hex())
	meta := r.c.GetMetadataLatest()
	key, err := r.c.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := r.c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	spec := r.c.GetSpecName()

	if spec == substrate.Kusama {
		records := kusama.NewKusamaRecord(sdr, meta)
		return records.Grandpa_NewAuthorities, nil
	}

	if spec == substrate.Westend {
		records := westend.NewWestendEventRecord(sdr, meta)
		return records.Grandpa_NewAuthorities, nil
	}

	return nil, fmt.Errorf("Not supported relay spec %s", spec)
}

func (r *relayReceiver) didPullBlockUpdatesLastJustifications(paraIncludedHeaderNumber substrate.SubstrateBlockNumber) bool {
	if r.lastJustificationCollected == nil {
		return false
	}

	return paraIncludedHeaderNumber <= *r.lastJustificationCollected
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData, paraHead substrate.SubstrateHash, paraHeight uint64) ([]byte, error) {
	mtaHeight := r.pC.getRelayMtaHeight()
	paraMtaHeight := r.pC.getParaMtaHeight()

	// This doesn't need para finality proof, but para blockproof
	if paraMtaHeight == paraHeight {
		return []byte{0xf8, 0}, nil
	}

	r.log.Debugf("newParaFinalityProof: relayMtaHeight %d", mtaHeight)
	// check out which block para chain get included
	paraIncludedHeader, praIncludeBlockHash := r.findParasInclusionCandidateIncludedHead(uint64(vd.RelayParentNumber), paraHead)

	// Acccurate checking with MTA height
	if uint64(paraIncludedHeader.Number) <= mtaHeight {
		r.log.Panicf("newParaFinalityProof: skip relayblock %d, mtaHeight: %d", uint64(paraIncludedHeader.Number), mtaHeight)
	}

	bus := make([][]byte, 0)
	bp := []byte{}
	rps := make([][]byte, 0)

	// create stateproof for para chain get included
	paraIncludedStateProof, err := r.newStateProof(*praIncludeBlockHash)
	if err != nil {
		return nil, err
	}

	if mtaHeight < uint64(paraIncludedHeader.Number) && !r.didPullBlockUpdatesLastJustifications(paraIncludedHeader.Number) {
		// get the latest block contains justification
		gj, hds, err := r.c.GetJustificationsAndUnknownHeaders(substrate.NewBlockNumber(mtaHeight))
		if err != nil {
			return nil, err
		}

		lastBlockNumber := gj.Commit.TargetNumber
		r.log.Debugf("newParaFinalityProof: found justification at %d", lastBlockNumber)

		// pull all headers with unknowheader in finality proofs
		blockHeaders, err := r.pullBlockHeaders(gj, hds)
		if err != nil {
			return nil, err
		}

		for i, blockHeader := range blockHeaders {
			var bu []byte
			if i == len(blockHeaders)-1 {
				// add justification in last blocks
				bu, err = r.newBlockUpdate(blockHeader, gj)
				if err != nil {
					return nil, err
				}
			} else {
				bu, err = r.newBlockUpdate(blockHeader, nil)
				if err != nil {
					return nil, err
				}
			}

			if i != 0 {
				r.syncBlocks(&blockHeader, nil)
			}

			bus = append(bus, bu)
		}

		r.syncBlocks(nil, &gj.Commit)

		// check if last block contains Grandpa_NewAuthorities event
		eventGrandpaNewAuthorities, err := r.getGrandpaNewAuthorities(gj.Commit.TargetHash)
		if err != nil {
			return nil, nil
		}

		if len(eventGrandpaNewAuthorities) > 0 {
			// No need to create new state proof if same block
			if *praIncludeBlockHash != gj.Commit.TargetHash {
				newAuthoritiesStateProof, err := r.newStateProof(gj.Commit.TargetHash)
				if err != nil {
					return nil, err
				}

				rps = append(rps, newAuthoritiesStateProof)
			}
		} else {
			encodedHeader, err := substrate.NewEncodedSubstrateHeader(*paraIncludedHeader)
			if err != nil {
				return nil, err
			}

			bp, err = r.newBlockProof(int64(paraIncludedHeader.Number), encodedHeader)
			if err != nil {
				return nil, err
			}
			rps = append(rps, paraIncludedStateProof)
		}

		r.lastJustificationCollected = (*substrate.SubstrateBlockNumber)(&lastBlockNumber)

		// } else {
		// mtaSynced := false
		// for !mtaSynced {
		// newMtaHeight := r.pC.getRelayMtaHeight()
		// localMtaHeight := r.pC.store.Height()

		// if newMtaHeight > uint64(localMtaHeight) {
		// r.syncBlocks(int64(*r.lastJustificationCollected))
		// mtaSynced = true
		// break
		// }

		// r.log.Debugf("newParaFinalityProof: wait for mtaSync local %d, remote %d, paraInclude %d", localMtaHeight, newMtaHeight, paraIncludedHeader.Number)
		// time.Sleep(time.Second * 3)
		// }

		// encodedHeader, err := substrate.NewEncodedSubstrateHeader(*paraIncludedHeader)

		// if err != nil {
		// 	return nil, err
		// }

		// bp, err = r.newBlockProof(int64(paraIncludedHeader.Number), encodedHeader)
		// if err != nil {
		// 	return nil, err
		// }

		// rps = append(rps, paraIncludedStateProof)
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
