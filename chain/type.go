package chain

import "fmt"

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

func (rm RelayMessage) HasWait() bool {
	for _, segment := range rm.Segments {
		if segment != nil && segment.GetResultParam != nil && segment.TransactionResult == nil {
			return true
		}
	}
	return false
}

func (rm RelayMessage) PendingTx() int {
	nu := 0
	for _, segment := range rm.Segments {
		if segment != nil && segment.GetResultParam != nil && segment.TransactionResult == nil {
			nu++
		}
	}

	return nu
}

func (rm RelayMessage) BuRange() string {
	return fmt.Sprintf("bu: %d ~ %d", rm.BlockUpdates[0].Height, rm.BlockUpdates[len(rm.BlockUpdates)-1].Height)
}

func (rm *RelayMessage) RemoveSegment(index int) {
	if l := len(rm.Segments); l-1 <= 0 {
		rm.Segments = nil
	} else if index == l-1 {
		rm.Segments = rm.Segments[:index]
	} else {
		rm.Segments = append(rm.Segments[:index], rm.Segments[index+1:]...)
	}
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
	// Segment split the give RelayMessage into small segments. One Segment is equal to one RelayMessage
	Segment(rm *RelayMessage, height int64) ([]*Segment, error)
	// Decode segment and update BlockProof for the given segment
	UpdateSegment(bp *BlockProof, segment *Segment) error
	// Send relay message to BMC.handleRelayMessage
	Relay(segment *Segment) (GetResultParam, error)
	// Get result based on usually transaction hash
	GetResult(p GetResultParam) (TransactionResult, error)
	GetStatus() (*BMCLinkStatus, error)
	// Monitor destination chain for update new BMCStatusLink with callback(height)
	MonitorLoop(height int64, cb MonitorCallback, scb func()) error
	StopMonitorLoop()
	FinalizeLatency() int
}

type ReceiveCallback func(bu *BlockUpdate, rps []*ReceiptProof)

type Receiver interface {
	// Monitor src chain with given transaction sequence to callback(BlockUpdate, ReceiptProof)
	ReceiveLoop(height int64, seq int64, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
}

// Suppose to be in pra package, however it occurs cycle import so place here
type ParaChainBlockUpdate struct {
	ScaleEncodedBlockHeader []byte
	FinalityProof           []byte
}

type ParaChainBlockUpdateExtra struct {
	ScaleEncodedBlockHeader []byte
	FinalityProofs          [][]byte
}
