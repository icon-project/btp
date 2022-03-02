package types

import (
	"bytes"
	"fmt"
	"github.com/near/borsh-go"
)

type Header struct {
	Height                int64       `json:"height"`
	PreviousHeight        int64       `json:"prev_height"`
	EpochId               CryptoHash  `json:"epoch_id"`
	NextEpochId           CryptoHash  `json:"next_epoch_id"`
	Hash                  CryptoHash  `json:"hash"`
	PreviousBlockHash     CryptoHash  `json:"prev_hash"`
	PreviousStateRoot     CryptoHash  `json:"prev_state_root"`
	ChunkReceiptsRoot     CryptoHash  `json:"chunk_receipts_root"`
	ChunkHeadersRoot      CryptoHash  `json:"chunk_headers_root"`
	ChunkTransactionRoot  CryptoHash  `json:"chunk_tx_root"`
	OutcomeRoot           CryptoHash  `json:"outcome_root"`
	ChunksIncluded        uint8       `json:"chunks_included"`
	ChallengesRoot        CryptoHash  `json:"challenges_root"`
	Timestamp             Timestamp   `json:"timestamp_nanosec"`
	RandomValue           CryptoHash  `json:"random_value"`
	ValidatorProposals    []string    `json:"validator_proposals"`
	ChunkMask             []bool      `json:"chunk_mask"`
	GasPrice              BigInt      `json:"gas_price"`
	BlockOrdinal          uint64      `json:"block_ordinal"`
	TotalSupply           BigInt      `json:"total_supply"`
	ChallengesResult      []string    `json:"challenges_result"`
	LastFinalBlock        CryptoHash  `json:"last_final_block"`
	LastDSFinalBlock      CryptoHash  `json:"last_ds_final_block"`
	NextBlockProducerHash CryptoHash  `json:"next_bp_hash"`
	BlockMerkleRoot       CryptoHash  `json:"block_merkle_root"`
	EpochSyncDataHash     CryptoHash  `json:"epoch_sync_data_hash"`
	Approvals             []Signature `json:"approvals"`
	Signature             Signature   `json:"signature"`
	LatestProtocolVersion uint32      `json:"latest_protocol_version"`
}

type HeaderInnerLite struct {
	Height                uint64
	EpochId               []byte
	NextEpochId           []byte
	PreviousStateRoot     []byte
	OutcomeRoot           []byte
	Timestamp             uint64
	NextBlockProducerHash []byte
	BlockMerkleRoot       []byte
}

func (h HeaderInnerLite) BorshSerialize() ([]byte, error) {
	serialized := new(bytes.Buffer)

	height, err := borsh.Serialize(h.Height)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerLite: Height")
	}

	timestamp, err := borsh.Serialize(h.Timestamp)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerLite: Timestamp")
	}

	serialized.Write(height)
	serialized.Write(h.EpochId)
	serialized.Write(h.NextEpochId)
	serialized.Write(h.PreviousStateRoot)
	serialized.Write(h.OutcomeRoot)
	serialized.Write(timestamp)
	serialized.Write(h.NextBlockProducerHash)
	serialized.Write(h.BlockMerkleRoot)

	return serialized.Bytes(), nil
}

type HeaderInnerRest struct {
	ChunkReceiptsRoot     []byte
	ChunkHeadersRoot      []byte
	ChunkTransactionRoot  []byte
	ChallengesRoot        []byte
	RandomValue           []byte
	ValidatorProposals    []string
	ChunkMask             []bool
	GasPrice              BigInt
	TotalSupply           BigInt
	ChallengesResult      []string
	LastFinalBlock        []byte
	LastDSFinalBlock      []byte
	BlockOrdinal          uint64
	PreviousHeight        uint64
	EpochSyncDataHash     []byte
	Approvals             []Signature
	LatestProtocolVersion uint32
}

func (h HeaderInnerRest) BorshSerialize() ([]byte, error) {
	serialized := new(bytes.Buffer)

	validatorProposals, err := borsh.Serialize(h.ValidatorProposals)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: ValidatorProposals")
	}

	chunkMask, err := borsh.Serialize(h.ChunkMask)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: ChunkMask")
	}

	gasPriceBigInt, err := h.GasPrice.Int()
	if err != nil {
		return nil, fmt.Errorf("failed to convert GasPrice to BigInt")
	}

	gasPrice, err := borsh.Serialize(gasPriceBigInt)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: GasPrice")
	}

	totalSupplyBigInt, err := h.TotalSupply.Int()
	if err != nil {
		return nil, fmt.Errorf("failed to convert TotalSupply to BigInt")
	}

	totalSupply, err := borsh.Serialize(totalSupplyBigInt)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: TotalSupply")
	}

	challengesResult, err := borsh.Serialize(h.ChallengesResult)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: ChallengesResult")
	}

	blockOrdinal, err := borsh.Serialize(h.BlockOrdinal)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: BlockOrdinal")
	}

	previousHeight, err := borsh.Serialize(h.PreviousHeight)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: PreviousHeight")
	}

	var epochSyncDataHash []byte
	if h.EpochSyncDataHash != nil {
		epochSyncDataHash = append([]byte{1}, h.EpochSyncDataHash...)
	} else {
		epochSyncDataHash = []byte{0}
	}

	var approvals []byte
	for i := 0; i < len(h.Approvals); i++ {
		if h.Approvals[i] != nil {
			approvals = append(approvals, append([]byte{1}, h.Approvals[i]...)...)
		} else {
			approvals = append(approvals, []byte{0}...)
		}
	}

	if len(h.Approvals) > 0 {
		len, _ := borsh.Serialize(uint32(len(h.Approvals)))
		approvals = append(len, approvals...)
	}

	latestProtocolVersion, err := borsh.Serialize(h.LatestProtocolVersion)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize HeaderInnerRest: LatestProtocolVersion")
	}

	serialized.Write(h.ChunkReceiptsRoot)
	serialized.Write(h.ChunkHeadersRoot)
	serialized.Write(h.ChunkTransactionRoot)
	serialized.Write(h.ChallengesRoot)
	serialized.Write(h.RandomValue)
	serialized.Write(validatorProposals)
	serialized.Write(chunkMask)
	serialized.Write(gasPrice)
	serialized.Write(totalSupply)
	serialized.Write(challengesResult)
	serialized.Write(h.LastFinalBlock)
	serialized.Write(h.LastDSFinalBlock)
	serialized.Write(blockOrdinal)
	serialized.Write(previousHeight)
	serialized.Write(epochSyncDataHash)
	serialized.Write(approvals)
	serialized.Write(latestProtocolVersion)

	return serialized.Bytes(), nil
}
