package client

import (
	"context"
	"math/big"
	"time"

	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/ethereum/go-ethereum/rpc"

	"github.com/icon-project/btp/common/log"
)

const (
	DefaultTimeout = 10 * time.Second //
)

type ExecutionLayer struct {
	client *ethclient.Client
	log    log.Logger
}

func NewExecutionLayer(url string, log log.Logger) (*ExecutionLayer, error) {
	rpcClient, err := rpc.Dial(url)
	if err != nil {
		return nil, err
	}
	return &ExecutionLayer{
		client: ethclient.NewClient(rpcClient),
		log:    log,
	}, nil
}

func (c *ExecutionLayer) BlockByNumber(num *big.Int) (*types.Block, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.BlockByNumber(ctx, num)
}

func (c *ExecutionLayer) TransactionReceipt(txHash common.Hash) (*types.Receipt, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.client.TransactionReceipt(ctx, txHash)
}

func (c *ExecutionLayer) GetBackend() bind.ContractBackend {
	return c.client
}
