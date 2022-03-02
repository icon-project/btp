package types

type ChainStatus struct {
	ChainId  string        `json:"chain_id"`
	SyncInfo ChainSyncInfo `json:"sync_info"`
}

type ChainSyncInfo struct {
	LatestBlockHash   string `json:"latest_block_hash"`
	LatestBlockHeight int64  `json:"latest_block_height"`
	LatestBlockTime   string `json:"latest_block_time"`
	Syncing           bool   `json:"syncing"`
}