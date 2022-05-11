package types

type ContractStateChange struct {
	BlockHash CryptoHash `json:"block_hash"`
	Changes   []Change   `json:"changes"`
}

type Change struct {
	Cause Cause      `json:"cause"`
	Type  string     `json:"type"`
	Data  ChangeData `json:"change"`
}

type Cause struct {
	Type        string     `json:"type"`
	ReceiptHash CryptoHash `json:"receipt_hash"`
}

type ChangeData struct {
	AccountId   AccountId `json:"account_id"`
	KeyBase64   string    `json:"key_base64"`
	ValueBase64 string    `json:"value_base64"`
}
