package pra

import (
	"context"
	"math/big"
	"time"

	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	srpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	stypes "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/icon-project/btp/chain/pra/binding"
)

// TODO: Create general types to avoid confusing between Substrate and Ethereum library.

const (
	BlockRetryInterval = time.Second * 1
	DefaultGasLimit    = 6721975
	DefaultGasPrice    = 20000000000
)

type Client struct {
	ethClient *ethclient.Client
	subAPI    *srpc.SubstrateAPI
	bmc       *binding.BMC
	log       log.Logger
}

func NewClient(uri string, bmcContractAddress string, l log.Logger) *Client {
	subAPI, err := srpc.NewSubstrateAPI(uri)
	if err != nil {
		l.Fatal(err)
	}

	ethClient, err := ethclient.Dial(uri)
	if err != nil {
		l.Fatal(err)
	}

	bmc, err := binding.NewBMC(common.HexToAddress(bmcContractAddress), ethClient)
	if err != nil {
		l.Fatal("failed to connect to BMC contract", err.Error())
	}

	c := &Client{
		subAPI:    subAPI,
		bmc:       bmc,
		ethClient: ethClient,
		log:       l,
	}
	return c
}

func (c *Client) IsSendMessageEvent(e EventEVMLog) bool {
	topics := []common.Hash{}

	for _, t := range e.Topics {
		topics = append(topics, common.HexToHash(t.Hex()))
	}

	_, err := c.bmc.ParseMessage(types.Log{
		Address: common.Address(e.Log.Address),
		Topics:  topics,
		Data:    []byte(e.Log.Data),
	})

	return err != nil
}

func (c *Client) newTransactOpts(w Wallet) *bind.TransactOpts {
	ew := w.(*wallet.EvmWallet)
	txopts := bind.NewKeyedTransactor(ew.Skey)
	txopts.GasLimit = DefaultGasLimit
	txopts.GasPrice = big.NewInt(DefaultGasPrice)

	return txopts
}

func (c *Client) GetTransactionReceipt(txhash common.Hash) (*types.Receipt, error) {
	return c.ethClient.TransactionReceipt(context.Background(), txhash)
}

func (c *Client) GetTransactionByHash(txhash common.Hash) (*types.Transaction, bool, error) {
	return c.ethClient.TransactionByHash(context.Background(), txhash)
}

func (c *Client) CloseAllMonitor() error {
	//TODO implement logic to stop monitoring
	return nil
}

func (c *Client) getMetadata(hash stypes.Hash) (*stypes.Metadata, error) {
	// TODO optimize metadata fetching
	return c.subAPI.RPC.State.GetMetadata(hash)
}

func (c *Client) getSystemEventReadProofKey(hash stypes.Hash) (stypes.StorageKey, error) {
	meta, err := c.getMetadata(hash)
	if err != nil {
		return nil, err
	}

	key, err := stypes.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	return key, nil
}

func (c *Client) getReadProof(key stypes.StorageKey, hash stypes.Hash) (ReadProof, error) {
	var res ReadProof
	err := c.subAPI.Client.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *Client) MonitorSubstrateBlock(h uint64, cb func(events *BlockNotification) error) error {
	currentBlock := h
	for {
		finalizedHash, err := c.subAPI.RPC.Chain.GetFinalizedHead()
		if err != nil {
			return err
		}

		finalizedHeader, err := c.subAPI.RPC.Chain.GetHeader(finalizedHash)
		if err != nil {
			return err
		}

		if currentBlock > uint64(finalizedHeader.Number) {
			c.log.Tracef("block not yet finalized target:%v latest:%v", currentBlock, finalizedHeader.Number)
			time.Sleep(BlockRetryInterval)
			continue
		}

		hash, err := c.subAPI.RPC.Chain.GetBlockHash(currentBlock)
		if err != nil && err.Error() == ErrBlockNotReady.Error() {
			time.Sleep(BlockRetryInterval)
			continue
		} else if err != nil {
			c.log.Error("failed to query latest block:%v error:%v", currentBlock, err)
			return err
		}

		events, err := c.getEvents(hash)
		if err != nil {
			return err
		}

		if err := cb(&BlockNotification{
			Header: finalizedHeader,
			Height: currentBlock,
			Hash:   hash,
			Events: events,
		}); err != nil {
			return err
		}
		currentBlock++
	}
}

func (c *Client) getEvents(hash stypes.Hash) (*SubstateWithFrontierEventRecord, error) {
	c.log.Trace("fetching block for events", "hash", hash.Hex())
	meta, err := c.getMetadata(hash)
	if err != nil {
		return nil, err
	}

	key, err := stypes.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := c.subAPI.RPC.State.GetStorageRaw(key, hash)
	if err != nil {
		return nil, err
	}

	records := &SubstateWithFrontierEventRecord{}
	if err = stypes.EventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		return nil, err
	}

	return records, nil
}
