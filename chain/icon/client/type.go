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
	"encoding/json"
	"fmt"
	"math/big"

	"github.com/icon-project/btp/common/intconv"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/types"
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

type VerifierStatus struct {
	SequenceOffset int64
	FirstMessageSn int64
	MessageCount   int64
}

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
	Serialized             []byte
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
	Index    HexInt `json:"_idx"`
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
		Height HexInt   `json:"height"`
		Extra  HexBytes `json:"extra"`
	} `json:"verifier"`
	BMRs []struct {
		Address      Address `json:"address"`
		BlockCount   HexInt  `json:"block_count"`
		MessageCount HexInt  `json:"msg_count"`
	} `json:"relays"`
	CurrentHeight HexInt `json:"cur_height"`
}

type BTPBlockParam struct {
	Height    HexInt `json:"height" validate:"required,t_int"`
	NetworkId HexInt `json:"networkID" validate:"required,t_int"`
}

type BTPNetworkInfoParam struct {
	Height HexInt `json:"height" validate:"optional,t_int"`
	Id     HexInt `json:"id" validate:"required,t_int"`
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
	return intconv.ParseInt(string(i), 64)
}

func (i HexInt) Int() (int, error) {
	v, err := intconv.ParseInt(string(i), 32)
	return int(v), err
}

func (i HexInt) BigInt() (*big.Int, error) {
	v := new(big.Int)
	err := intconv.ParseBigInt(v, string(i))
	return v, err
}

func NewHexInt(v int64) HexInt {
	return HexInt(intconv.FormatInt(v))
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

type BlockVoteUpdate struct {
	BlockHeader []byte
	Votes       []byte
	Validators  []byte
}

type BlockWitness struct {
	Height  int64
	Witness [][]byte
}
type BlockUpdate struct {
	Height    int64
	BlockHash []byte
	Header    []byte
	Proof     []byte
}
type BlockProof struct {
	Header       []byte
	BlockWitness *BlockWitness
}
type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*EventProof
	Events      []*Event
}

type EventProof struct {
	Index int
	Proof []byte
}

type Event struct {
	Next     types.BtpAddress
	Sequence int64
	Message  []byte
}

//T_SIG
type Signature string

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

//icon2 relaymessage
type BTPNotification struct {
	Header HexBytes `json:"header"`
	Proof  string   `json:"proof"`
}

type BTPRequest struct {
	Height    HexInt `json:"height"`
	NetworkID HexInt `json:"networkID"`
	ProofFlag bool   `json:"proofFlag"`
}

type NetworkInfo struct {
	StartHeight     HexInt   `json:"startHeight"`
	NetworkTypeID   HexInt   `json:"networkTypeID"`
	NetworkTypeName string   `json:"networkTypeName"`
	NetworkID       HexInt   `json:"networkID"`
	NextMessageSN   HexInt   `json:"nextMessageSN"`
	PrevNSHash      HexBytes `json:"prevNSHash"`
	LastNSHash      HexBytes `json:"lastNSHash"`
}

type BTPBlockUpdate struct {
	BTPBlockHeader []byte
	BTPBlockProof  []byte
}

type BTPBlockHeader struct {
	MainHeight             int64
	Round                  int32
	NextProofContextHash   []byte
	NetworkSectionToRoot   [][]byte
	NetworkID              int64
	UpdateNumber           int64
	PrevNetworkSectionHash []byte
	MessageCount           int64
	MessagesRoot           []byte
	NextProofContext       []byte
}
