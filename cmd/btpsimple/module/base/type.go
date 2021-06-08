package base

import (
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
	"github.com/reactivex/rxgo/v2"
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}
type MonitorCallback func(height int64) error

type Client interface {
	CloseAllMonitor()
	GetBlockHeaderByHeight(int64, *BlockHeader) ([]byte, error)
	GetBlockRequest(int64) *BlockRequest
	GetBlockNotificationHash(*BlockNotification) ([]byte, error)
	GetBlockProof(*BlockHeader) ([]byte, error)
	GetEventRequest(BtpAddress, string, int64) *BlockRequest
	GetBlockNotificationHeight(*BlockNotification) (int64, error)
	GetTransactionResult(*GetResultParam) (*TransactionResult, error)
	GetBMCLinkStatus(wallet Wallet, destination, source BtpAddress) (*BMCLinkStatus, error)
	GetReceiptProofs(*BlockNotification, bool, EventLogFilter) ([]*ReceiptProof, bool, error)
	CheckRPCPendingErrorCode(err *jsonrpc.ErrorCode) bool
	MonitorReceiverBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	MonitorSenderBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	Initialize(uri string, l log.Logger)
	MapErrorWithTransactionResult(*TransactionResult, error) error
	IsTransactionOverLimit(int) bool
	BMCRelayMethodTransactionParam(w Wallet, dst, src BtpAddress, prev string, rm *RelayMessageClient, stepLimit int64) (TransactionParam, error)
	SendTransaction(*TransactionParam) ([]byte, error)
	AttachHash(*TransactionHashParam, []byte) error
	GetTransactionParams(*Segment) (TransactionParam, error)
	SignTransaction(Wallet, *TransactionParam) error
	GetRelayMethodParams(*TransactionParam) (string, string, error)
	UnmarshalFromSegment(string, *RelayMessageClient) error
}
type Sender interface {
	Segment(rm *RelayMessage, height int64) ([]*Segment, error)
	UpdateSegment(bp *BlockProof, segment *Segment) error
	Relay(segment *Segment) (GetResultParam, error)
	GetResult(p GetResultParam) (TransactionResult, error)
	GetStatus() (*BMCLinkStatus, error)
	MonitorLoop(height int64, cb MonitorCallback, scb func()) error
	StopMonitorLoop()
	FinalizeLatency() int
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
	Next     BtpAddress
	Sequence int64
	Message  []byte
}

type RelayMessage struct {
	From          BtpAddress
	BlockUpdates  []*BlockUpdate
	BlockProof    *BlockProof
	ReceiptProofs []*ReceiptProof
	Seq           uint64
	HeightOfDst   int64
	Segments      []*Segment
}

type RelayMessageClient struct {
	BlockUpdates        [][]byte
	BlockProof          []byte
	ReceiptProofs       [][]byte
	height              int64
	numberOfBlockUpdate int
	eventSequence       int64
	numberOfEvent       int
}

type Segment struct {
	TransactionParam    TransactionParam
	GetResultParam      GetResultParam
	TransactionResult   TransactionResult
	Height              int64
	NumberOfBlockUpdate int
	EventSequence       int64
	NumberOfEvent       int
}

type BMCLinkStatus struct {
	TxSeq    int64
	RxSeq    int64
	Verifier struct {
		Height     int64
		Offset     int64
		LastHeight int64
	}
	BMRs []struct {
		Address      string
		BlockCount   int64
		MessageCount int64
	}
	BMRIndex         int
	RotateHeight     int64
	RotateTerm       int
	DelayLimit       int
	MaxAggregation   int
	CurrentHeight    int64
	RxHeight         int64
	RxHeightSrc      int64
	BlockIntervalSrc int
	BlockIntervalDst int
}

type BlockRequest interface{}
type BlockNotification interface{}
type TransactionParam interface{}
type GetResultParam interface{}
type TransactionResult interface{}
type TransactionResultBytes interface{}
type TransactionHashParam interface{}
type ReceiveCallback func(bu *BlockUpdate, rps []*ReceiptProof)
type EventLogFilter interface{}
type EventLog interface{}

type Receiver interface {
	ReceiveLoop(height int64, seq int64, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
}
