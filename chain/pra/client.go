package pra

import (
	"context"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/icon-project/btp/chain/pra/binding"
)

type Client struct {
	ethClient *ethclient.Client
	bmc       *binding.BMC
	log       log.Logger
}

func NewClient(uri string, bmcContractAddress string, l log.Logger) *Client {
	ethClient, err := ethclient.Dial(uri)
	if err != nil {
		l.Fatal(err)
	}

	bmc, err := binding.NewBMC(common.HexToAddress(bmcContractAddress), ethClient)
	if err != nil {
		l.Fatal(err)
	}

	c := &Client{
		bmc:       bmc,
		ethClient: ethClient,
		log:       l,
	}
	return c
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

func (c *Client) MonitorBlock(cb chain.MonitorCallback) error {
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
