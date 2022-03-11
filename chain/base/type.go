package base

import (
	"github.com/icon-project/btp/chain"

	"github.com/icon-project/btp/common/log"
	rxgo "github.com/reactivex/rxgo/v2"
)

/*---------------Interfaces-------------------*/

type Client interface {
	//common
	GetBlockNotificationHeight(*BlockNotification) (int64, error)
	CloseAllMonitor()
	Initialize(uri string, l log.Logger)
	ComputeBlockHash(serialized []byte) ([]byte, error)

	//sender
	MonitorSenderBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	GetBlockRequest(int64) *BlockRequest
	GetTransactionResult(*chain.GetResultParam) (chain.TransactionResult, error)
	GetBMCLinkStatus(destination, source chain.BtpAddress) (*chain.BMCLinkStatus, error)
	IsTransactionOverLimit(int) bool
	BMCRelayMethodTransactionParam(w Wallet, dst chain.BtpAddress, prev string, rm *RelayMessageClient, stepLimit int64) (chain.TransactionParam, error)
	SendTransaction(*chain.TransactionParam) ([]byte, error)
	AssignHash(Wallet, *TransactionHashParam, []byte) error //TODO : Need to rename the function name
	GetTransactionParams(*chain.Segment) (chain.TransactionParam, error)
	CreateTransaction(wallet Wallet, p *chain.TransactionParam) error
	SignTransaction(*chain.TransactionParam) error
	GetRelayMethodParams(*chain.TransactionParam) (string, string, error)
	UnmarshalFromSegment(string, *RelayMessageClient) error
	GetNonce(publicKey string, accountId string) (int64, error)

	//receiver
	MonitorReceiverBlock(p *BlockRequest, cb func(observable rxgo.Observable) error, scb func()) error
	GetBlockHeaderByHeight(int64) (*BlockHeader, error)
	GetBlockProof(*BlockNotification) ([]byte, error)
	GetEventRequest(source chain.BtpAddress, destination chain.BtpAddress, height int64) *BlockRequest
	GetReceiptProofs(*BlockRequest) ([]*chain.ReceiptProof, error)
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

type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*chain.EventProof
}