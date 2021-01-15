package module

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
	Segment(rm *RelayMessage) ([]*Segment, error)
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
}
