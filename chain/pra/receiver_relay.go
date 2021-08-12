package pra

import (
	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
)

type relayReceiver struct {
	c               substrate.SubstrateClient
	bmvC            icon.PraBmvClient
	bmvStatus       icon.PraBmvStatus
	expectMtaHeight uint64 // keep track of MtaHeight per paraFinalityProof
	store           *mta.ExtAccumulator
	log             log.Logger
}

func NewRelayReceiver(opt receiverOptions, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log}
	rC.c, _ = substrate.NewSubstrateClient(opt.RelayEndpoint)
	rC.bmvC = icon.NewPraBmvClient(opt.DstEndpoint, opt.PraBmvAddress.ContractAddress(), log)
	rC.bmvStatus = icon.PraBmvStatus{
		RelayMtaHeight: int64(rC.bmvC.GetRelayMtaHeight()),
		RelayMtaOffset: int64(rC.bmvC.GetRelayMtaOffset()),
		RelaySetId:     int64(rC.bmvC.GetSetId()),
		ParaMtaHeight:  int64(rC.bmvC.GetParaMtaHeight()),
	}

	rC.prepareDatabase(int64(opt.RelayOffSet), opt.AbsBaseDir(), opt.RelayBtpAddress.NetworkAddress())
	return rC
}

func (r *relayReceiver) findParasInclusionCandidateIncludedHead(from uint64, paraHead substrate.SubstrateHash, paraChainId substrate.SubstrateParachainId) (*substrate.SubstrateHeader, *substrate.SubstrateHash) {
	r.log.Debugf("findParasInclusionCandidateIncludedHead: from %d paraHead: %s", from, paraHead.Hex())
	foundEvent := false
	blockNumber := from
	for !foundEvent {
		blockHash, err := r.c.GetBlockHash(blockNumber)
		if err != nil {
			r.log.Panicf("findParasInclusionCandidateIncludedHead: can't get blockhash %d", blockNumber)
		}

		eventParasInclusionCandidateIncluded, err := r.getParasInclusionCandidateIncluded(blockHash)
		if err != nil {
			r.log.Panicf("findParasInclusionCandidateIncludedHead: can't get events %s", blockHash.Hex())
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

	return nil, nil
}

func (r *relayReceiver) buildBlockUpdates(nexMtaHeight uint64, gj *substrate.GrandpaJustification, fetchtedBlockHeaders []substrate.SubstrateHeader) ([][]byte, error) {
	bus := make([][]byte, 0)
	blockHeaders := make([]substrate.SubstrateHeader, 0)

	// Fetch headers with fetched blockheaders.
	from := nexMtaHeight
	to := uint64(gj.Commit.TargetNumber)

	if len(fetchtedBlockHeaders) > 0 {
		from = uint64(fetchtedBlockHeaders[len(fetchtedBlockHeaders)-1].Number)

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
			votes, err = r.newVotes(gj, substrate.SetId(r.bmvStatus.RelaySetId))
			if err != nil {
				return nil, err
			}
		}

		bu, err = r.newBlockUpdate(blockHeader, votes)
		if err != nil {
			return nil, err
		}

		// Sync MTA
		r.updateMta(uint64(blockHeader.Number-1), blockHeader.ParentHash)
		r.expectMtaHeight = uint64(blockHeader.Number - 1)

		bus = append(bus, bu)
	}

	// Sync MTA
	relayBlockhash, err := r.c.GetBlockHash(to)
	if err != nil {
		return nil, err
	}

	r.updateMta(uint64(to), relayBlockhash)
	r.expectMtaHeight = uint64(to)

	r.log.Debugf("buildBlockUpdates: %d ~ %d", blockHeaders[0].Number, blockHeaders[len(blockHeaders)-1].Number)
	return bus, nil
}

func (r *relayReceiver) buildFinalityProof(includeHeader *substrate.SubstrateHeader, includeHash *substrate.SubstrateHash) ([][]byte, error) {
	finalityProofs := make([][]byte, 0)

	if r.expectMtaHeight < uint64(r.bmvStatus.RelayMtaHeight) {
		r.expectMtaHeight = uint64(r.bmvStatus.RelayMtaHeight)
	}

	// For blockproof to work
	for r.expectMtaHeight < uint64(includeHeader.Number) {
		bus := make([][]byte, 0)
		sps := make([][]byte, 0)

		gj, bhs, err := r.c.GetJustificationsAndUnknownHeaders(substrate.SubstrateBlockNumber(r.expectMtaHeight + 1))
		if err != nil {
			return nil, err
		}

		r.log.Debugf("buildFinalityProof: found justification at %d by %d", gj.Commit.TargetNumber, r.expectMtaHeight+1)

		newBus, err := r.buildBlockUpdates(uint64(r.expectMtaHeight+1), gj, bhs)
		if err != nil {
			return nil, err
		}

		bus = append(bus, newBus...)

		// Update expectMta for next message
		r.expectMtaHeight = uint64(gj.Commit.TargetNumber)

		eventGrandpaNewAuthorities, err := r.getGrandpaNewAuthorities(gj.Commit.TargetHash)
		if err != nil {
			return nil, err
		}

		if len(eventGrandpaNewAuthorities) > 0 {
			newAuthoritiesStateProof, err := r.newStateProof(gj.Commit.TargetHash)
			if err != nil {
				return nil, err
			}

			sps = append(sps, newAuthoritiesStateProof)
		}

		r.log.Tracef("newFinalityProofs: lastBlocks %d", r.expectMtaHeight)
		finalityProof, err := r.newFinalityProof(bus, sps, nil)
		if err != nil {
			return nil, err
		}

		finalityProofs = append(finalityProofs, finalityProof)

		// early exit and only need one StateProof
		if (r.expectMtaHeight) == uint64(includeHeader.Number) {
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

	sps := [][]byte{includedStateProof}

	finalityProof, err := r.newFinalityProof(nil, sps, bp)
	if err != nil {
		return nil, err
	}

	finalityProofs = append(finalityProofs, finalityProof)
	return finalityProofs, nil
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData, paraChainId substrate.SubstrateParachainId, paraHead substrate.SubstrateHash, paraHeight uint64) ([][]byte, error) {
	r.log.Tracef("newParaFinalityProof: paraBlock %d paraHead %s", paraHeight, paraHead.Hex())

	r.bmvStatus.RelayMtaHeight = int64(r.bmvC.GetRelayMtaHeight())
	r.bmvStatus.ParaMtaHeight = int64(r.bmvC.GetParaMtaHeight())

	// Sync MTA per pra block
	localRelayMtaHeight := r.store.Height()
	to := localRelayMtaHeight + 5
	if r.bmvStatus.RelayMtaHeight < to {
		to = r.bmvStatus.RelayMtaHeight
	}

	r.log.Tracef("newParaFinalityProof: relayMtaHeight %d", r.bmvStatus.RelayMtaHeight)
	if localRelayMtaHeight < r.bmvStatus.RelayMtaHeight {
		for i := localRelayMtaHeight + 1; i <= to; i++ {
			relayHash, _ := r.c.GetBlockHash(uint64(i))
			r.updateMta(uint64(i), relayHash)
		}
	}

	if paraHeight <= uint64(r.bmvStatus.ParaMtaHeight) {
		return nil, nil
	}

	includeHeader, includeHash := r.findParasInclusionCandidateIncludedHead(uint64(vd.RelayParentNumber), paraHead, paraChainId)

	if uint64(includeHeader.Number) < r.bmvC.GetRelayMtaOffset() {
		r.log.Panicf("newParaFinalityProof: includeHeader %d <= relayMtaOffset %d", uint64(includeHeader.Number), r.bmvC.GetRelayMtaOffset())
	}

	r.bmvStatus.RelaySetId = int64(r.bmvC.GetSetId())
	r.bmvStatus.RelayMtaOffset = int64(r.bmvC.GetRelayMtaOffset())

	return r.buildFinalityProof(includeHeader, includeHash)
}
