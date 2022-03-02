package near

import (
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/near/testdata/mock"
	"github.com/icon-project/btp/chain/near/types"
)

type Mockapi struct {
	*mock.Storage
}

var LatestBlockHeight int64

type Ress struct {
	Result  []*chain.Event
	Receipt []string
}

func NewMockApi(storage mock.Storage) Mockapi {
	var emptyResponse mock.Response
	var defaults = mock.Default()

	if storage.LatestBlockHeight == emptyResponse {
		storage.LatestBlockHeight = defaults.LatestBlockHeight
	}

	if storage.LatestBlockHash == emptyResponse {
		storage.LatestBlockHash = defaults.LatestBlockHash
	}

	if storage.TransactionHash == emptyResponse {
		storage.TransactionHash = defaults.TransactionHash
	}

	for key, value := range storage.BlockByHeightMap {
		defaults.BlockByHeightMap[key] = value
	}

	storage.BlockByHeightMap = defaults.BlockByHeightMap

	for key, value := range storage.BlockByHashMap {
		defaults.BlockByHashMap[key] = value
	}

	storage.BlockByHashMap = defaults.BlockByHashMap
	for key, value := range storage.NonceMap {
		defaults.NonceMap[key] = value
	}

	storage.NonceMap = defaults.NonceMap

	for key, value := range storage.BmcLinkStatusMap {
		defaults.BmcLinkStatusMap[key] = value
	}
	storage.BmcLinkStatusMap = defaults.BmcLinkStatusMap

	for key, value := range storage.BmvStatusMap {
		defaults.BmvStatusMap[key] = value
	}
	storage.BmvStatusMap = defaults.BmvStatusMap

	for key, value := range storage.ContractStateChangeMap {
		defaults.ContractStateChangeMap[key] = value
	}
	storage.ContractStateChangeMap = defaults.ContractStateChangeMap

	return Mockapi{
		&storage,
	}
}

func (api *Mockapi) getLatestBlockHash() (string, error) {
	if api.LatestBlockHash.Error != nil {
		return "", api.LatestBlockHash.Error
	}
	if latestBlockHash, Ok := (api.LatestBlockHash.Reponse).(string); Ok {
		return latestBlockHash, nil
	}

	return "", fmt.Errorf("failed to cast LatestBlockHash to string")
}

func (api *Mockapi) getLatestBlockHeight() (int64, error) {
	if api.LatestBlockHeight.Error != nil {
		return 0, api.LatestBlockHeight.Error
	}
	if latestBlockHeight, Ok := (api.LatestBlockHeight.Reponse).(int); Ok {
		return int64(latestBlockHeight), nil
	}

	return 0, fmt.Errorf("failed to cast LatestBlockHeight to int64")
}

func (api *Mockapi) getNonce(accountId string, publicKey string) (int64, error) {
	if api.NonceMap[accountId].Error != nil {
		return 0, api.NonceMap[accountId].Error
	}

	if nonce, Ok := (api.NonceMap[accountId].Reponse).(int64); Ok {
		return nonce, nil
	}

	return 0, fmt.Errorf("failed to cast Nonce to int64")
}

func (api *Mockapi) getBlockByHash(blockHash string) (types.Block, error) {
	var block types.Block

	if api.BlockByHashMap[blockHash].Error != nil {
		return block, api.BlockByHashMap[blockHash].Error
	}

	if response, Ok := (api.BlockByHashMap[blockHash].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &block)
		if err != nil {
			return block, err
		}

		return block, nil
	}

	return block, fmt.Errorf("failed to cast Block to []byte")
}

func (api *Mockapi) broadcastTransactionAsync(string) (string, error) {
	if api.TransactionHash.Error != nil {
		return "", api.TransactionHash.Error
	}

	if transactionHash, Ok := (api.TransactionHash.Reponse).(string); Ok {
		return transactionHash, nil
	}

	return "", fmt.Errorf("failed to cast TransactionHash to string")
}

func (api *Mockapi) broadcastTransaction(string) (string, error) {
	if api.TransactionHash.Error != nil {
		return "", api.TransactionHash.Error
	}

	if transactionHash, Ok := (api.TransactionHash.Reponse).(string); Ok {
		return transactionHash, nil
	}

	return "", fmt.Errorf("failed to cast TransactionHash to string")
}

func (api *Mockapi) getReceiptProof(blockHash *types.CryptoHash, receiptId *types.CryptoHash, receiverId string) (types.ReceiptProof, error) {
	var receiptProof types.ReceiptProof

	if api.ReceiptProofMap[receiptId.Base58Encode()].Error != nil {
		return receiptProof, api.ReceiptProofMap[receiptId.Base58Encode()].Error
	}

	if response, Ok := (api.ReceiptProofMap[receiptId.Base58Encode()].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &receiptProof)
		if err != nil {
			return receiptProof, err
		}

		return receiptProof, nil
	}

	return receiptProof, fmt.Errorf("failed to cast ReceiptProof to []byte")
}

func (api *Mockapi) getBmcLinkStatus(accountId string, link *chain.BtpAddress) (types.BmcStatus, error) {
	var bmcStatus types.BmcStatus

	if api.BmcLinkStatusMap[link.Account()].Error != nil {
		return bmcStatus, api.BmcLinkStatusMap[link.Account()].Error
	}

	if response, Ok := (api.BmcLinkStatusMap[link.Account()].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &bmcStatus)
		if err != nil {
			return bmcStatus, err
		}

		return bmcStatus, nil
	}

	return bmcStatus, fmt.Errorf("failed to cast BmcStatus to []byte")
}

func (api *Mockapi) getBmvStatus(accountId string) (types.BmvStatus, error) {
	var bmvStatus types.BmvStatus

	if api.BmvStatusMap[accountId].Error != nil {
		return bmvStatus, api.BmvStatusMap[accountId].Error
	}

	if response, Ok := (api.BmvStatusMap[accountId].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &bmvStatus)
		if err != nil {
			return bmvStatus, err
		}

		return bmvStatus, nil
	}

	return bmvStatus, fmt.Errorf("failed to cast BmvStatus to []byte")
}

func (api *Mockapi) getNextBlockProducers(blockHash *types.CryptoHash) (types.NextBlockProducers, error) {
	var nextBlockProducers types.NextBlockProducers

	if api.LightClientBlockMap[blockHash.Base58Encode()].Error != nil {
		return nextBlockProducers, api.LightClientBlockMap[blockHash.Base58Encode()].Error
	}

	if response, Ok := (api.LightClientBlockMap[blockHash.Base58Encode()].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &nextBlockProducers)
		if err != nil {
			return nextBlockProducers, err
		}

		return nextBlockProducers, nil
	}

	return nextBlockProducers, fmt.Errorf("failed to cast NextBlockProducers to []byte")
}

func (api *Mockapi) getBlockByHeight(height int64) (types.Block, error) {
	var block types.Block

	if api.BlockByHeightMap[height].Error != nil {
		return block, api.BlockByHeightMap[height].Error
	}
	if response, Ok := (api.BlockByHeightMap[height].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &block)
		if err != nil {
			return block, err
		}
		return block, nil
	}
	return block, fmt.Errorf("failed to cast Block to []byte")
}

func (api *Mockapi) getContractStateChange(height int64, accountId string, keyPrefix string) (types.ContractStateChange, error) {
	var changes types.ContractStateChange

	if api.ContractStateChangeMap[height].Error != nil {
		return changes, api.ContractStateChangeMap[height].Error
	}

	if response, Ok := (api.ContractStateChangeMap[height].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &changes)
		if err != nil {
			return changes, err
		}

		return changes, nil
	}

	return changes, fmt.Errorf("failed to cast ContractStateChange to []byte")
}

func (api *Mockapi) getTransactionResult(transactionId string, senderId string) (types.TransactionResult, error) {
	var transactionResult types.TransactionResult

	if api.TransactionResultMap[transactionId].Error != nil {
		return transactionResult, api.TransactionResultMap[transactionId].Error
	}

	if response, Ok := (api.TransactionResultMap[transactionId].Reponse).([]byte); Ok {
		err := json.Unmarshal(response, &transactionResult)
		if err != nil {
			return transactionResult, err
		}

		return transactionResult, nil
	}

	return transactionResult, fmt.Errorf("failed to cast TransactionResult to []byte")
}
