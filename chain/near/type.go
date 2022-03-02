package near

import (
	"bytes"
	"crypto/ed25519"
	"encoding/binary"
	"fmt"
	"math/big"

	"strconv"

	"github.com/btcsuite/btcutil/base58"
	"github.com/icon-project/btp/chain"
	"github.com/near/borsh-go"
)

type HexInt string

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

type Blockheader struct {
	Innerlite     Innerlite `json:"inner_lite"`
	InnerRestHash string    `json:"inner_rest_hash"`
	PrevBlockHash string    `json:"prev_block_hash"`
}

type GetStatus struct {
	Link string `json:"link"`
}
type CallFunctionResult struct {
	Result      []byte `json:"result"`
	BlockHeight int64  `json:"block_height"`
	BlockHash   string `json:"block_hash"`
}
type CallFunction struct {
	RequestType  string `json:"request_type"`
	Finality     string `json:"finality"`
	AccountId    string `json:"account_id"`
	MethodName   string `json:"method_name"`
	ArgumentsB64 string `json:"args_base64"`
}

// type ChangeProofParam struct {
// 	Type       string `json:"type"`
// 	ReceiptId  string `json:"receipt_id"`
// 	ReceiverId string `json:"receiver_id"`
// 	BlockHash  string `json:"light_client_head"`
// }

// type ChangeProofresult struct {
// 	BlockHeaderLite  Blockheaderlite    `json:"block_header_lite"`
// 	BlockProof       []interface{}      `json:"block_proof"`
// 	OutComeProofs    OutComeProof       `json:"outcome_proof"`
// 	OutcomeRootProod []Outcomerootproof `json:"outcome_root_proof"`
// }

type Outcomerootproof struct {
	Direction string `json:"direction"`
	Hash      string `json:"hash"`
}
type Blockheaderlite struct {
	Innerlite         Innerlite `json:"inner_lite"`
	InnerRestHash     string    `json:"inner_rest_hash"`
	PreviousBlockHash string    `json:"prev_block_hash"`
}

type OutComeProof struct {
	BlockHash string  `json:"block_hash"`
	ReceiptId string  `json:"id"`
	Outcomes  Outcome `json:"outcome"`
	Proofs    []Proof `json:"proof"`
}
type Proof struct {
	Direction string `json:"direction"`
	Hash      string `json:"hash"`
}

type Innerlite struct {
	Height          uint64 `json:"height"`
	EpochId         string `json:"epoch_id"`
	NextEpochId     string `json:"next_epoch_id"`
	PrevStateRoot   string `json:"prev_state_root"`
	OutcomeRoot     string `json:"outcome_root"`
	TimeStamp       string `json:"timestamp_nanosec"`
	NextBpHash      string `json:"next_bp_hash"`
	BlockMerkleRoot string `json:"block_merkle_root"`
}

type InnerLiteDecode struct {
	Timestamp       []byte
	NextBpHash      []byte
	BlockMerkleRoot []byte
}

func (i *Innerlite) Serialize() []byte {
	ts, _ := strconv.Atoi(i.TimeStamp)
	bs := make([]byte, 8)
	binary.LittleEndian.PutUint64(bs, uint64(ts))

	bh := make([]byte, 8)
	binary.LittleEndian.PutUint64(bh, i.Height)
	innerLite := new(bytes.Buffer)
	innerLite.Write(bh)
	innerLite.Write(base58.Decode(i.EpochId))
	innerLite.Write(base58.Decode(i.NextEpochId))
	innerLite.Write(base58.Decode(i.PrevStateRoot))
	innerLite.Write(base58.Decode(i.OutcomeRoot))
	innerLite.Write(bs)
	innerLite.Write(base58.Decode(i.NextBpHash))
	innerLite.Write(base58.Decode(i.BlockMerkleRoot))

	return innerLite.Bytes()
}

type TransactionResultWithReceipts struct {
	TXReceipts         []Receipts              `json:"receipts"`
	ReceiptsOutcome    []ReceiptsOutcome       `json:"receipts_outcome"`
	Status             TransactionResultStatus `json:"status"`
	Transaction        Tx                      `json:"transaction"`
	TransactionOutcome ReceiptsOutcome         `json:"transaction_outcome"`
}

type Tx struct {
	Action     []Actions `json:"actions"`
	Hash       string    `json:"hash"`
	Nonce      int64     `json:"nonce"`
	PublicKey  string    `json:"public_key"`
	ReceiverId string    `json:"receiver_id"`
	Signature  string    `json:"signature"`
	SignerId   string    `json:"signer_id"`
}

type ReceiptsOutcome struct {
	BlockHash string        `json:"block_hash"`
	Id        string        `json:"id"`
	Ocome     Outcome       `json:"outcome"`
	Proof     []interface{} `json:"proof"`
}

type Outcome struct {
	ExecutorId  string   `json:"executor_id"`
	GasBurnt    int64    `json:"gas_burnt`
	MetaData    Metadata `json:"metadata"`
	ReceiptId   []string `json:"receipt_ids"`
	Status      Success  `json:"status"`
	TokensBurnt string   `json:"tokens_burnt"`
}

type Success struct {
	SuccessValue string `json:"SuccessValue"`
}
type Metadata struct {
	GasProfile []Gasprofile `json:"gas_profile"`
}
type Gasprofile struct {
	Cost         string `json:"cost"`
	CostCategory string `json:"cost_category"`
	GasUsed      string `json:"gas_used"`
}

type Receipts struct {
	PredecessorID string  `json:"predecessor_id"`
	Receipt       Receipt `json:"receipt"`
	ReceiptId     string  `json:"receipt_id"`
	ReceiverID    string  `json:"receiver_id"`
}

type Receipt struct {
	Action          []Actions   `json:"actions"`
	GasPrice        string      `json:"gas_price"`
	InputDataId     interface{} `json:"input_data_ids"`
	SignerId        string      `json:"signer_id"`
	SignerPublicKey string      `json:"signer_public_key"`
}

type Actions interface {
}
type BlockHeightParam struct {
	BlockID int64 `json:"block_id"`
}
type GetResultParam struct {
	Txid     string
	SenderId string
}

//Transaction Status Or Results
type TransactionResult struct {
	Status             TransactionResultStatus `json:"status"`
	Transaction        NearTransaction         `json:"transaction"`
	TransactionOutcome NearTransactionOutcome  `json:"transaction_outcome"`
}

type NearTransaction struct {
	SignerId   string        `json:"signer_id"`
	PublicKey  string        `json:"public_key"`
	Nonce      int           `json:"nonce"`
	ReceiverId string        `json:"receiver_id"`
	Actions    []interface{} `json:"actions"`
	Signature  string        `json:"signature"`
	Txid       string        `json:"hash"`
}

type NearTransactionOutcome struct {
	Proof     []interface{} `json:"proof"`
	BlockHash string        `json:"block_hash"`
	Id        string        `json:"id"`
	Outcome   NearOutcome   `json:"outcome"`
}

type NearOutcome struct {
	ReceiptIds  []string          `json:"receipt_ids"`
	GasBurnt    int64             `json:"gas_burnt"`
	TokensBurnt string            `json:"tokens_burnt"`
	ExecutorId  string            `json:"executor_id"`
	Status      map[string]string `json:"status"`
}

type TransactionResultStatus struct {
	SuccessValue     string  `json:"SuccessValue"`
	SuccessReceiptId string  `json:"SuccessReceiptId"`
	Failure          Failure `json:"Failure"`
	Unknown          string  `json:"Unknown"`
}

type Failure struct {
	ActionError ActionError `json:"ActionError"`
}

type ActionError struct {
	Index float64 `json:"index"`
	Kind  Kind    `json:"kind"`
}

type Kind struct {
	AccountDoesNotExist AccountDoesNotExist `json:"AccountDoesNotExist"`
}

type AccountDoesNotExist struct {
	AccountId string `json:"account_id"`
}

type ChainStatus struct {
	ChainId  string        `json:"chain_id"`
	SyncInfo ChainSyncInfo `json:"sync_info"`
}

type BlockRequest struct {
	Height       int64       `json:"height"`
	EventFilters EventFilter `json:"eventFilters,omitempty"`
}

type EventFilter struct {
	AccountId   string `json:"addr,omitempty"`
	Message     string
	Destination chain.BtpAddress
}
type Address string

type ChainSyncInfo struct {
	LatestBlockHash   string `json:"latest_block_hash"`
	LatestBlockHeight int64  `json:"latest_block_height"`
	LatestBlockTime   string `json:"latest_block_time"`
	Syncing           bool   `json:"syncing"`
}

type Transacparm struct {
	PublicKey  ed25519.PublicKey
	TransacHex []byte
	PrivateKey ed25519.PrivateKey
	Signature  []byte
}
type NonceParam struct {
	AccountId    string `json:"account_id"`
	PublicKey    string `json:"public_key"`
	Finality     string `json:"finality"`
	Request_type string `json:"request_type"`
}
type NonceResponse struct {
	Nonce       int64  `json:"nonce"`
	Permission  string `json:"permission"`
	BlockHeight int64  `json:"block_height"`
	BlockHash   string `json:"block_hash"`
	Error       string `json:"error"`
}

type Nextbp struct {
	AccountId string `json:"account_id"`
	PublicKey string `json:"public_key"`
	Stake     string `json:"stake"`
}

type LightClientResponse struct {
	PreviousBlockHash  string              `json:"prev_block_hash"`
	NextBlockInnerHash string              `json:"next_block_inner_hash"`
	Innerlite          Innerlite           `json:"inner_lite"`
	InnerRestHash      string              `json:"inner_rest_hash"`
	NextBps            []Nextbp            `json:"next_bps"`
	ApprovalsAfterNext []ApprovalAfterNext `json:"approvals_after_next"`
}

type ApprovalAfterNext string

type LightClientResponseDecoded struct {
	PreviousBlockHash  []byte
	NextBlockInnerHash []byte
	InnerLiteBytes     InnerLiteByte
	InnerRestBytes     []byte
	NextBpsBytes       []NextBpByte
	ApprovalsAfterNext []ApprovalAfterNextByte
}

type InnerLiteByte struct {
	Height          []byte
	EpochId         []byte
	NextEpochId     []byte
	PrevStateRoot   []byte
	OutcomeRoot     []byte
	TimeStamp       []byte
	NextBpHash      []byte
	BlockMerkleRoot []byte
}

type NextBpByte struct {
	AccoutnId []byte
	PublicKey []byte
	Stake     []byte
}

type ApprovalAfterNextByte []byte

func (l *LightClientResponse) Decode() (*LightClientResponseDecoded, error) {

	var result LightClientResponseDecoded

	// for innerlite bytes
	//convert timestamp form string to int
	timestamp, err := strconv.Atoi(l.Innerlite.TimeStamp)

	if err != nil {
		return nil, fmt.Errorf("Failed to decode lightclient response")
	}

	//convert int to bytes using littleEndian byte order
	timestampbytes := make([]byte, 8)

	binary.LittleEndian.PutUint64(timestampbytes, uint64(timestamp))

	heightinBytes := make([]byte, 8)

	binary.LittleEndian.PutUint64(heightinBytes, l.Innerlite.Height)

	innerLite := InnerLiteByte{
		Height:          heightinBytes,
		EpochId:         base58.Decode(l.Innerlite.EpochId),
		NextEpochId:     base58.Decode(l.Innerlite.NextEpochId),
		PrevStateRoot:   base58.Decode(l.Innerlite.PrevStateRoot),
		OutcomeRoot:     base58.Decode(l.Innerlite.OutcomeRoot),
		TimeStamp:       timestampbytes,
		NextBpHash:      base58.Decode(l.Innerlite.NextBpHash),
		BlockMerkleRoot: base58.Decode(l.Innerlite.BlockMerkleRoot),
	}

	result.PreviousBlockHash = base58.Decode(l.PreviousBlockHash)
	result.NextBlockInnerHash = base58.Decode(l.NextBlockInnerHash)
	result.InnerLiteBytes = innerLite

	result.InnerRestBytes = base58.Decode(l.InnerRestHash)
	for _, bp := range l.NextBps {

		result.NextBpsBytes = append(result.NextBpsBytes, NextBpByte{
			AccoutnId: base58.Decode(bp.AccountId),
			PublicKey: base58.Decode(bp.PublicKey),
			Stake:     base58.Decode(bp.Stake),
		})

	}

	for _, ap := range l.ApprovalsAfterNext {

		result.ApprovalsAfterNext = append(result.ApprovalsAfterNext, []byte(ap))
	}

	return &result, nil
}

type LightClientParam struct {
	BlockHash string
}

type Result struct {
	Blockhash string
}

type BlockRequestHash struct {
	BlockId string `json:"block_id"`
}

type BlockRequestHeight struct {
	BlockId int64 `json:"block_id"`
}

type ContractStateChangeRequest struct {
	ChangeType      string   `json:"changes_type"`
	AccountIds      []string `json:"account_ids"`
	KeyPrefixBase64 string   `json:"key_prefix_base64"`
	BlockId         int64    `json:"block_id"`
}

type Change struct {
	Cause Cause      `json:"cause"`
	Type  string     `json:"type"`
	Data  ChangeData `json:"change"`
}

type Cause struct {
	Type        string `json:"type"`
	ReceiptHash string `json:"receipt_hash"`
}

type ContractStateChangeResponse struct {
	BlockHash string   `json:"block_hash"`
	Changes   []Change `json:"changes"`
}

type ChangeData struct {
	AccountId   string `json:"account_id"`
	KeyBase64   string `json:"key_base64"`
	ValueBase64 string `json:"value_base64"`
}

type MonitorBlockData struct {
	BlockHash string
	BlockId   int64
}

type VerifierStatus struct {
	Height     int64 `json:"mta_height"`
	Offset     int64 `json:"mta_offset"`
	LastHeight int64 `json:"last_height"`
}
type BMCStatus struct {
	TxSeq    int64  `json:"tx_seq"`
	RxSeq    int64  `json:"rx_seq"`
	Verifier string `json:"verifier"`
	BMRs     []struct {
		Address      Address `json:"account_id"`
		BlockCount   int64   `json:"block_count"`
		MessageCount int64   `json:"message_count"`
	} `json:"relays"`
	BMRIndex         int   `json:"relay_index"`
	RotateHeight     int64 `json:"rotate_height"`
	RotateTerm       int   `json:"rotate_term"`
	DelayLimit       int   `json:"delay_limit"`
	MaxAggregation   int   `json:"max_agg"`
	CurrentHeight    int64 `json:"current_height"`
	RxHeight         int64 `json:"rx_height"`
	RxHeightSrc      int64 `json:"rx_height_src"`
	BlockIntervalSrc int   `json:"block_interval_src"`
	BlockIntervalDst int   `json:"block_interval_dst"`
}

type AccessKey struct {
	Nonce      uint64
	Permission AccessKeyPermission
}

// AccessKeyPermission encodes a NEAR access key permission.
type AccessKeyPermission struct {
	Enum         borsh.Enum `borsh_enum:"true"` // treat struct as complex enum when serializing/deserializing
	FunctionCall FunctionCallPermission
	FullAccess   borsh.Enum
}

// FunctionCallPermission encodes a NEAR function call permission (an access
// key permission).
type FunctionCallPermission struct {
	Allowance   *big.Int
	ReceiverId  string
	MethodNames []string
}

type PublicKey struct {
	KeyType uint8
	Data    [32]byte
}

// A Transaction encodes a NEAR transaction.
type Transaction struct {
	SignerID   string
	PublicKey  PublicKey
	Nonce      uint64
	ReceiverID string
	BlockHash  [32]byte
	Actions    []Action
}

// Action simulates an enum for Borsh encoding.
type Action struct {
	Enum           borsh.Enum `borsh_enum:"true"` // treat struct as complex enum when serializing/deserializing
	CreateAccount  borsh.Enum
	DeployContract DeployContract
	FunctionCall   FunctionCall
	Transfer       Transfer
	Stake          Stake
	AddKey         AddKey
	DeleteKey      DeleteKey
	DeleteAccount  DeleteAccount
}

// The DeployContract action.
type DeployContract struct {
	Code []byte
}

// The FunctionCall action.
type FunctionCall struct {
	MethodName string
	Args       []byte
	Gas        uint64
	Deposit    big.Int // u128
}

// The Transfer action.
type Transfer struct {
	Deposit big.Int // u128
}

// The Stake action.
type Stake struct {
	Stake     big.Int // u128
	PublicKey PublicKey
}

// The AddKey action.
type AddKey struct {
	PublicKey PublicKey
	AccessKey AccessKey
}

// The DeleteKey action.
type DeleteKey struct {
	PublicKey PublicKey
}

// The DeleteAccount action.
type DeleteAccount struct {
	BeneficiaryID string
}

// A Signature used for signing transaction.
type Signature struct {
	KeyType uint8
	Data    [64]byte
}

// SignedTransaction encodes signed transactions for NEAR.
type SignedTransaction struct {
	Transaction Transaction
	Signature   Signature
}

type Error struct {
	Name    string `json:"name"`
	Cause   Causes `json:"cause"`
	Code    int64  `json:"code"`
	Message string `json:"message"`
	Data    string `json:"data"`
}
type Causes struct {
	Name string      `json:"name"`
	Info interface{} `json:"info"`
}

// type RelayMessageParam struct {
// 	Previous string `json:"_prev"`
// 	Message  string `json:"_msg"`
// }

// type TransactionParam struct {
// 	From              string
// 	To                string
// 	RelayMessage      RelayMessageParam
// 	Base64encodedData string
// }
