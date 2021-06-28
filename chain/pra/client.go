package pra

import (
	"context"
	"math/big"
	"time"

	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	gsrpc "github.com/centrifuge/go-substrate-rpc-client/v3"
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

type Client struct {
	ethClient         *ethclient.Client
	subAPI            *gsrpc.SubstrateAPI
	bmc               *binding.BMC
	log               log.Logger
	stopMonitorSignal chan bool
}

func NewClient(uri string, bmcContractAddress string, l log.Logger) *Client {
	subAPI, err := gsrpc.NewSubstrateAPI(uri)
	if err != nil {
		l.Fatal(err)
	}

	ethClient, err := ethclient.Dial(uri)
	if err != nil {
		l.Fatal(err)
	}

	bmc, err := binding.NewBMC(EvmHexToAddress(bmcContractAddress), ethClient)
	if err != nil {
		l.Fatal("failed to connect to BMC contract", err.Error())
	}

	c := &Client{
		subAPI:            subAPI,
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
	metadata, err := c.subAPI.RPC.State.GetMetadata(hash)
	if err != nil {
		return nil, err
	}

	return &SubstrateMetaData{metadata}, nil
}

func (c *Client) getSystemEventReadProofKey(hash SubstrateHash) (SubstrateStorageKey, error) {
	meta, err := c.getMetadata(hash)
	if err != nil {
		return nil, err
	}

	return CreateStorageKey(meta.Metadata, "System", "Events", nil, nil)
}

func (c *Client) getReadProof(key SubstrateStorageKey, hash SubstrateHash) (ReadProof, error) {
	var res ReadProof
	err := c.subAPI.Client.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *Client) lastFinalizedHeader() (*SubstrateHeader, error) {
	finalizedHash, err := c.subAPI.RPC.Chain.GetFinalizedHead()
	if err != nil {
		return nil, err
	}

	finalizedHeader, err := c.subAPI.RPC.Chain.GetHeader(finalizedHash)
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

	return c.subAPI.RPC.Chain.GetHeaderLatest()
}

// MonitorBlock pulls block from the given height
func (c *Client) MonitorBlock(height uint64, fetchEvent bool, cb func(v *BlockNotification) error) error {
	current := height

	for {
		select {
		case <-c.stopMonitorSignal:
			return nil
		default:
			header, err := c.bestLatestBlockHeader()
			if err != nil {
				return err
			}

			if current > uint64(header.Number) {
				c.log.Tracef("block not yet finalized target:%v latest:%v", current, header.Number)
				<-time.After(BlockRetryInterval)
				continue
			}

			hash, err := c.subAPI.RPC.Chain.GetBlockHash(current)
			if err != nil && err.Error() == ErrBlockNotReady.Error() {
				<-time.After(BlockRetryInterval)
				continue
			} else if err != nil {
				c.log.Error("failed to query latest block:%v error:%v", current, err)
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

func (c *Client) getEvents(blockHash SubstrateHash) (*SubstateWithFrontierEventRecord, error) {
	c.log.Trace("fetching block for events", "hash", blockHash.Hex())
	meta, err := c.getMetadata(blockHash)
	if err != nil {
		return nil, err
	}

	key, err := CreateStorageKey(meta.Metadata, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := c.subAPI.RPC.State.GetStorageRaw(key.StorageKey(), blockHash)
	if err != nil {
		return nil, err
	}

	records := &SubstateWithFrontierEventRecord{}
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
