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

package bsc

import (
	"encoding/hex"
	"fmt"
	"math/big"
	"strconv"
	"strings"

	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/icon-project/btp/cmd/btp2/module"

	"github.com/icon-project/btp/common/jsonrpc"
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

type VerifierStatus struct {
	Height           int64
	Sequence_offset  int64
	First_message_sn int64
	Message_count    int64
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

type BMCStatus struct {
	TxSeq    HexInt `json:"tx_seq"`
	RxSeq    HexInt `json:"rx_seq"`
	Verifier struct {
		Height     HexInt `json:"height"`
		Offset     HexInt `json:"offset"`
		LastHeight HexInt `json:"last_height"`
	} `json:"verifier"`
	BMRs []struct {
		Address      Address `json:"address"`
		BlockCount   HexInt  `json:"block_count"`
		MessageCount HexInt  `json:"msg_count"`
	} `json:"relays"`
	BMRIndex         HexInt `json:"relay_idx"`
	RotateHeight     HexInt `json:"rotate_height"`
	RotateTerm       HexInt `json:"rotate_term"`
	DelayLimit       HexInt `json:"delay_limit"`
	MaxAggregation   HexInt `json:"max_agg"`
	CurrentHeight    HexInt `json:"cur_height"`
	RxHeight         HexInt `json:"rx_height"`
	RxHeightSrc      HexInt `json:"rx_height_src"`
	BlockIntervalSrc HexInt `json:"block_interval_src"`
	BlockIntervalDst HexInt `json:"block_interval_dst"`
}

type TransactionHashParam struct {
	Hash common.Hash
}

type BlockRequest struct {
	Height       *big.Int       `json:"height"`
	EventFilters []*EventFilter `json:"eventFilters,omitempty"`
}

type EventFilter struct {
	Addr      Address   `json:"addr,omitempty"`
	Signature string    `json:"event"`
	Indexed   []*string `json:"indexed,omitempty"`
	Data      []*string `json:"data,omitempty"`
}

type BlockNotification struct {
	Hash   common.Hash
	Height *big.Int
	Header *types.Header
}

type BlockUpdate struct {
	BlockHeader []byte
	Validators  []byte
	EvmHeader   []byte
}

type RelayMessage struct {
	BlockUpdates  [][]byte
	BlockProof    []byte
	ReceiptProofs [][]byte
	//
	height              int64
	numberOfBlockUpdate int
	eventSequence       int64
	numberOfEvent       int
}

func (r *RelayMessage) GetHeight() int64 {
	return r.height
}

func (r *RelayMessage) GetNumberOfBlockUpdate() int {
	return r.numberOfBlockUpdate
}

func (r *RelayMessage) GetEventSequence() int64 {
	return r.eventSequence
}

func (r *RelayMessage) GetNumberOfEvent() int {
	return r.numberOfEvent
}

func (r *RelayMessage) SetHeight(height int64) {
	r.height = height
}

func (r *RelayMessage) SetNumberOfBlockUpdate(numberOfBlockUpdate int) {
	r.numberOfBlockUpdate = numberOfBlockUpdate
}

func (r *RelayMessage) SetEventSequence(eventSequence int64) {
	r.eventSequence = eventSequence
}

func (r *RelayMessage) SetNumberOfEvent(numberOfEvent int) {
	r.numberOfEvent = numberOfEvent
}

type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*module.EventProof
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

type Receipt struct {
	// Consensus fields: These fields are defined by the Yellow Paper
	Type              uint8
	PostState         []byte
	Status            uint64
	CumulativeGasUsed uint64
	Bloom             []byte
	Logs              []*EVMLog

	TxHash          common.Hash
	ContractAddress common.Address
	GasUsed         uint64

	BlockHash        common.Hash
	BlockNumber      uint64
	TransactionIndex uint
}

func MakeReceipt(receipt *types.Receipt) *Receipt {
	logs := make([]*EVMLog, len(receipt.Logs))

	for _, log := range receipt.Logs {
		logs = append(logs, MakeLog(log))
	}

	return &Receipt{
		Type:              receipt.Type,
		PostState:         receipt.PostState,
		Status:            receipt.Status,
		CumulativeGasUsed: receipt.CumulativeGasUsed,
		Bloom:             receipt.Bloom.Bytes(),
		Logs:              logs,
		TxHash:            receipt.TxHash,
		ContractAddress:   receipt.ContractAddress,
		GasUsed:           receipt.GasUsed,
		BlockHash:         receipt.BlockHash,
		BlockNumber:       receipt.BlockNumber.Uint64(),
		TransactionIndex:  receipt.TransactionIndex,
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
