package types

import (
	"crypto/sha256"
	"github.com/icon-project/btp/common/codec"
)

type SerializableHeader struct {
	PreviousBlockHash []byte
	InnerLite         HeaderInnerLite
	InnerRest         HeaderInnerRest
	Signature         Signature
}

type Block struct {
	Author string  `json:"author"`
	Header Header  `json:"header"`
	Chunks []Chunk `json:"chunks"`
}

func (b *Block) InnerLite() HeaderInnerLite {
	return HeaderInnerLite{
		Height:                uint64(b.Header.Height),
		EpochId:               b.Header.EpochId,
		NextEpochId:           b.Header.NextEpochId,
		PreviousStateRoot:     b.Header.PreviousStateRoot,
		OutcomeRoot:           b.Header.OutcomeRoot,
		Timestamp:             uint64(b.Header.Timestamp),
		NextBlockProducerHash: b.Header.NextBlockProducerHash,
		BlockMerkleRoot:       b.Header.BlockMerkleRoot,
	}
}

func (b *Block) InnerRest() HeaderInnerRest {
	return HeaderInnerRest{
		ChunkReceiptsRoot:     b.Header.ChunkReceiptsRoot,
		ChunkHeadersRoot:      b.Header.ChunkHeadersRoot,
		ChunkTransactionRoot:  b.Header.ChunkTransactionRoot,
		ChallengesRoot:        b.Header.ChallengesRoot,
		RandomValue:           b.Header.RandomValue,
		ValidatorProposals:    b.Header.ValidatorProposals,
		ChunkMask:             b.Header.ChunkMask,
		GasPrice:              b.Header.GasPrice,
		TotalSupply:           b.Header.TotalSupply,
		ChallengesResult:      b.Header.ChallengesResult,
		LastFinalBlock:        b.Header.LastFinalBlock,
		LastDSFinalBlock:      b.Header.LastDSFinalBlock,
		BlockOrdinal:          b.Header.BlockOrdinal,
		PreviousHeight:        uint64(b.Header.PreviousHeight),
		EpochSyncDataHash:     b.Header.EpochSyncDataHash,
		Approvals:             b.Header.Approvals,
		LatestProtocolVersion: b.Header.LatestProtocolVersion,
	}
}

func (b *Block) Height() int64 {
	return b.Header.Height
}

func (b *Block) Hash() []byte {
	return b.Header.Hash
}

func (b *Block) ComputeInnerHash(innerLite HeaderInnerLite, innerRest HeaderInnerRest) ([]byte, error) {
	innerLiteSerialized, err := innerLite.BorshSerialize()
	if err != nil {
		return nil, err
	}

	innerRestSerialized, err := innerRest.BorshSerialize()
	if err != nil {
		return nil, err
	}

	innerliteHash := sha256.Sum256(innerLiteSerialized)
	innerRestHash := sha256.Sum256(innerRestSerialized)
	innerHash := CombineHash(innerliteHash[:], innerRestHash[:])

	return innerHash, nil

}

func (b *Block) ComputeHash(PreviousBlockHash []byte, innerLite HeaderInnerLite, innerRest HeaderInnerRest) ([]byte, error) {
	innerHash, err := b.ComputeInnerHash(innerLite, innerRest)
	if err != nil {
		return nil, err
	}

	hash := CombineHash(innerHash, PreviousBlockHash)

	return hash, nil
}

func (b *Block) SerializableHeader() SerializableHeader {
	return SerializableHeader{
		PreviousBlockHash: b.Header.PreviousBlockHash,
		InnerLite:         b.InnerLite(),
		InnerRest:         b.InnerRest(),
		Signature:         b.Header.Signature,
	}
}

func (b *Block) RlpSerialize() ([]byte, error) {
	return codec.RLP.MarshalToBytes(b.SerializableHeader())
}
