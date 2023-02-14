/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package client

import (
	"encoding/hex"
	"fmt"
	"math/big"
	"strconv"
	"strings"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/ethereum/go-ethereum/core/types"

	"github.com/icon-project/btp/common/jsonrpc"
	btpTypes "github.com/icon-project/btp/common/types"
)

const (
	JsonrpcApiVersion                                = 3
	JsonrpcErrorCodeSystem         jsonrpc.ErrorCode = -31000
	JsonrpcErrorCodeTxPoolOverflow jsonrpc.ErrorCode = -31001
	JsonrpcErrorCodePending        jsonrpc.ErrorCode = -31002
	JsonrpcErrorCodeExecuting      jsonrpc.ErrorCode = -31003
	JsonrpcErrorCodeNotFound       jsonrpc.ErrorCode = -31004
	JsonrpcErrorLackOfResource     jsonrpc.ErrorCode = -31005
	JsonrpcErrorCodeTimeout        jsonrpc.ErrorCode = -31006
	JsonrpcErrorCodeSystemTimeout  jsonrpc.ErrorCode = -31007
	JsonrpcErrorCodeScore          jsonrpc.ErrorCode = -30000
)

const (
	DuplicateTransactionError = iota + 2000
	TransactionPoolOverflowError
	ExpiredTransactionError
	FutureTransactionError
	TransitionInterruptedError
	InvalidTransactionError
	InvalidQueryError
	InvalidResultError
	NoActiveContractError
	NotContractAddressError
	InvalidPatchDataError
	CommittedTransactionError
)

const (
	ResultStatusSuccess           = "0x1"
	ResultStatusFailureCodeRevert = 32
	ResultStatusFailureCodeEnd    = 99
)

type VerifierStatus_v2 struct {
	SequenceOffset int64
	FirstMessageSn int64
	MessageCount   int64
}

type VerifierStatus_v1 struct {
	Offset     int64
	LastHeight int64
}

type EventLog struct {
	Addr    []byte
	Indexed [][]byte
	Data    [][]byte
}

type TransactionResult struct {
	To                 Address `json:"to"`
	CumulativeStepUsed HexInt  `json:"cumulativeStepUsed"`
	StepUsed           HexInt  `json:"stepUsed"`
	StepPrice          HexInt  `json:"stepPrice"`
	EventLogs          []struct {
		Addr    Address  `json:"scoreAddress"`
		Indexed []string `json:"indexed"`
		Data    []string `json:"data"`
	} `json:"eventLogs"`
	LogsBloom HexBytes `json:"logsBloom"`
	Status    HexInt   `json:"status"`
	Failure   *struct {
		CodeValue    HexInt `json:"code"`
		MessageValue string `json:"message"`
	} `json:"failure,omitempty"`
	SCOREAddress Address  `json:"scoreAddress,omitempty"`
	BlockHash    HexBytes `json:"blockHash" validate:"required,t_hash"`
	BlockHeight  HexInt   `json:"blockHeight" validate:"required,t_int"`
	TxIndex      HexInt   `json:"txIndex" validate:"required,t_int"`
	TxHash       HexBytes `json:"txHash" validate:"required,t_int"`
}

type TransactionParam struct {
	FromAddress string      `json:"from" validate:"required,t_addr_eoa"`
	ToAddress   string      `json:"to" validate:"required,t_addr"`
	NetworkID   HexInt      `json:"nid" validate:"required,t_int"`
	Params      interface{} `json:"params,omitempty"`
	TransactOpt *bind.TransactOpts
}

type CallData struct {
	Method string      `json:"method"`
	Params interface{} `json:"params,omitempty"`
}

type BMCRelayMethodParams struct {
	Prev     string `json:"_prev"`
	Messages string `json:"_msg"`
}

type TransactionHashParam struct {
	Hash common.Hash
}

type BlockRequest struct {
	Height      *big.Int
	FilterQuery *ethereum.FilterQuery
}

//type EventFilter struct {
//	Addr      Address   `json:"addr,omitempty"`
//	Signature string    `json:"event"`
//	Indexed   []*string `json:"indexed,omitempty"`
//	Data      []*string `json:"data,omitempty"`
//}

type BlockNotification struct {
	Hash   common.Hash
	Height *big.Int
	Header *types.Header
	Logs   []types.Log
}

type Event struct {
	Next     btpTypes.BtpAddress
	Sequence *big.Int
	Message  []byte
}

type BlockUpdate struct {
	Height    int64
	BlockHash []byte
	Header    []byte
	Proof     []byte
}

type EvmBlockUpdate struct {
	BlockHeader []byte
	Validators  []byte
	EvmHeader   []byte
}

type ExtraField struct {
	ExtraVanity []byte
}

type RelayMessage struct {
	Receipts [][]byte
}

type Receipt struct {
	Index  int64
	Events []byte
	Height int64
}

type ReceiptProof struct {
	Index       int64
	Proof       []byte
	Height      int64 //Bridge only
	EventProofs []*EventProof
	Events      []*Event
}

type EventProof struct {
	Index int
	Proof []byte
}

type ConsensusStates struct {
	PreValidatorSetChangeHeight uint64
	AppHash                     [32]byte
	CurValidatorSetHash         [32]byte
	NextValidatorSet            []byte
}

type StorageProof struct {
	StateRoot    common.Hash     `json:"stateRoot"`
	Height       uint64          `json:"height"`
	Address      common.Address  `json:"address"`
	AccountProof []string        `json:"accountProof"`
	Balance      uint64          `json:"balance"`
	CodeHash     common.Hash     `json:"codeHash"`
	Nonce        hexutil.Uint64  `json:"nonce"`
	StorageHash  common.Hash     `json:"storageHash"`
	StorageProof []StorageResult `json:"storageProof"`
}

type StorageResult struct {
	Key   string       `json:"key"`
	Value *hexutil.Big `json:"value"`
	Proof []string     `json:"proof"`
}

// Header represents a block header in the Ethereum blockchain.
type Header struct {
	ParentHash  common.Hash
	UncleHash   common.Hash
	Coinbase    common.Address
	Root        common.Hash
	TxHash      common.Hash
	ReceiptHash common.Hash
	Bloom       []byte
	Difficulty  uint64
	Number      uint64
	GasLimit    uint64
	GasUsed     uint64
	Time        uint64
	Extra       []byte
	MixDigest   common.Hash
	Nonce       types.BlockNonce
}

func MakeHeader(header *types.Header) *Header {
	// Convert Geth types to Goloop RLP friendly type
	return &Header{
		ParentHash:  header.ParentHash,
		UncleHash:   header.UncleHash,
		Coinbase:    header.Coinbase,
		Root:        header.Root,
		TxHash:      header.TxHash,
		ReceiptHash: header.ReceiptHash,
		Bloom:       header.Bloom.Bytes(),
		Difficulty:  header.Difficulty.Uint64(),
		Number:      header.Number.Uint64(),
		GasLimit:    header.GasLimit,
		GasUsed:     header.GasUsed,
		Time:        header.Time,
		Extra:       header.Extra,
		MixDigest:   header.MixDigest,
		Nonce:       header.Nonce,
	}
}

type EVMLog struct {
	Address     string
	Topics      [][]byte
	Data        []byte
	BlockNumber uint64
	TxHash      []byte
	TxIndex     uint
	BlockHash   []byte
	Index       uint
	Removed     bool
}

func MakeLog(log *types.Log) *EVMLog {
	topics := make([][]byte, 0)

	for _, topic := range log.Topics {
		topics = append(topics, topic.Bytes())
	}

	return &EVMLog{
		Address:     log.Address.String(),
		Topics:      topics,
		Data:        log.Data,
		BlockNumber: log.BlockNumber,
		TxHash:      log.TxHash.Bytes(),
		TxIndex:     log.TxIndex,
		BlockHash:   log.BlockHash.Bytes(),
		Index:       log.Index,
		Removed:     log.Removed,
	}
}

func HexToAddress(s string) common.Address {
	return common.HexToAddress(s)
}

// HexBytes T_BIN_DATA, T_HASH
type HexBytes string

func (hs HexBytes) Value() ([]byte, error) {
	if hs == "" {
		return nil, nil
	}
	return hex.DecodeString(string(hs[2:]))
}

// HexInt T_INT
type HexInt string

func (i HexInt) Value() (int64, error) {
	s := string(i)
	if strings.HasPrefix(s, "0x") {
		s = s[2:]
	}
	return strconv.ParseInt(s, 16, 64)
}

func (i HexInt) Int() (int, error) {
	s := string(i)
	if strings.HasPrefix(s, "0x") {
		s = s[2:]
	}
	v, err := strconv.ParseInt(s, 16, 32)
	return int(v), err
}

// Address T_ADDR_EOA, T_ADDR_SCORE
type Address string

func (a Address) Value() ([]byte, error) {
	var b [21]byte
	switch a[:2] {
	case "cx":
		b[0] = 1
	case "hx":
	default:
		return nil, fmt.Errorf("invalid prefix %s", a[:2])
	}
	n, err := hex.Decode(b[1:], []byte(a[2:]))
	if err != nil {
		return nil, err
	}
	if n != 20 {
		return nil, fmt.Errorf("invalid length %d", n)
	}
	return b[:], nil
}

// Signature T_SIG
type Signature string
