package types

type NonceParam struct {
	AccountId    string `json:"account_id"`
	PublicKey    string `json:"public_key"`
	Finality     string `json:"finality"`
	Request_type string `json:"request_type"`
}
type NonceResponse struct {
	Nonce       int64  `json:"nonce"`
	Permission  string `json:"permission"`
	BlockHeight int64  `json:"block_height"`
	BlockHash   string `json:"block_hash"`
	Error       string `json:"error"`
}
