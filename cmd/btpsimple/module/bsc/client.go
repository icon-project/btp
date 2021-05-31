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
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/ethereum/go-ethereum/rpc"
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
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}

type Client struct {
	l log.Logger
	*rpc.Client
	*rpc.ClientSubscription
	ethClient *ethclient.Client
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

func (c *Client) NewTransaction(p *TransactionParam) *types.Transaction {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()

	fromAddress := common.HexToAddress(p.FromAddress)
	toAddress := common.HexToAddress(p.ToAddress)

	txCount, _ := c.ethClient.PendingTransactionCount(ctx)
	nonce, _ := c.ethClient.NonceAt(ctx, fromAddress, nil)
	nonce = uint64(txCount) + nonce

	return newTransaction(nonce, toAddress, nil, 6000000, big.NewInt(6000000), nil)
}

func (c *Client) newTransactOpts(chainID int64) (*bind.TransactOpts, error) {
	privateKey, err := crypto.HexToECDSA("")
	txo, err := bind.NewKeyedTransactorWithChainID(privateKey, big.NewInt(chainID))
	if err != nil {
		return nil, err
	}
	return txo, nil
}

func (c *Client) SignTransaction(signerKey *ecdsa.PrivateKey, tx *types.Transaction) error {
	//signer := types.EIP155Signer{}
	signer := types.LatestSignerForChainID(big.NewInt(ChainID))
	tx, err := types.SignTx(tx, signer, signerKey)
	if err != nil {
		c.l.Errorf("could not sign tx: %v", err)
		return err
	}
	return nil
}

func (c *Client) SendTransaction(tx *types.Transaction) error {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()

	err := c.ethClient.SendTransaction(ctx, tx)

	if err != nil {
		c.l.Errorf("could not send tx: %v", err)
		return nil
	}

	return nil
}
func (c *Client) GetTransactionResult(p *TransactionHashParam) (*types.Receipt, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	tr, err := c.ethClient.TransactionReceipt(ctx, p.Hash)
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

func (c *Client) GetBlockHeaderByHeight(height *big.Int) (*types.Header, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	head, _ := c.ethClient.HeaderByNumber(ctx, height)
	return head, nil
}

func (c *Client) GetProofForResult(p *ProofResultParam) ([][]byte, error) {
	var result [][]byte
	// TODO: get proof for result
	return result, nil
}
func (c *Client) GetProofForEvents(p *ProofEventsParam) ([][][]byte, error) {
	var result [][][]byte
	// TODO: het proof for event
	return result, nil
}

func (c *Client) MonitorBlock(p *BlockRequest, cb func(b *BlockNotification) error) error {
	return c.Monitor(cb)
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
				c.l.Fatal(err)
			case header := <-subch:
				b := &BlockNotification{Hash: header.Hash(), Height: header.Number}
				err := cb(b)
				if err != nil {
					return
				}
				c.l.Debugf("MonitorBlock %v", header.Number.Int64())
			}
		}
	}()

	return nil
}

func (c *Client) CloseMonitor() {
	c.l.Debugf("CloseMonitor %s", c.Client)
	c.ClientSubscription.Unsubscribe()
	c.ethClient.Close()
	c.Client.Close()
}

func (c *Client) CloseAllMonitor() {
	// TODO: do we need to multiple connections?
	c.CloseMonitor()
}

func NewClient(uri string, l log.Logger) *Client {
	//TODO options {MaxRetrySendTx, MaxRetryGetResult, MaxIdleConnsPerHost, Debug, Dump}
	//tr := &http.Transport{MaxIdleConnsPerHost: 1000}
	client, _ := rpc.Dial(uri)

	c := &Client{
		Client:    client,
		ethClient: ethclient.NewClient(client),
		l:         l,
	}
	opts := BinanceOptions{}
	opts.SetBool(IconOptionsDebug, true)
	return c
}

const (
	HeaderKeyIconOptions = "Icon-Options"
	IconOptionsDebug     = "debug"
	IconOptionsTimeout   = "timeout"
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
