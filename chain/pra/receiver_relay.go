package pra

import (
	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
)

type relayReceiver struct {
	c               substrate.SubstrateClient
	bmvC            icon.PraBmvClient
	bmvStatus       icon.PraBmvStatus
	expectMtaHeight uint64 // keep track of localRelayMtaHeight per paraFinalityProof
	expectSetId     uint64
	store           *mta.ExtAccumulator
	log             log.Logger
}

func NewRelayReceiver(opt receiverOptions, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log}
	var err error
	rC.c, err = substrate.NewSubstrateClient(opt.RelayEndpoint)
	if err != nil {
		log.Panic("fail to connect to relay endpoint %v", err)
	}

	rC.c.Init()
	rC.bmvC = icon.NewPraBmvClient(opt.DstEndpoint, opt.PraBmvAddress.ContractAddress(), log)
	rC.bmvStatus = rC.bmvC.GetPraBmvStatus()
	rC.prepareDatabase(int64(rC.bmvStatus.RelayMtaOffset), opt.MtaRootSize, opt.AbsBaseDir(), opt.RelayBtpAddress.NetworkAddress())
	return rC
}

func (r *relayReceiver) findParasInclusionCandidateIncludedHead(from uint64, paraHead substrate.SubstrateHash, paraChainId substrate.SubstrateParachainId) (*substrate.SubstrateHeader, *substrate.SubstrateHash) {
	r.log.Debugf("findParasInclusionCandidateIncludedHead: from %d paraHead: %s", from, paraHead.Hex())
	blockNumber := from
	for {
		blockHash, err := r.c.GetBlockHash(blockNumber)
		if err != nil {
			r.log.Panic(errors.Wrapf(err, "findParasInclusionCandidateIncludedHead: can't get blockhash %d", blockNumber))
		}

		eventParasInclusionCandidateIncluded, err := r.getParasInclusionCandidateIncluded(blockHash)
		if err != nil {
			r.log.Panic(errors.Wrapf(err, "findParasInclusionCandidateIncludedHead: can't get events %s", blockHash.Hex()))
		}

		if len(eventParasInclusionCandidateIncluded) > 0 {
			for _, event := range eventParasInclusionCandidateIncluded {
				// parachain include must match id and parahead
				if event.CandidateReceipt.Descriptor.ParaId == paraChainId &&
					event.CandidateReceipt.Descriptor.ParaHead == paraHead {

					header, err := r.c.GetHeader(blockHash)
					if err != nil {
						r.log.Panicf("findParasInclusionCandidateIncludedHead: can't get header %s", blockHash.Hex())
					}

					r.log.Debugf("findParasInclusionCandidateIncludedHead: found at relayblock %d", header.Number)
					return header, &blockHash
				}
			}
		}

		blockNumber++
	}
}

func (r *relayReceiver) buildBlockUpdates(nexMtaHeight uint64, gj *substrate.GrandpaJustification, fetchtedBlockHeaders []substrate.SubstrateHeader) ([][]byte, error) {
	bus := make([][]byte, 0)
	blockHeaders := make([]substrate.SubstrateHeader, 0)

	// Fetch headers with fetched blockheaders.
	from := nexMtaHeight
	to := uint64(gj.Commit.TargetNumber)

	if len(fetchtedBlockHeaders) > 0 {
		from = uint64(fetchtedBlockHeaders[len(fetchtedBlockHeaders)-1].Number)

		// not need to fetch again
		if from == to {
			to -= 1
		}

		nextMtaHash, err := r.c.GetBlockHash(nexMtaHeight)
		if err != nil {
			return nil, err
		}

		nextMtaBlockHeader, err := r.c.GetHeader(nextMtaHash)
		if err != nil {
			return nil, err
		}

		blockHeaders = append(blockHeaders, *nextMtaBlockHeader)
	}

	misisingBlockNumbers := make([]substrate.SubstrateBlockNumber, 0)

	for i := from; i <= to; i++ {
		misisingBlockNumbers = append(misisingBlockNumbers, substrate.SubstrateBlockNumber(i))
	}

	missingBlockHeaders, err := r.c.GetBlockHeaderByBlockNumbers(misisingBlockNumbers)
	if err != nil {
		return nil, err
	}

	blockHeaders = append(blockHeaders, fetchtedBlockHeaders...)
	blockHeaders = append(blockHeaders, missingBlockHeaders...)

	for i, blockHeader := range blockHeaders {
		var bu []byte
		var votes []byte = nil
		if i == len(blockHeaders)-1 {
			votes, err = r.newVotes(gj, substrate.SetId(r.expectSetId))
			if err != nil {
				return nil, err
			}
		}

		bu, err = r.newBlockUpdate(blockHeader, votes)
		if err != nil {
			return nil, err
		}

		r.log.Tracef("buildBlockUpdates: %d", blockHeader.Number)

		// Sync MTA
		r.updateMta(uint64(blockHeader.Number-1), blockHeader.ParentHash)
		r.expectMtaHeight = uint64(blockHeader.Number - 1)

		bus = append(bus, bu)
	}

	// Sync MTA
	relayBlockhash, err := r.c.GetBlockHash(uint64(blockHeaders[len(blockHeaders)-1].Number))
	if err != nil {
		return nil, err
	}

	r.updateMta(uint64(blockHeaders[len(blockHeaders)-1].Number), relayBlockhash)
	r.expectMtaHeight = uint64(blockHeaders[len(blockHeaders)-1].Number)

	r.log.Debugf("buildBlockUpdates: %d ~ %d", blockHeaders[0].Number, blockHeaders[len(blockHeaders)-1].Number)
	return bus, nil
}

func (r *relayReceiver) buildFinalityProof(includeHeader *substrate.SubstrateHeader, includeHash *substrate.SubstrateHash) ([][]byte, error) {
	finalityProofs := make([][]byte, 0)

	r.log.Debugf("buildFinalityProof: remoteRelayMtaHeight: %d", r.bmvStatus.RelayMtaHeight)
	if r.expectMtaHeight < uint64(r.bmvStatus.RelayMtaHeight) {
		r.expectMtaHeight = uint64(r.bmvStatus.RelayMtaHeight)
		r.expectSetId = uint64(r.bmvStatus.RelaySetId)
	}

	r.log.Debugf("buildFinalityProof: localExpectRelayMtaHeight: %d", r.expectMtaHeight)
	// For blockproof to work
	for r.expectMtaHeight < uint64(includeHeader.Number) {
		stateProofs := make([][]byte, 0)

		gj, bhs, err := r.c.GetJustificationsAndUnknownHeaders(substrate.SubstrateBlockNumber(r.expectMtaHeight + 1))
		if err != nil {
			return nil, err
		}

		r.log.Debugf("buildFinalityProof: found justification at %d by %d", gj.Commit.TargetNumber, r.expectMtaHeight+1)
		blockUpdates, err := r.buildBlockUpdates(uint64(r.expectMtaHeight+1), gj, bhs)
		if err != nil {
			return nil, err
		}

		// Update expectMta for next message
		r.expectMtaHeight = uint64(gj.Commit.TargetNumber)

		eventGrandpaNewAuthorities, err := r.c.GetSystemEvents(gj.Commit.TargetHash, "Grandpa", "NewAuthorities")
		if err != nil {
			return nil, err
		}

		if len(eventGrandpaNewAuthorities) > 0 {
			r.log.Debugf("buildFinalityProof: found GrandpaNewAuthorities %d", gj.Commit.TargetNumber)
			newAuthoritiesStateProof, err := r.newStateProof(gj.Commit.TargetHash)
			if err != nil {
				return nil, err
			}

			stateProofs = append(stateProofs, newAuthoritiesStateProof)
			r.expectSetId++
		}

		r.log.Tracef("newFinalityProofs: lastBlock at %d", r.expectMtaHeight)
		finalityProof, err := r.newFinalityProof(blockUpdates, stateProofs, []byte{})
		if err != nil {
			return nil, err
		}

		finalityProofs = append(finalityProofs, finalityProof)

		// early exit and only need one StateProof
		if (r.expectMtaHeight) == uint64(includeHeader.Number) && len(eventGrandpaNewAuthorities) > 0 {
			r.log.Debugf("buildFinalityProof: GrandpaNewAuthorities and LastBlock is equal at %d", gj.Commit.TargetNumber)
			return finalityProofs, nil
		}
	}

	bp, err := r.newBlockProof(int64(includeHeader.Number), int64(r.expectMtaHeight))
	if err != nil {
		return nil, err
	}

	includedStateProof, err := r.newStateProof(*includeHash)
	if err != nil {
		return nil, err
	}

	stateProofs := [][]byte{includedStateProof}

	finalityProof, err := r.newFinalityProof(nil, stateProofs, bp)
	if err != nil {
		return nil, err
	}

	r.log.Tracef("newFinalityProofs: blockProof and stateProof at %d", includeHeader.Number)
	finalityProofs = append(finalityProofs, finalityProof)
	return finalityProofs, nil
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData, paraChainId substrate.SubstrateParachainId, paraHead substrate.SubstrateHash, paraHeight uint64) ([][]byte, error) {
	r.bmvStatus = r.bmvC.GetPraBmvStatus()
	if paraHeight <= uint64(r.bmvStatus.ParaMtaHeight) {
		r.log.Tracef("newParaFinalityProof: paraHeight smaller than paraMtaHeight %d", r.bmvStatus.ParaMtaHeight)
		return nil, nil
	}

	includeHeader, includeHash := r.findParasInclusionCandidateIncludedHead(uint64(vd.RelayParentNumber), paraHead, paraChainId)

	if uint64(includeHeader.Number) < r.bmvC.GetRelayMtaOffset() {
		r.log.Panicf("newParaFinalityProof: includeHeader %d <= relayMtaOffset %d", uint64(includeHeader.Number), r.bmvC.GetRelayMtaOffset())
	}

	localRelayMtaHeight := r.store.Height()

	// Sync MTA completely
	// TODO fetch with runtime.GOMAXPROCS(runtime.NumCPU()) hashes per call
	// sort these hashses, and add to MTA one by one
	if localRelayMtaHeight < r.bmvStatus.RelayMtaHeight {
		r.log.Tracef("newParaFinalityProof: sync localRelayMtaHeight %d with remoteRelayMtaHeight %d", localRelayMtaHeight, r.bmvStatus.RelayMtaHeight)
		for i := localRelayMtaHeight + 1; i <= r.bmvStatus.RelayMtaHeight; i++ {
			relayHash, _ := r.c.GetBlockHash(uint64(i))
			r.updateMta(uint64(i), relayHash)
		}
	}

	return r.buildFinalityProof(includeHeader, includeHash)
}
