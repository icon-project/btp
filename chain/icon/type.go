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

package icon

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/icon-project/btp/chain"
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

const (
	BMCRelayMethod     = "handleRelayMessage"
	BMCFragmentMethod  = "handleFragment"
	BMCGetStatusMethod = "getStatus"
)

type BlockHeader struct {
	Version                int
	Height                 int64
	Timestamp              int64
	Proposer               []byte
	PrevID                 []byte
	VotesHash              []byte
	NextValidatorsHash     []byte
	PatchTransactionsHash  []byte
	NormalTransactionsHash []byte
	LogsBloom              []byte
	Result                 []byte
	serialized             []byte
}

type ReceiptData struct {
	Status             int
	To                 []byte
	CumulativeStepUsed []byte
	StepUsed           []byte
	StepPrice          []byte
	LogsBloom          []byte
	EventLogs          []EventLog
	SCOREAddress       []byte
	EventLogsHash      []byte
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
	Version     HexInt      `json:"version" validate:"required,t_int"`
	FromAddress Address     `json:"from" validate:"required,t_addr_eoa"`
	ToAddress   Address     `json:"to" validate:"required,t_addr"`
	Value       HexInt      `json:"value,omitempty" validate:"optional,t_int"`
	StepLimit   HexInt      `json:"stepLimit" validate:"required,t_int"`
	Timestamp   HexInt      `json:"timestamp" validate:"required,t_int"`
	NetworkID   HexInt      `json:"nid" validate:"required,t_int"`
	Nonce       HexInt      `json:"nonce,omitempty" validate:"optional,t_int"`
	Signature   string      `json:"signature" validate:"required,t_sig"`
	DataType    string      `json:"dataType,omitempty" validate:"optional,call|deploy|message"`
	Data        interface{} `json:"data,omitempty"`
	TxHash      HexBytes    `json:"-"`
}
type CallData struct {
	Method string      `json:"method"`
	Params interface{} `json:"params,omitempty"`
}

type DeployData struct {
	ContentType string      `json:"contentType"`
	Content     string      `json:"content"`
	Params      interface{} `json:"params,omitempty"`
}

type DeployParamsBMC struct {
	NetAddress string `json:"_net"`
}

type DeployParamsBMV struct {
	BMC        Address `json:"_bmc"`
	NetAddress string  `json:"_net"`
	MTAOffset  HexInt  `json:"_offset"`
	Validators string  `json:"_validators"`
}

type BMCRelayMethodParams struct {
	Prev     string `json:"_prev"`
	Messages string `json:"_msg"`
}

type BMCFragmentMethodParams struct {
	Prev     string `json:"_prev"`
	Messages string `json:"_msg"`
	Index    int64  `json:"_idx"`
}

type BMCLinkMethodParams struct {
	Target string `json:"_link"`
}
type BMCUnlinkMethodParams struct {
	Target string `json:"_link"`
}
type BMCAddRouteMethodParams struct {
	Destination string `json:"_dst"`
	Link        string `json:"_link"`
}
type BMCRemoveRouteMethodParams struct {
	Destination string `json:"_dst"`
}

type CallParam struct {
	FromAddress Address     `json:"from" validate:"optional,t_addr_eoa"`
	ToAddress   Address     `json:"to" validate:"required,t_addr_score"`
	DataType    string      `json:"dataType" validate:"required,call"`
	Data        interface{} `json:"data"`
}

type BMCStatusParams struct {
	Target string `json:"_link"`
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
	Hash HexBytes `json:"txHash" validate:"required,t_hash"`
}

type BlockHeightParam struct {
	Height HexInt `json:"height" validate:"required,t_int"`
}
type DataHashParam struct {
	Hash HexBytes `json:"hash" validate:"required,t_hash"`
}
type ProofResultParam struct {
	BlockHash HexBytes `json:"hash" validate:"required,t_hash"`
	Index     HexInt   `json:"index" validate:"required,t_int"`
}
type ProofEventsParam struct {
	BlockHash HexBytes `json:"hash" validate:"required,t_hash"`
	Index     HexInt   `json:"index" validate:"required,t_int"`
	Events    []HexInt `json:"events"`
}

type BlockRequest struct {
	Height       HexInt         `json:"height"`
	EventFilters []*EventFilter `json:"eventFilters,omitempty"`
}

type EventFilter struct {
	Addr      Address   `json:"addr,omitempty"`
	Signature string    `json:"event"`
	Indexed   []*string `json:"indexed,omitempty"`
	Data      []*string `json:"data,omitempty"`
}

type BlockNotification struct {
	Hash    HexBytes     `json:"hash"`
	Height  HexInt       `json:"height"`
	Indexes [][]HexInt   `json:"indexes,omitempty"`
	Events  [][][]HexInt `json:"events,omitempty"`
}

type EventRequest struct {
	EventFilter
	Height HexInt `json:"height"`
}

type EventNotification struct {
	Hash   HexBytes `json:"hash"`
	Height HexInt   `json:"height"`
	Index  HexInt   `json:"index"`
	Events []HexInt `json:"events,omitempty"`
}

type WSEvent string

const (
	WSEventInit WSEvent = "WSEventInit"
)

type WSResponse struct {
	Code    int    `json:"code"`
	Message string `json:"message,omitempty"`
}

//T_BIN_DATA, T_HASH
type HexBytes string

func (hs HexBytes) Value() ([]byte, error) {
	if hs == "" {
		return nil, nil
	}
	return hex.DecodeString(string(hs[2:]))
}
func NewHexBytes(b []byte) HexBytes {
	return HexBytes("0x" + hex.EncodeToString(b))
}

//T_INT
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

func NewHexInt(v int64) HexInt {
	return HexInt("0x" + strconv.FormatInt(v, 16))
}

//T_ADDR_EOA, T_ADDR_SCORE
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

func NewAddress(b []byte) Address {
	if len(b) != 21 {
		return ""
	}
	switch b[0] {
	case 1:
		return Address("cx" + hex.EncodeToString(b[1:]))
	case 0:
		return Address("hx" + hex.EncodeToString(b[1:]))
	default:
		return ""
	}
}

//T_SIG
type Signature string

type BlockUpdate struct {
	BlockHeader []byte
	Votes       []byte
	Validators  []byte
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

type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*chain.EventProof
}

type Block struct {
	//BlockHash              HexBytes  `json:"block_hash" validate:"required,t_hash"`
	//Version                HexInt    `json:"version" validate:"required,t_int"`
	Height int64 `json:"height" validate:"required,t_int"`
	//Timestamp              int64             `json:"time_stamp" validate:"required,t_int"`
	//Proposer               HexBytes  `json:"peer_id" validate:"optional,t_addr_eoa"`
	//PrevID                 HexBytes  `json:"prev_block_hash" validate:"required,t_hash"`
	//NormalTransactionsHash HexBytes  `json:"merkle_tree_root_hash" validate:"required,t_hash"`
	NormalTransactions []struct {
		TxHash HexBytes `json:"txHash"`
		//Version   HexInt   `json:"version"`
		From Address `json:"from"`
		To   Address `json:"to"`
		//Value     HexInt   `json:"value,omitempty" `
		//StepLimit HexInt   `json:"stepLimit"`
		//TimeStamp HexInt   `json:"timestamp"`
		//NID       HexInt   `json:"nid,omitempty"`
		//Nonce     HexInt   `json:"nonce,omitempty"`
		//Signature HexBytes `json:"signature"`
		DataType string          `json:"dataType,omitempty"`
		Data     json.RawMessage `json:"data,omitempty"`
	} `json:"confirmed_transaction_list"`
	//Signature              HexBytes  `json:"signature" validate:"optional,t_hash"`
}
