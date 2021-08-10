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
	c         substrate.SubstrateClient
	pC        praBmvClient
	mtaHeight uint64
	mtaOffset uint64
	log       log.Logger
}

func NewRelayReceiver(opt receiverOptions, log log.Logger) relayReceiver {
	rC := relayReceiver{log: log}
	rC.c, _ = substrate.NewSubstrateClient(opt.RelayEndpoint)
	rC.pC = NewPraBmvClient(
		opt.IconEndpoint, log, opt.PraBmvAddress,
		NewRelayStoreConfig(opt.AbsBaseDir(), opt.RelayBtpAddress.NetworkAddress()),
	)

	rC.pC.prepareDatabase(opt.RelayOffSet)
	return rC
}

func (r *relayReceiver) syncBlocks(bn uint64, blockHash substrate.SubstrateHash) {
	next := r.pC.store.Height() + 1
	if next < int64(bn) {
		r.log.Fatalf("syncBlocks: found missing block next:%d bu:%d", next, bn)
		return
	}
	if next == int64(bn) {
		r.log.Debugf("syncBlocks: sync %d", bn)
		r.pC.store.AddHash(blockHash[:])
		if err := r.pC.store.Flush(); err != nil {
			r.log.Fatalf("fail to MTA Flush err:%+v", err)
		}
	}
}

// newBlockProof creates a new BlockProof
func (r *relayReceiver) newBlockProof(height int64, header []byte) ([]byte, error) {
	at, w, err := r.pC.store.WitnessForAt(height, int64(r.mtaHeight), int64(r.mtaOffset))
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

func (r *relayReceiver) newVotes(justifications *substrate.GrandpaJustification, setId substrate.SetId) ([]byte, error) {
	v := Votes{
		VoteMessage: make([]byte, 0),
		Signatures:  make([][]byte, 0),
	}

	vm := substrate.NewVoteMessage(justifications, setId)
	var err error
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

func (r *relayReceiver) newBlockUpdate(header substrate.SubstrateHeader, votes []byte) ([]byte, error) {
	var err error
	var update RelayBlockUpdate
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(header); err != nil {
		return nil, err
	}

	if votes != nil {
		r.log.Debugf("newBlockUpdate: BlockUpdate with votes %d", header.Number)
		update.Votes = votes
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

	return nil, fmt.Errorf("not supported relay spec %s", spec)
}

func (r *relayReceiver) newParaFinalityProof(vd *substrate.PersistedValidationData, paraChainId substrate.SubstrateParachainId, paraHead substrate.SubstrateHash, paraHeight uint64) ([]byte, error) {
	r.log.Tracef("newParaFinalityProof: paraBlock %d relayChainId %d", paraHeight, paraChainId)
	paraMtaHeight := r.pC.getParaMtaHeight()

	if paraHeight <= paraMtaHeight {
		r.log.Debugf("newParaFinalityProof: no need FinalityProof at paraBlock %d paraMtaHeight %d", paraHeight, paraMtaHeight)
		return []byte{0xf8, 0}, nil
	}

	remoteMtaHeight := r.pC.getRelayMtaHeight()
	remoteMtaOffet := r.pC.getRelayMtaOffset()
	remoteSetId := r.pC.getSetId()

	// Sync
	if r.mtaHeight < remoteMtaHeight {
		r.mtaHeight = remoteMtaHeight
		r.mtaOffset = remoteMtaOffet
	}

	r.log.Debugf("newParaFinalityProof: remoteMtaHeight %d remoteSetId %d", remoteMtaHeight, remoteSetId)

	// check out which block para chain get included
	paraIncludedHeader, praIncludeBlockHash := r.findParasInclusionCandidateIncludedHead(uint64(vd.RelayParentNumber), paraHead, paraChainId)
	if uint64(paraIncludedHeader.Number) <= remoteMtaOffet {
		r.log.Panicf("newParaFinalityProof: paraIncludedHeader %d <= relayMtaOffset %d", uint64(paraIncludedHeader.Number), remoteMtaOffet)
	}

	// create stateproof for para chain get included
	paraIncludedStateProof, err := r.newStateProof(*praIncludeBlockHash)
	if err != nil {
		return nil, err
	}

	bus := make([][]byte, 0)
	sps := make([][]byte, 0)

	if r.mtaHeight < uint64(paraIncludedHeader.Number) {
		// get the latest block contains justification
		gj, hds, err := r.c.GetJustificationsAndUnknownHeaders(substrate.NewBlockNumber(r.mtaHeight))
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
			var votes []byte = nil
			if i == len(blockHeaders)-1 {
				votes, err = r.newVotes(gj, substrate.SetId(remoteSetId))
				if err != nil {
					return nil, err
				}
			}

			bu, err = r.newBlockUpdate(blockHeader, votes)
			if err != nil {
				return nil, err
			}

			r.syncBlocks(uint64(blockHeader.Number-1), blockHeader.ParentHash)
			r.mtaHeight = uint64(blockHeader.Number - 1)
			bus = append(bus, bu)
		}

		r.syncBlocks(uint64(gj.Commit.TargetNumber), gj.Commit.TargetHash)
		r.mtaHeight = uint64(gj.Commit.TargetNumber) // Prevent next paraProof add relayblockUpdates

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

				sps = append(sps, newAuthoritiesStateProof)
			}
		}
	}

	encodedHeader, err := substrate.NewEncodedSubstrateHeader(*paraIncludedHeader)
	if err != nil {
		return nil, err
	}

	var bp []byte

	bp, err = r.newBlockProof(int64(paraIncludedHeader.Number), encodedHeader)
	if err != nil {
		return nil, err
	}
	sps = append(sps, paraIncludedStateProof)

	msg := &ParachainFinalityProof{
		RelayBlockUpdates: bus,
		RelayBlockProof:   bp,
		RelayStateProofs:  sps,
	}

	rmb, err := codec.RLP.MarshalToBytes(msg)
	if err != nil {
		return nil, err
	}

	return rmb, err
}
