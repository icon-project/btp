package chain

import (
	"math/big"
)

var BigIntOne = big.NewInt(1)

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
	Height      int64 //Bridge only
	EventProofs []*EventProof
	Events      []*Event
}
type EventProof struct {
	Index int
	Proof []byte
}

type Event struct {
	Next     BtpAddress
	Sequence *big.Int
	Message  []byte
}

type RelayMessage struct {
	From          BtpAddress
	BlockUpdates  []*BlockUpdate
	BlockProof    *BlockProof
	ReceiptProofs []*ReceiptProof
	Seq           uint64
	HeightOfDst   int64
	HeightOfSrc   int64

	Segments []*Segment
}

type Segment struct {
	TransactionParam  TransactionParam //possible byte array
	GetResultParam    GetResultParam
	TransactionResult TransactionResult
	//
	Height              int64
	NumberOfBlockUpdate int
	EventSequence       *big.Int
	NumberOfEvent       int
}

type BMCLinkStatus struct {
	TxSeq    *big.Int
	RxSeq    *big.Int
	Verifier struct {
		Height int64
		Extra  []byte
	}
	CurrentHeight int64
}

type TransactionParam interface{}
type GetResultParam interface{}
type TransactionResult interface{}

type MonitorCallback func(height int64) error

type Sender interface {
	Relay(segment *Segment) (GetResultParam, error)
	GetResult(p GetResultParam) (TransactionResult, error)
	GetStatus() (*BMCLinkStatus, error)
	MonitorLoop(height int64, cb MonitorCallback, scb func()) error
	StopMonitorLoop()
	FinalizeLatency() int
	TxSizeLimit() int
}

type ReceiveCallback func(bu *BlockUpdate, rps []*ReceiptProof) error

type Receiver interface {
	ReceiveLoop(height int64, seq *big.Int, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
}

type Chain interface {
	Serve(sender Sender) error
}
