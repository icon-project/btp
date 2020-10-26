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
	Index int
	Proof []byte
	//TODO [TBD] support blockchain which not serve EventProof
	EventProofs []*EventProof
}
type EventProof struct {
	Index int
	Proof []byte
}

type RelayMessage struct {
	From          BtpAddress
	BlockUpdates  []*BlockUpdate
	BlockProof    *BlockProof
	ReceiptProofs []*ReceiptProof
	Seq           uint64
}

type BMCLinkStatus struct {
	TxSeq    int64
	RxSeq    int64
	Verifier struct {
		Height     int64
		Offset     int64
		LastHeight int64
	}
}

type GetResultParam interface{}
type TransactionResult interface{}

type Sender interface {
	Relay(rm *RelayMessage) (GetResultParam, error)
	GetResult(p GetResultParam) (TransactionResult, error)
	GetStatus() (*BMCLinkStatus, error)
	LimitOfTransactionSize() int
}

type ReceiveCallback func(bu *BlockUpdate, rps []*ReceiptProof)

type Receiver interface {
	ReceiveLoop(height int64, seq int64, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
}

type RelayChain interface {
	Serve() error
}