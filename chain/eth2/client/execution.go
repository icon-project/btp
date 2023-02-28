package client

import (
	"context"
	"crypto/ecdsa"
	"math/big"
	"time"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/ethereum/go-ethereum/rpc"

	"github.com/icon-project/btp/common/log"
)

const (
	DefaultTimeout  = 10 * time.Second //
	DefaultGasLimit = 8000000
)

type ExecutionLayer struct {
	client  *ethclient.Client
	chainID *big.Int
	log     log.Logger
}

func NewExecutionLayer(url string, log log.Logger) (*ExecutionLayer, error) {
	rpcClient, err := rpc.Dial(url)
	if err != nil {
		return nil, err
	}
	c := &ExecutionLayer{
		client: ethclient.NewClient(rpcClient),
		log:    log,
	}
	c.chainID, err = c.GetChainID()
	if err != nil {
		return nil, err
	}

	return c, nil
}

func (c *ExecutionLayer) GetBackend() bind.ContractBackend {
	return c.client
}

func (c *ExecutionLayer) NewTransactOpts(k *ecdsa.PrivateKey) (*bind.TransactOpts, error) {
	txo, err := bind.NewKeyedTransactorWithChainID(k, c.chainID)
	if err != nil {
		return nil, err
	}
	txo.GasPrice, _ = c.client.SuggestGasPrice(context.Background())
	txo.GasLimit = uint64(DefaultGasLimit)
	return txo, nil
}

func (c *ExecutionLayer) GetChainID() (*big.Int, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.ChainID(ctx)
}

func (c *ExecutionLayer) BlockByNumber(num *big.Int) (*types.Block, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.BlockByNumber(ctx, num)
}

func (c *ExecutionLayer) TransactionByHash(hash common.Hash) (tx *types.Transaction, isPending bool, err error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.TransactionByHash(ctx, hash)
}

func (c *ExecutionLayer) TransactionReceipt(txHash common.Hash) (*types.Receipt, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.TransactionReceipt(ctx, txHash)
}

func (c *ExecutionLayer) FilterLogs(fq ethereum.FilterQuery) ([]types.Log, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.FilterLogs(ctx, fq)
}

func (c *ExecutionLayer) GetRevertMessage(txHash common.Hash) (string, error) {
	tx, _, err := c.client.TransactionByHash(context.Background(), txHash)
	if err != nil {
		return "", err
	}

	from, err := types.Sender(types.NewEIP155Signer(tx.ChainId()), tx)
	if err != nil {
		return "", err
	}

	msg := ethereum.CallMsg{
		From:     from,
		To:       tx.To(),
		Gas:      tx.Gas(),
		GasPrice: tx.GasPrice(),
		Value:    tx.Value(),
		Data:     tx.Data(),
	}

	_, err = c.client.CallContract(context.Background(), msg, nil)
	return err.Error(), nil

}
