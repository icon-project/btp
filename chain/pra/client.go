package pra

import (
	"context"
	"sync"
	"time"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	srpc "github.com/centrifuge/go-substrate-rpc-client/v3"
	sclient "github.com/centrifuge/go-substrate-rpc-client/v3/client"
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
)

type Client struct {
	ethClient *ethclient.Client
	subAPI    *srpc.SubstrateAPI
	bmc       *binding.BMC
	log       log.Logger
	meta      *stypes.Metadata
	mutex     *sync.RWMutex
}

func NewClient(uri string, bmcContractAddress string, l log.Logger) *Client {
	// subAPI, err := srpc.NewSubstrateAPI(uri)
	// if err != nil {
	// 	l.Fatal(err)
	// }

	// meta, err := subAPI.RPC.State.GetMetadataLatest()
	// if err != nil {
	// 	l.Fatal(err)
	// }

	ethClient, err := ethclient.Dial(uri)
	if err != nil {
		l.Fatal(err)
	}

	bmc, err := binding.NewBMC(common.HexToAddress(bmcContractAddress), ethClient)
	if err != nil {
		l.Fatal("got error when connect to BMC contract", err.Error())
	}

	c := &Client{
		mutex: &sync.RWMutex{},
		// meta:      meta,
		// subAPI:    subAPI,
		bmc:       bmc,
		ethClient: ethClient,
		log:       l,
	}
	return c
}

func (c *Client) getMetadata() *stypes.Metadata {
	c.mutex.RLock()
	defer c.mutex.RUnlock()

	clone := new(stypes.Metadata)
	*clone = *c.meta
	return clone
}

func (c *Client) updateMetatdata() error {
	c.mutex.RLock()
	defer c.mutex.RUnlock()

	meta, err := c.subAPI.RPC.State.GetMetadataLatest()
	if err != nil {
		return err
	}
	c.meta = meta
	return nil
}

func (c *Client) newTransactOpts(w Wallet) *bind.TransactOpts {
	ew := w.(*wallet.EvmWallet)
	return bind.NewKeyedTransactor(ew.Skey)
}

func (c *Client) GetTransactionReceipt(txhash common.Hash) (*types.Receipt, error) {
	return c.ethClient.TransactionReceipt(context.Background(), txhash)
}

func (c *Client) GetTransactionByHash(txhash common.Hash) (*types.Transaction, bool, error) {
	return c.ethClient.TransactionByHash(context.Background(), txhash)
}

func (c *Client) MonitorEvmBlock(cb chain.MonitorCallback) error {

	headers := make(chan *types.Header)
	sub, err := c.ethClient.SubscribeNewHead(context.Background(), headers)
	if err != nil {
		return err
	}

	go func() {
		for {
			select {
			case err := <-sub.Err():
				log.Fatal(err)
			case header := <-headers:
				cb(header.Number.Int64())
				c.log.Debugf("MonitorBlock %v", header.Number.Int64())
			}
		}
	}()

	return nil
}

func (c *Client) MonitorEvent() error {
	return nil
}

func (c *Client) CloseAllMonitor() error {
	return nil
}

func (c *Client) getReadProof(key stypes.StorageKey, hash *stypes.Hash) ([]byte, error) {
	var res string
	err := sclient.CallWithBlockHash(c.subAPI.Client, &res, "state_getStorage", hash, key.Hex())
	if err != nil {
		return nil, err
	}

	bz, err := stypes.HexDecodeString(res)
	if err != nil {
		return nil, err
	}

	return bz, nil
}

func (c *Client) getSystemEventReadProofKey() (stypes.StorageKey, error) {
	meta := c.getMetadata()

	key, err := stypes.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	return key, nil
}

func (c *Client) queryStorage(prefix, method string, arg1, arg2 []byte, result interface{}) (bool, error) {

	key, err := stypes.CreateStorageKey(c.getMetadata(), prefix, method, arg1, arg2)
	if err != nil {
		return false, err
	}
	return c.subAPI.RPC.State.GetStorageLatest(key, result)
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
			c.log.Trace("Block not yet finalized", "target", currentBlock, "latest", finalizedHeader.Number)
			time.Sleep(BlockRetryInterval)
			continue
		}

		hash, err := c.subAPI.RPC.Chain.GetBlockHash(currentBlock)
		if err != nil && err.Error() == ErrBlockNotReady.Error() {
			time.Sleep(BlockRetryInterval)
			continue
		} else if err != nil {
			c.log.Error("Failed to query latest block", "block", currentBlock, "err", err)
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

func (c *Client) getEvents(hash stypes.Hash) (stypes.EventRecordsRaw, error) {
	c.log.Trace("Fetching block for events", "hash", hash.Hex())
	meta := c.getMetadata()

	key, err := stypes.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	var records stypes.EventRecordsRaw
	_, err = c.subAPI.RPC.State.GetStorage(key, &records, hash)
	if err != nil {
		return nil, err
	}

	return records, nil
}
