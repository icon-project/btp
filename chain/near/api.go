package near

import (
	"encoding/base64"
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/near/types"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

type api struct {
	*jsonrpc.Client
	logger log.Logger
}

type RequestParams struct {
	BlockID int64 `json:"block_id"`
}

func (api *api) getNonce(accountId string, publicKey string) (int64, error) {
	var response types.NonceResponse
	params := struct {
		AccountId    string `json:"account_id"`
		PublicKey    string `json:"public_key"`
		Finality     string `json:"finality"`
		Request_type string `json:"request_type"`
	}{
		AccountId:    accountId,
		PublicKey:    publicKey,
		Finality:     "final",
		Request_type: "view_access_key",
	}

	if _, err := api.Do("query", params, &response); err != nil {
		return -1, err
	}

	if response.Error != "" {

		return -1, fmt.Errorf(response.Error)
	}

	return response.Nonce, nil
}

/*
 Accepts Base 64 Encoded Serialized Data of the Transaction
*/
//Sends a transaction and waits until transaction is fully complete. (Has a 10 second timeout)
func (api *api) broadcastTransaction(base64EncodedData string) (string, error) {
	var transactionStatus TransactionResult

	if _, err := api.Do("broadcast_tx_commit", []interface{}{base64EncodedData}, &transactionStatus); err != nil {
		return "", err
	}
	// Return Transaction ID
	transactionId := transactionStatus.Transaction.Txid

	return transactionId, nil
}

//Sends a transaction and immediately returns transaction hash.
func (api *api) broadcastTransactionAsync(base64EncodedData string) (string, error) {
	var transactionStatus string

	if _, err := api.Do("broadcast_tx_async", []interface{}{base64EncodedData}, &transactionStatus); err != nil {
		return "", err
	}

	// Return Transaction ID
	return transactionStatus, nil
}

func (api *api) getBlockByHash(blockHash string) (types.Block, error) {
	var block types.Block

	param := struct {
		BlockId string `json:"block_id"`
	}{
		BlockId: blockHash,
	}

	if _, err := api.Do("block", &param, &block); err != nil {
		return block, err
	}

	return block, nil
}

func (api *api) getBlockByHeight(height int64) (types.Block, error) {
	var block types.Block

	param := struct {
		BlockId int64 `json:"block_id"`
	}{
		BlockId: height,
	}

	if _, err := api.Do("block", param, &block); err != nil {
		return block, err
	}

	return block, nil
}

func (api *api) getLatestBlockHeight() (int64, error) {
	var chainStatus types.ChainStatus

	if _, err := api.Do("status", []interface{}{}, &chainStatus); err != nil {
		return 0, err
	}

	return chainStatus.SyncInfo.LatestBlockHeight, nil
}

func (api *api) getLatestBlockHash() (string, error) {
	var chainStatus types.ChainStatus

	if _, err := api.Do("status", []interface{}{}, &chainStatus); err != nil {
		return "", err
	}

	return chainStatus.SyncInfo.LatestBlockHash, nil
}

func (api *api) getTransactionResult(transactionId string, senderId string) (types.TransactionResult, error) {
	var transactionResult types.TransactionResult

	params := []string{transactionId, senderId}

	if _, err := api.Do("tx", &params, &transactionResult); err != nil {
		return transactionResult, err
	}

	return transactionResult, nil
}

func (api *api) getContractStateChange(height int64, accountId string, keyPrefix string) (types.ContractStateChange, error) {
	var contractStateChange types.ContractStateChange

	param := struct {
		ChangeType string   `json:"changes_type"`
		AccountIds []string `json:"account_ids"`
		KeyPrefix  string   `json:"key_prefix_base64"`
		BlockId    int64    `json:"block_id"`
	}{
		ChangeType: "data_changes",
		AccountIds: []string{accountId},
		KeyPrefix:  keyPrefix,
		BlockId:    height,
	}

	if _, err := api.Do("EXPERIMENTAL_changes", &param, &contractStateChange); err != nil {
		return contractStateChange, err
	}

	return contractStateChange, nil
}

func (api *api) getNextBlockProducers(blockHash *types.CryptoHash) (types.NextBlockProducers, error) {
	var nextBlockProducers types.NextBlockProducers

	if _, err := api.Do("next_light_client_block", []string{blockHash.Base58Encode()}, &nextBlockProducers); err != nil {
		return nil, err
	}

	return nextBlockProducers, nil
}

func (api *api) getBmcLinkStatus(accountId string, link *chain.BtpAddress) (types.BmcStatus, error) {
	var response CallFunctionResult
	var bmcStatus types.BmcStatus

	methodParam, err := json.Marshal(struct {
		Link string `json:"link"`
	}{
		Link: link.String(),
	})

	if err != nil {
		return bmcStatus, err
	}

	param := &types.CallFunction{
		RequestType:  "call_function",
		Finality:     "final",
		AccountId:    types.AccountId(accountId),
		MethodName:   "get_status",
		ArgumentsB64: base64.URLEncoding.EncodeToString(methodParam),
	}

	if _, err := api.Do("query", &param, &response); err != nil {
		return bmcStatus, err
	}
	err = json.Unmarshal(response.Result, &bmcStatus)
	if err != nil {
		return bmcStatus, err
	}

	return bmcStatus, nil
}

func (api *api) getBmvStatus(accountId string) (types.BmvStatus, error) {
	var response CallFunctionResult
	var bmvStatus types.BmvStatus

	param := &types.CallFunction{
		RequestType:  "call_function",
		Finality:     "final",
		AccountId:    types.AccountId(accountId),
		MethodName:   "get_status",
		ArgumentsB64: base64.StdEncoding.EncodeToString(nil),
	}

	if _, err := api.Do("query", &param, &response); err != nil {
		return bmvStatus, err
	}

	err := json.Unmarshal(response.Result, &bmvStatus)
	if err != nil {
		return bmvStatus, err
	}

	return bmvStatus, nil
}

func (api *api) getReceiptProof(blockHash *types.CryptoHash, receiptId *types.CryptoHash, receiverId string) (types.ReceiptProof, error) {
	var receiptProof types.ReceiptProof

	param := struct {
		Type       string `json:"type"`
		ReceiptId  string `json:"receipt_id"`
		ReceiverId string `json:"receiver_id"`
		BlockHash  string `json:"light_client_head"`
	}{
		Type:       "receipt",
		ReceiptId:  receiptId.Base58Encode(),
		ReceiverId: receiverId,
		BlockHash:  blockHash.Base58Encode(),
	}

	if _, err := api.Do("EXPERIMENTAL_light_client_proof", &param, &receiptProof); err != nil {
		return receiptProof, err
	}

	return receiptProof, nil
}
