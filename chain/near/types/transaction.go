package types

type TransactionResult struct {
	Status             ExecutionStatus            `json:"status"`
	Transaction        TransactionView            `json:"transaction"`
	TransactionOutcome ExecutionOutcomeWithIdView `json:"transaction_outcome"`
}

type TransactionView struct {
	SignerId   string        `json:"signer_id"`
	PublicKey  string        `json:"public_key"`
	Nonce      int           `json:"nonce"`
	ReceiverId string        `json:"receiver_id"`
	Actions    []interface{} `json:"actions"` // TODO: ActionView
	Signature  string        `json:"signature"`
	Txid       string        `json:"hash"`
}

type RelayMessageParam struct {
	Previous string `json:"_prev"`
	Message  string `json:"_msg"`
}

type TransactionParam struct {
	From              string
	To                string
	RelayMessage      RelayMessageParam
	Base64encodedData string
}
