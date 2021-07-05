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
type EvmHash = common.Hash
type EvmLog = types.Log

func EvmHexToHash(s string) common.Hash {
	return common.HexToHash(s)
}

func EvmHexToAddress(s string) common.Address {
	return common.HexToAddress(s)
}
