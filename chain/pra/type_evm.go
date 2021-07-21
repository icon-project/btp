package pra

import (
	"math/big"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/rpc"
	"github.com/icon-project/btp/chain/pra/frontier"
)

type EvmReceipt = types.Receipt
type EvmTransaction = types.Transaction
type EvmCallMsg = ethereum.CallMsg
type EvmAddress = common.Address
type EvmDataError = rpc.DataError
type EvmHash = common.Hash
type EvmLog = types.Log

func NewEvmLog(e frontier.EventEVMLog) EvmLog {
	topics := []EvmHash{}

	for _, t := range e.Log.Topics {
		topics = append(topics, EvmHexToHash(t.Hex()))
	}

	return EvmLog{
		Address: EvmAddress(e.Log.Address),
		Topics:  topics,
		Data:    []byte(e.Log.Data),
	}
}

func EvmHexToHash(s string) common.Hash {
	return common.HexToHash(s)
}

func EvmHexToAddress(s string) common.Address {
	return common.HexToAddress(s)
}

func NewEvmNewTransaction(nonce uint64, to common.Address, amount *big.Int, gasLimit uint64, gasPrice *big.Int, data []byte) *EvmTransaction {
	return types.NewTransaction(nonce, to, amount, gasLimit, gasPrice, data)
}
