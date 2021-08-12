package pra

import (
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/mta"
)

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

// newBlockProof creates a new BlockProof
func (r *relayReceiver) newBlockProof(height int64, expectMtaHeight int64) ([]byte, error) {
	hash, err := r.c.GetBlockHash(uint64(height))
	if err != nil {
		return nil, err
	}

	header, err := r.c.GetHeader(hash)
	if err != nil {
		return nil, err
	}

	encodedHeader, err := substrate.NewEncodedSubstrateHeader(*header)
	if err != nil {
		return nil, err
	}

	at, w, err := r.store.WitnessForAt(height, expectMtaHeight, int64(r.bmvStatus.RelayMtaOffset))
	if err != nil {
		r.log.Errorf("newBlockProof: height %d mtaHeight %d %v", height, r.bmvStatus.RelayMtaHeight, err)
		return nil, err
	}

	bp := &chain.BlockProof{
		Header: encodedHeader,
		BlockWitness: &chain.BlockWitness{
			Height:  at,
			Witness: mta.WitnessesToHashes(w),
		},
	}

	r.log.Debugf("newBlockProof height:%d, at:%d", height, at)
	r.log.Tracef("newBlockProof %x", bp.BlockWitness.Witness)

	b, err := codec.RLP.MarshalToBytes(bp)
	if err != nil {
		return nil, err
	}

	return b, nil
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

func (r *relayReceiver) newFinalityProof(bus, sps [][]byte, bp []byte) ([]byte, error) {
	msg := &ParachainFinalityProof{
		RelayBlockUpdates: make([][]byte, 0),
		RelayBlockProof:   bp,
		RelayStateProofs:  sps,
	}

	rmb, err := codec.RLP.MarshalToBytes(msg)
	if err != nil {
		return nil, err
	}

	return rmb, err
}
