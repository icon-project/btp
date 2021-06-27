package pra

import (
	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/rpc"
)

type EvmReceipt = types.Receipt
type EvmTransaction = types.Transaction
type EvmCallMsg = ethereum.CallMsg
type EvmAddress = common.Address
type EvmDataError = rpc.DataError

func (e *EventEVMLog) EvmLog() types.Log {
	topics := []common.Hash{}

	for _, t := range e.Topics {
		topics = append(topics, common.HexToHash(t.Hex()))
	}

	return types.Log{
		Address: common.Address(e.Log.Address),
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
