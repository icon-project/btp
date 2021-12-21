package pra

import (
	"context"
	"math/big"
	"regexp"
	"time"

	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"

	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/icon-project/btp/chain/pra/binding"
	"github.com/icon-project/btp/chain/pra/substrate"
)

const (
	BlockRetryInterval = time.Second * 1
	DefaultReadTimeout = 10 * time.Second
)

var RetryHTTPError = regexp.MustCompile(`connection reset by peer|EOF`)

type EthClient interface {
	TransactionReceipt(ctx context.Context, hash EvmHash) (*EvmReceipt, error)
	TransactionByHash(ctx context.Context, hash EvmHash) (*EvmTransaction, bool, error)
	CallContract(ctx context.Context, callMsg EvmCallMsg, block *big.Int) ([]byte, error)
	SuggestGasPrice(ctx context.Context) (*big.Int, error)
	ChainID(ctx context.Context) (*big.Int, error)
	PendingNonceAt(ctx context.Context, account common.Address) (uint64, error)
}

type BMCContract interface {
	HandleRelayMessage(opts *bind.TransactOpts, _prev string, _msg string) (*EvmTransaction, error)
	ParseMessage(log EvmLog) (*binding.BMCMessage, error)
	GetStatus(opts *bind.CallOpts, _link string) (binding.TypesLinkStats, error)
}

type Client struct {
	ethClient         EthClient
	subClient         substrate.SubstrateClient
	bmc               BMCContract
	log               log.Logger
	stopMonitorSignal chan bool
}

func NewClient(url string, bmcContractAddress string, l log.Logger) *Client {
	subClient, err := substrate.NewSubstrateClient(url)
	if err != nil {
		l.Fatalf("failed to create Parachain Client err:%v", err.Error())
	}

	subClient.Init()

	c := &Client{
		subClient:         subClient,
		log:               l,
		stopMonitorSignal: make(chan bool),
	}

	if len(bmcContractAddress) > 0 {

		ethClient, err := ethclient.Dial(url)
		if err != nil {
			l.Fatal("failed to connect to Parachain EVM", err.Error())
		}

		bmc, err := binding.NewBMC(EvmHexToAddress(bmcContractAddress), ethClient)
		if err != nil {
			l.Fatal("failed to connect to Parachain BMC contract", err.Error())
		}
		c.bmc = bmc
		c.ethClient = ethClient
	}

	return c
}

func (c *Client) SubstrateClient() substrate.SubstrateClient {
	return c.subClient
}

func (c *Client) newTransactOpts(w Wallet) *bind.TransactOpts {
	ew := w.(*wallet.EvmWallet)
	context := context.Background()
	chainID, err := c.ethClient.ChainID(context)
	if err != nil {
		log.Panicf("failed to get ChainID err:%v", err.Error())
	}
	txopts, err := bind.NewKeyedTransactorWithChainID(ew.Skey, chainID)
	if err != nil {
		log.Panicf("failed to create a transaction signer from a single private key err:%v", err.Error())
	}
	txopts.GasPrice, err = c.ethClient.SuggestGasPrice(context)
	if err != nil {
		log.Panicf("failed to get suggest gas price err:%v", err.Error())
	}
	txopts.Context = context
	nonce, err := c.ethClient.PendingNonceAt(context, txopts.From)
	if err != nil {
		log.Panicf("failed to get pending nonce at err:%v", err.Error())
	}
	txopts.Nonce = new(big.Int).SetUint64(nonce)

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

func (c *Client) lastFinalizedHeader() (*substrate.SubstrateHeader, error) {
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
func (c *Client) bestLatestBlockHeader() (*substrate.SubstrateHeader, error) {
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
func (c *Client) MonitorBlock(height uint64, fetchHeader bool, cb func(v *BlockNotification) error) error {
	current := height

	for {
		select {
		case <-c.stopMonitorSignal:
			return nil
		default:
			latestHeader, err := c.bestLatestBlockHeader()
			if err != nil && RetryHTTPError.MatchString(err.Error()) {
				<-time.After(BlockRetryInterval)
				continue
			} else if err != nil {
				return err
			}

			if current > uint64(latestHeader.Number) {
				c.log.Tracef("block not yet finalized target:%v latest:%v", current, latestHeader.Number)
				<-time.After(BlockRetryInterval)
				continue
			}

			hash, err := c.subClient.GetBlockHash(current)
			if err != nil && (err.Error() == ErrBlockNotReady.Error() || RetryHTTPError.MatchString(err.Error())) {
				<-time.After(BlockRetryInterval)
				continue
			} else if err != nil {
				c.log.Error("failed to query latest block:%v error:%v", current, err)
				return err
			}

			v := &BlockNotification{
				Height: current,
				Hash:   hash,
			}

			if fetchHeader {
				header, err := c.subClient.GetHeader(hash)
				if err != nil {
					return err
				}

				v.Header = header
			}

			if err := cb(v); err != nil {
				return err
			}
			current++
		}
	}
}

// CallContract executes a message call transaction, which is directly executed in the VM
// of the node, but never mined into the blockchain.
func (c *Client) CallContract(callMsg EvmCallMsg, blockNumber *big.Int) ([]byte, error) {
	return c.ethClient.CallContract(context.Background(), callMsg, blockNumber)
}
