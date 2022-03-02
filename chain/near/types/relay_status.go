package types

type RelayStatus struct {
	Address      AccountId `json:"account_id"`
	BlockCount   int64     `json:"block_count"`
	MessageCount int64     `json:"message_count"`
}
