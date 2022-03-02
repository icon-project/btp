package types

type Chunk struct {
	ChunkHash            CryptoHash `json:"chunk_hash"`
	PreviousBlockHash    CryptoHash `json:"prev_block_hash"`
	OutcomeRoot          CryptoHash `json:"outcome_root"`
	PrevStateRoot        CryptoHash `json:"prev_state_root"`
	EncodedMerkleRoot    CryptoHash `json:"encoded_merkle_root"`
	EncodedLength        uint       `json:"encoded_length"`
	HeightCreated        uint       `json:"height_created"`
	HeightIncluded       uint       `json:"height_included"`
	ShardId              uint       `json:"shard_id"`
	OutgoingReceiptsRoot CryptoHash `json:"outgoing_receipts_root"`
	TxRoot               CryptoHash `json:"tx_root"`
	Signature            CryptoHash `json:"signature"`
}
