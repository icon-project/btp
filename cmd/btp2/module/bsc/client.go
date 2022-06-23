/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bsc

import (
	"context"
	"crypto/ecdsa"
	"fmt"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/common/hexutil"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/ethereum/go-ethereum/rpc"
	"github.com/icon-project/btp/cmd/btp2/module/bsc/systemcontracts"
	"github.com/icon-project/btp/common/wallet"
	"math/big"
	"strconv"
	"strings"
	"time"

	"github.com/icon-project/btp/common/log"
)

const (
	DefaultSendTransactionRetryInterval        = 3 * time.Second         //3sec
	DefaultGetTransactionResultPollingInterval = 1500 * time.Millisecond //1.5sec
	DefaultTimeout                             = 10 * time.Second        //
	ChainID                                    = 56
	DefaultGasLimit                            = 8000000
	DefaultGasPrice                            = 5000000000
)

var (
	tendermintLightClientContractAddr = common.HexToAddress("0x0000000000000000000000000000000000001003")
	BlockRetryInterval                = time.Second * 3
	BlockRetryLimit                   = 5
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}

type Client struct {
	log                   log.Logger
	subscription          *rpc.ClientSubscription
	ethClient             *ethclient.Client
	rpcClient             *rpc.Client
	chainID               *big.Int
	tendermintLightClient *systemcontracts.Tendermintlightclient
	stop                  <-chan bool
}

func toBlockNumArg(number *big.Int) string {
	if number == nil {
		return "latest"
	}
	pending := big.NewInt(-1)
	if number.Cmp(pending) == 0 {
		return "pending"
	}
	return hexutil.EncodeBig(number)
}

func newTransaction(nonce uint64, to common.Address, amount *big.Int, gasLimit uint64, gasPrice *big.Int, data []byte) *types.Transaction {
	return types.NewTx(&types.LegacyTx{
		Nonce:    nonce,
		To:       &to,
		Value:    amount,
		Gas:      gasLimit,
		GasPrice: gasPrice,
		Data:     data,
	})
}

func (c *Client) newTransactOpts(w Wallet) (*bind.TransactOpts, error) {
	txo, err := bind.NewKeyedTransactorWithChainID(w.(*wallet.EvmWallet).Skey, c.chainID)
	if err != nil {
		return nil, err
	}
	txo.GasPrice, _ = c.ethClient.SuggestGasPrice(context.Background())
	txo.GasLimit = uint64(DefaultGasLimit)
	return txo, nil
}

func (c *Client) SignTransaction(signerKey *ecdsa.PrivateKey, tx *types.Transaction) error {
	signer := types.LatestSignerForChainID(c.chainID)
	tx, err := types.SignTx(tx, signer, signerKey)
	if err != nil {
		c.log.Errorf("could not sign tx: %v", err)
		return err
	}
	return nil
}

func (c *Client) SendTransaction(tx *types.Transaction) error {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()

	err := c.ethClient.SendTransaction(ctx, tx)

	if err != nil {
		c.log.Errorf("could not send tx: %v", err)
		return nil
	}

	return nil
}

func (c *Client) GetTransactionReceipt(hash common.Hash) (*types.Receipt, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	tr, err := c.ethClient.TransactionReceipt(ctx, hash)
	if err != nil {
		return nil, err
	}
	return tr, nil
}

func (c *Client) GetTransaction(hash common.Hash) (*types.Transaction, bool, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	tx, pending, err := c.ethClient.TransactionByHash(ctx, hash)
	if err != nil {
		return nil, pending, err
	}
	return tx, pending, err
}

func (c *Client) GetBlockByHeight(height *big.Int) (*types.Block, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	block, err := c.ethClient.BlockByNumber(ctx, height)
	if err != nil {
		return nil, err
	}
	return block, nil
}

func (c *Client) GetHeaderByHeight(height *big.Int) (*types.Block, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	block, err := c.ethClient.BlockByNumber(ctx, height)
	if err != nil {
		return nil, err
	}
	return block, nil
}

func (c *Client) GetProof(height *big.Int, addr common.Address) (StorageProof, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	var proof StorageProof
	if err := c.rpcClient.CallContext(ctx, &proof, "eth_getProof", addr, nil, toBlockNumArg(height)); err != nil {
		return proof, err
	}
	return proof, nil
}

func (c *Client) GetBlockReceipts(block *types.Block) ([]*types.Receipt, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	var receipts []*types.Receipt
	for _, tx := range block.Transactions() {
		receipt, err := c.ethClient.TransactionReceipt(ctx, tx.Hash())
		if err != nil {
			return nil, err
		}
		receipts = append(receipts, receipt)
	}
	return receipts, nil
}

func (c *Client) GetChainID() (*big.Int, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return c.ethClient.ChainID(ctx)
}

func (c *Client) GetLatestConsensusState() (ConsensusStates, error) {
	callOpts := &bind.CallOpts{
		Pending: true,
		Context: context.Background(),
	}
	lastHeight, _ := c.tendermintLightClient.LatestHeight(callOpts)
	return c.tendermintLightClient.LightClientConsensusStates(callOpts, lastHeight)
}

func (c *Client) GetConsensusState(height *big.Int) (ConsensusStates, error) {
	callOpts := &bind.CallOpts{
		Pending: true,
		Context: context.Background(),
	}
	return c.tendermintLightClient.LightClientConsensusStates(callOpts, height.Uint64())
}

func (c *Client) MonitorBlock(p *BlockRequest, cb func(b *BlockNotification) error) error {
	return c.Poll(p, cb)
}

func (c *Client) Poll(p *BlockRequest, cb func(b *BlockNotification) error) error {
	go func() {
		current := p.Height
		var retry = BlockRetryLimit
		for {
			select {
			case <-c.stop:
				return
			default:
				// Exhausted all error retries
				if retry == 0 {
					c.log.Error("Polling failed, retries exceeded")
					//l.sysErr <- ErrFatalPolling
					return
				}

				latestHeader, err := c.ethClient.HeaderByNumber(context.Background(), current) // c.GetHeaderByHeight(current)
				if err != nil {
					c.log.Error("Unable to get latest block ", current, err)
					retry--
					<-time.After(BlockRetryInterval)
					continue
				}

				if latestHeader.Number.Cmp(current) < 0 {
					c.log.Debug("Block not ready, will retry", "target:", current, "latest:", latestHeader.Number)
					<-time.After(BlockRetryInterval)
					continue
				}

				v := &BlockNotification{
					Height: current,
					Hash:   latestHeader.Hash(),
					Header: latestHeader,
				}

				if err := cb(v); err != nil {
					c.log.Errorf(err.Error())
				}

				current.Add(current, big.NewInt(1))
				retry = BlockRetryLimit
			}
		}
	}()
	return nil
}

func (c *Client) Monitor(cb func(b *BlockNotification) error) error {
	subch := make(chan *types.Header)
	sub, err := c.ethClient.SubscribeNewHead(context.Background(), subch)
	if err != nil {
		return err
	}

	go func() {
		for {
			select {
			case err := <-sub.Err():
				c.log.Fatal(err)
			case header := <-subch:
				b := &BlockNotification{Hash: header.Hash(), Height: header.Number, Header: header}
				err := cb(b)
				if err != nil {
					return
				}
				c.log.Debugf("MonitorBlock %v", header.Number.Int64())
			}
		}
	}()

	return nil
}

func (c *Client) CloseMonitor() {
	c.log.Debugf("CloseMonitor %s", c.rpcClient)
	c.subscription.Unsubscribe()
	c.ethClient.Close()
	c.rpcClient.Close()
}

func (c *Client) CloseAllMonitor() {
	// TODO: do we need to multiple connections?
	c.CloseMonitor()
}

func NewClient(uri string, log log.Logger) *Client {
	//TODO options {MaxRetrySendTx, MaxRetryGetResult, MaxIdleConnsPerHost, Debug, Dump} }
	rpcClient, err := rpc.Dial(uri)
	if err != nil {
		log.Fatal("Error creating client", err)
	}
	c := &Client{
		rpcClient: rpcClient,
		ethClient: ethclient.NewClient(rpcClient),
		log:       log,
	}
	c.chainID, _ = c.GetChainID()
	log.Tracef("Client Connected Chain ID: ", c.chainID)
	c.tendermintLightClient, err = systemcontracts.NewTendermintlightclient(tendermintLightClientContractAddr, c.ethClient)
	if err != nil {
		c.log.Error("Error creating tendermintLightclient system contract", err)
	}
	opts := BinanceOptions{}
	opts.SetBool(IconOptionsDebug, true)
	return c
}

const (
	IconOptionsDebug   = "debug"
	IconOptionsTimeout = "timeout"
)

type BinanceOptions map[string]string

func (opts BinanceOptions) Set(key, value string) {
	opts[key] = value
}
func (opts BinanceOptions) Get(key string) string {
	if opts == nil {
		return ""
	}
	v := opts[key]
	if len(v) == 0 {
		return ""
	}
	return v
}
func (opts BinanceOptions) Del(key string) {
	delete(opts, key)
}
func (opts BinanceOptions) SetBool(key string, value bool) {
	opts.Set(key, strconv.FormatBool(value))
}
func (opts BinanceOptions) GetBool(key string) (bool, error) {
	return strconv.ParseBool(opts.Get(key))
}
func (opts BinanceOptions) SetInt(key string, v int64) {
	opts.Set(key, strconv.FormatInt(v, 10))
}
func (opts BinanceOptions) GetInt(key string) (int64, error) {
	return strconv.ParseInt(opts.Get(key), 10, 64)
}
func (opts BinanceOptions) ToHeaderValue() string {
	if opts == nil {
		return ""
	}
	strs := make([]string, len(opts))
	i := 0
	for k, v := range opts {
		strs[i] = fmt.Sprintf("%s=%s", k, v)
		i++
	}
	return strings.Join(strs, ",")
}
