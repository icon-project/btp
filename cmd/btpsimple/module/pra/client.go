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

package pra

import (
	"context"
	"crypto/ecdsa"
	"fmt"
	"sync"
	"time"

	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/common/log"
)

const (
	DefaultSendTransactionRetryInterval        = 3 * time.Second         //3sec
	DefaultGetTransactionResultPollingInterval = 1500 * time.Millisecond //1.5sec
)

type Client struct {
	*ethclient.Client
	conns map[string]*websocket.Conn
	l     log.Logger
	mtx   sync.Mutex
}

func (c *Client) SignTransaction(privateKey *ecdsa.PrivateKey, tx *types.Transaction) (*types.Transaction, error) {
	chainID, err := c.NetworkID(context.Background())
	if err != nil {
		log.Fatal(err)
	}

	signedTx, err := types.SignTx(tx, types.NewEIP155Signer(chainID), privateKey)
	if err != nil {
		log.Fatal(err)
	}

	return signedTx, nil
}

func (c *Client) SendSignedTransaction(tx *types.Transaction) (*HexBytes, error) {
	var result = HexBytes(tx.Hash().Hex())

	err := c.SendTransaction(context.Background(), tx)

	if err != nil {
		return nil, err
	}

	return &result, nil
}

func (c *Client) MonitorBlock(p *BlockRequest, cb func(conn *websocket.Conn, v *BlockNotification) error, scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	resp := &BlockNotification{}
	return c.Monitor("/block", p, resp, func(conn *websocket.Conn, v interface{}) {
		switch t := v.(type) {
		case *BlockNotification:
			if err := cb(conn, t); err != nil {
				c.l.Debugf("MonitorBlock callback return err:%+v", err)
			}
		case WSEvent:
			c.l.Debugf("MonitorBlock WSEvent %s %+v", conn.LocalAddr().String(), t)
			switch t {
			case WSEventInit:
				if scb != nil {
					scb(conn)
				}
			}
		case error:
			errCb(conn, t)
		default:
			errCb(conn, fmt.Errorf("not supported type %T", t))
		}
	})
}

func NewClient(uri string, l log.Logger) *Client {
	eC, err := ethclient.Dial(uri)

	if err != nil {
		log.Fatal(err)
	}

	c := &Client{
		Client: eC,
		conns:  make(map[string]*websocket.Conn),
		l:      l,
	}

	return c
}
