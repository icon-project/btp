package base

import (
	"github.com/icon-project/btp/common/log"
	rxgo "github.com/reactivex/rxgo/v2"
)


type Client interface {
	CloseAllMonitor()
	Initialize(uri string, l log.Logger)
	ComputeBlockHash(serialized []byte) ([]byte, error)

	//sender
	MonitorSenderBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	GetBlockRequest(int64) *BlockRequest
	GetTransactionResult(*GetResultParam) (TransactionResult, error)
	GetBMCLinkStatus(destination, source BtpAddress) (*BMCLinkStatus, error)
	IsTransactionOverLimit(int) bool
	BMCRelayMethodTransactionParam(w Wallet, dst BtpAddress, prev string, rm *RelayMessageClient, stepLimit int64) (TransactionParam, error)
	SendTransaction(*TransactionParam) ([]byte, error)
	AssignHash(Wallet, *TransactionHashParam, []byte) error //TODO : Need to rename the function name
	GetTransactionParams(*Segment) (TransactionParam, error)
	CreateTransaction(wallet Wallet, p *TransactionParam) error
	SignTransaction(*TransactionParam) error
	GetRelayMethodParams(*TransactionParam) (string, string, error)
	UnmarshalFromSegment(string, *RelayMessageClient) error
	GetNonce(publicKey string, accountId string) (int64, error)

	//receiver
	MonitorReceiverBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	GetBlockHeader(int64) (*BlockHeader, error)
	GetBlockProof(*BlockNotification) ([]byte, error)
	GetEventRequest(source BtpAddress, destination BtpAddress, height int64) *BlockRequest
	GetReceiptProofs(*BlockRequest) ([]*ReceiptProof, error)
}

type Wallet interface {
	Address() string
	Sign(data []byte) ([]byte, error)
	PublicKey() []byte
	ECDH(pubKey []byte) ([]byte, error)
}

type BlockRequest interface{}

type BlockNotification interface {
	Height() int64
	Hash() []byte
}

type TransactionResultBytes interface{}
type TransactionHashParam interface{}
type EventLogFilter interface{}
type EventLog interface{}

/*---------------struct-------------------*/

type BlockHeader struct {
	Height     int64
	Hash       []byte
	Serilaized []byte
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

type BlockUpdate struct {
	Height    int64
	BlockHash []byte
	Header    []byte
	Proof     []byte
}

type BlockWitness struct {
	Height  int64
	Witness [][]byte
}

type BlockProof struct {
	Header       []byte
	BlockWitness *BlockWitness
}
type ReceiptProof struct {
	Height      int64
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

	Segments []*Segment
}

type Segment struct {
	TransactionParam  TransactionParam
	GetResultParam    GetResultParam
	TransactionResult TransactionResult
	//
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
	//BMR rotate
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

type TransactionParam interface{}
type GetResultParam interface{}
type TransactionResult interface{}

type MonitorCallback func(height int64) error

type Sender interface {
	Segment(rm *RelayMessage, height int64) ([]*Segment, error)
	UpdateSegment(bp *BlockProof, segment *Segment) error
	Relay(segment *Segment) (GetResultParam, error)
	GetResult(p GetResultParam) (TransactionResult, error)
	GetStatus() (*BMCLinkStatus, error)
	//
	MonitorLoop(height int64, cb MonitorCallback, scb func()) error
	StopMonitorLoop()
	FinalizeLatency() int
}

type ReceiveCallback func(bu *BlockUpdate, rps []*ReceiptProof)

type Receiver interface {
	ReceiveLoop(height int64, seq int64, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
	GetBlockUpdate(height int64) (*BlockUpdate, error)
}