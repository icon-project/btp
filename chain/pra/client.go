package pra

import (
	"context"
	"math/big"
	"time"

	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/icon-project/btp/chain/pra/binding"
)

const (
	BlockRetryInterval       = time.Second * 1
	DefaultGasLimit          = 6721975
	DefaultGasPrice    int64 = 1000000000
	DefaultReadTimeout       = 10 * time.Second
)

type EthClient interface {
	TransactionReceipt(ctx context.Context, hash EvmHash) (*EvmReceipt, error)
	TransactionByHash(ctx context.Context, hash EvmHash) (*EvmTransaction, bool, error)
	CallContract(ctx context.Context, callMsg EvmCallMsg, block *big.Int) ([]byte, error)
}

type BMCContract interface {
	HandleRelayMessage(opts *bind.TransactOpts, _prev string, _msg string) (*EvmTransaction, error)
	ParseMessage(log EvmLog) (*binding.BMCMessage, error)
	GetStatus(opts *bind.CallOpts, _link string) (binding.TypesLinkStats, error)
}

type SubstrateClient interface {
	Call(result interface{}, method string, args ...interface{}) error
	GetMetadata(blockHash SubstrateHash) (*SubstrateMetaData, error)
	GetFinalizedHead() (SubstrateHash, error)
	GetHeader(hash SubstrateHash) (*SubstrateHeader, error)
	GetHeaderLatest() (*SubstrateHeader, error)
	GetBlockHash(blockNumber uint64) (SubstrateHash, error)
	GetStorageRaw(key SubstrateStorageKey, blockHash SubstrateHash) (*SubstrateStorageDataRaw, error)
	GetBlockHashLatest() (SubstrateHash, error)
}

type Client struct {
	ethClient         EthClient
	subClient         SubstrateClient
	bmc               BMCContract
	log               log.Logger
	stopMonitorSignal chan bool
}

func NewClient(url string, bmcContractAddress string, l log.Logger) *Client {
	subClient, err := NewSubstrateClient(url)
	if err != nil {
		l.Fatalf("failed to create Parachain Client err:%v", err.Error())
	}

	ethClient, err := ethclient.Dial(url)

	bmc, err := binding.NewBMC(EvmHexToAddress(bmcContractAddress), ethClient)
	if err != nil {
		l.Fatal("failed to connect to Parachain BMC contract", err.Error())
	}

	c := &Client{
		subClient:         subClient,
		bmc:               bmc,
		ethClient:         ethClient,
		log:               l,
		stopMonitorSignal: make(chan bool),
	}
	return c
}

func (c *Client) IsSendMessageEvent(e EventEVMLog) bool {
	_, err := c.bmc.ParseMessage(e.EvmLog())
	return err != nil
}

func (c *Client) newTransactOpts(w Wallet) *bind.TransactOpts {
	ew := w.(*wallet.EvmWallet)
	txopts := bind.NewKeyedTransactor(ew.Skey)
	txopts.GasLimit = DefaultGasLimit
	txopts.GasPrice = big.NewInt(DefaultGasPrice)
	txopts.Context = context.Background()

	return txopts
}

func (c *Client) GetTransactionReceipt(txhash string) (*EvmReceipt, error) {
	receipt, err := c.ethClient.TransactionReceipt(context.Background(), EvmHexToHash(txhash))
	if err != nil {
		return nil, err
	}

	return receipt, nil
}

func (c *Client) GetTransactionByHash(txhash string) (*EvmTransaction, bool, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultReadTimeout)
	defer cancel()
	tx, pending, err := c.ethClient.TransactionByHash(ctx, EvmHexToHash(txhash))
	if err != nil {
		return nil, pending, err
	}
	return tx, pending, err
}

func (c *Client) CloseAllMonitor() error {
	close(c.stopMonitorSignal)
	return nil
}

func (c *Client) getMetadata(hash SubstrateHash) (*SubstrateMetaData, error) {
	metadata, err := c.subClient.GetMetadata(hash)
	if err != nil {
		return nil, err
	}

	return metadata, nil
}

func (c *Client) getSystemEventReadProofKey(hash SubstrateHash) (SubstrateStorageKey, error) {
	meta, err := c.getMetadata(hash)
	if err != nil {
		return nil, err
	}

	return CreateStorageKey(meta, "System", "Events", nil, nil)
}

func (c *Client) getReadProof(key SubstrateStorageKey, hash SubstrateHash) (ReadProof, error) {
	var res ReadProof
	err := c.subClient.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *Client) lastFinalizedHeader() (*SubstrateHeader, error) {
	finalizedHash, err := c.subClient.GetFinalizedHead()
	if err != nil {
		return nil, err
	}

	finalizedHeader, err := c.subClient.GetHeader(finalizedHash)
	if err != nil {
		return nil, err
	}

	return finalizedHeader, nil
}

// bestLatestBlockHeader returns the best latest header
// in testnet if the chain do not support finalizing headers, it returns latest header
func (c *Client) bestLatestBlockHeader() (*SubstrateHeader, error) {
	finalizedHeader, err := c.lastFinalizedHeader()
	if err != nil {
		return nil, err
	}

	if finalizedHeader.Number > 0 {
		return finalizedHeader, nil
	}

	return c.subClient.GetHeaderLatest()
}

// MonitorBlock pulls block from the given height
func (c *Client) MonitorBlock(height uint64, fetchEvent bool, cb func(v *BlockNotification) error) error {
	current := height

	for {
		select {
		case <-c.stopMonitorSignal:
			return nil
		default:
			latestHeader, err := c.bestLatestBlockHeader()
			if err != nil {
				return err
			}

			if current > uint64(latestHeader.Number) {
				c.log.Debugf("block not yet finalized target:%v latest:%v", current, latestHeader.Number)
				<-time.After(BlockRetryInterval)
				continue
			}

			hash, err := c.subClient.GetBlockHash(current)
			if err != nil && err.Error() == ErrBlockNotReady.Error() {
				<-time.After(BlockRetryInterval)
				continue
			} else if err != nil {
				c.log.Error("failed to query latest block:%v error:%v", current, err)
				return err
			}

			header, err := c.subClient.GetHeader(hash)
			if err != nil {
				return err
			}

			v := &BlockNotification{
				Header: header,
				Height: current,
				Hash:   hash,
			}

			if fetchEvent {
				if events, err := c.getEvents(v.Hash); err != nil {
					return err
				} else {
					v.Events = events
				}
			}

			if err := cb(v); err != nil {
				return err
			}
			current++
		}
	}
}

func (c *Client) getEvents(blockHash SubstrateHash) (*MoonriverEventRecord, error) {
	c.log.Trace("fetching block for events", "hash", blockHash.Hex())
	meta, err := c.getMetadata(blockHash)
	if err != nil {
		return nil, err
	}

	key, err := CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := c.subClient.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	records := &MoonriverEventRecord{}
	if err = SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		return nil, err
	}

	return records, nil
}

// CallContract executes a message call transaction, which is directly executed in the VM
// of the node, but never mined into the blockchain.
func (c *Client) CallContract(callMsg EvmCallMsg, blockNumber *big.Int) ([]byte, error) {
	return c.ethClient.CallContract(context.Background(), callMsg, blockNumber)
}
