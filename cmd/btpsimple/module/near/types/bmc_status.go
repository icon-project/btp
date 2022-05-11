package types

type BmcStatus struct {
	TxSeq            int64         `json:"tx_seq"`
	RxSeq            int64         `json:"rx_seq"`
	Verifier         AccountId     `json:"verifier"`
	BMRs             []RelayStatus `json:"relays"`
	BMRIndex         int           `json:"relay_index"`
	RotateHeight     int64         `json:"rotate_height"`
	RotateTerm       int           `json:"rotate_term"`
	DelayLimit       int           `json:"delay_limit"`
	MaxAggregation   int           `json:"max_aggregation"`
	CurrentHeight    int64         `json:"current_height"`
	RxHeight         int64         `json:"rx_height"`
	RxHeightSrc      int64         `json:"rx_height_src"`
	BlockIntervalSrc int           `json:"block_interval_src"`
	BlockIntervalDst int           `json:"block_interval_dst"`
}
