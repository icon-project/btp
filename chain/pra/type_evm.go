package pra

import (
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
)

type EvmReceipt *types.Receipt
type EvmTransaction *types.Transaction

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
