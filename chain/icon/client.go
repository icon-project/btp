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

package icon

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"reflect"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	DefaultSendTransactionRetryInterval        = 3 * time.Second         //3sec
	DefaultGetTransactionResultPollingInterval = 1500 * time.Millisecond //1.5sec
	defaultKeepAliveInterval                   = 10 * time.Second
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}

type Client struct {
	*jsonrpc.Client
	conns map[string]*jsonrpc.RecConn
	l     log.Logger
	mtx   sync.Mutex
}

func countBytesOfCompactJSON(jsonData interface{}) int {
	data, err := json.Marshal(jsonData)
	if err != nil {
		return txMaxDataSize
	}

	if len(data) == 0 {
		return txMaxDataSize
	}
	b := bytes.NewBuffer(nil)
	if err := json.Compact(b, data); err != nil {
		return txMaxDataSize
	}
	return b.Len()
}

var txSerializeExcludes = map[string]bool{"signature": true}

func (c *Client) SignTransaction(w Wallet, p *TransactionParam) error {
	p.Timestamp = NewHexInt(time.Now().UnixNano() / int64(time.Microsecond))
	js, err := json.Marshal(p)
	if err != nil {
		return err
	}

	bs, err := SerializeJSON(js, nil, txSerializeExcludes)
	if err != nil {
		return err
	}
	bs = append([]byte("icx_sendTransaction."), bs...)
	txHash := crypto.SHA3Sum256(bs)
	p.TxHash = NewHexBytes(txHash)
	sig, err := w.Sign(txHash)
	if err != nil {
		return err
	}
	p.Signature = base64.StdEncoding.EncodeToString(sig)
	return nil
}
func (c *Client) SendTransaction(p *TransactionParam) (*HexBytes, error) {
	var result HexBytes
	if _, err := c.Do("icx_sendTransaction", p, &result); err != nil {
		return nil, err
	}
	return &result, nil
}
func (c *Client) SendTransactionAndWait(p *TransactionParam) (*HexBytes, error) {
	var result HexBytes
	if _, err := c.Do("icx_sendTransactionAndWait", p, &result); err != nil {
		return nil, err
	}
	return &result, nil
}
func (c *Client) GetTransactionResult(p *TransactionHashParam) (*TransactionResult, error) {
	tr := &TransactionResult{}
	if _, err := c.Do("icx_getTransactionResult", p, tr); err != nil {
		return nil, err
	}
	return tr, nil
}
func (c *Client) WaitTransactionResult(p *TransactionHashParam) (*TransactionResult, error) {
	tr := &TransactionResult{}
	if _, err := c.Do("icx_waitTransactionResult", p, tr); err != nil {
		return nil, err
	}
	return tr, nil
}
func (c *Client) Call(p *CallParam, r interface{}) error {
	_, err := c.Do("icx_call", p, r)
	return err
}
func (c *Client) SendTransactionAndGetResult(p *TransactionParam) (*HexBytes, *TransactionResult, error) {
	thp := &TransactionHashParam{}
txLoop:
	for {
		txh, err := c.SendTransaction(p)
		if err != nil {
			switch err {
			case chain.ErrSendFailByOverflow:
				//TODO Retry max
				time.Sleep(DefaultSendTransactionRetryInterval)
				c.l.Debugf("Retry SendTransaction")
				continue txLoop
			default:
				switch re := err.(type) {
				case *jsonrpc.Error:
					switch re.Code {
					case JsonrpcErrorCodeSystem:
						if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
							switch subEc {
							case 2000: //DuplicateTransactionError
								//Ignore
								c.l.Debugf("DuplicateTransactionError txh:%v", txh)
								thp.Hash = *txh
								break txLoop
							}
						}
					}
				}
			}
			c.l.Debugf("fail to SendTransaction hash:%v, err:%+v", txh, err)
			return &thp.Hash, nil, err
		}
		thp.Hash = *txh
		break txLoop
	}

txrLoop:
	for {
		time.Sleep(DefaultGetTransactionResultPollingInterval)
		txr, err := c.GetTransactionResult(thp)
		if err != nil {
			switch re := err.(type) {
			case *jsonrpc.Error:
				switch re.Code {
				case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
					//TODO Retry max
					c.l.Debugln("Retry GetTransactionResult", thp)
					continue txrLoop
				}
			}
		}
		c.l.Debugf("GetTransactionResult hash:%v, txr:%+v, err:%+v", thp.Hash, txr, err)
		return &thp.Hash, txr, err
	}
}

func (c *Client) GetBlockByHeight(p *BlockHeightParam) (*Block, error) {
	result := &Block{}
	if _, err := c.Do("icx_getBlockByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (c *Client) GetBlockHeaderByHeight(p *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := c.Do("icx_getBlockHeaderByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (c *Client) GetVotesByHeight(p *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := c.Do("icx_getVotesByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (c *Client) GetDataByHash(p *DataHashParam) ([]byte, error) {
	var result []byte
	_, err := c.Do("icx_getDataByHash", p, &result)
	if err != nil {
		return nil, err
	}
	return result, nil
}
func (c *Client) GetProofForResult(p *ProofResultParam) ([][]byte, error) {
	var result [][]byte
	if _, err := c.Do("icx_getProofForResult", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (c *Client) GetProofForEvents(p *ProofEventsParam) ([][][]byte, error) {
	var result [][][]byte
	if _, err := c.Do("icx_getProofForEvents", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (c *Client) MonitorBlock(p *BlockRequest, cb func(conn *jsonrpc.RecConn, v *BlockNotification) error, scb func(conn *jsonrpc.RecConn), errCb func(*jsonrpc.RecConn, error)) error {
	resp := &BlockNotification{}
	reconP := p

	return c.Monitor("/block", p, resp, reconP, func(conn *jsonrpc.RecConn, v interface{}) {
		switch t := v.(type) {
		case *BlockNotification:
			h, _ := t.Height.Int()
			reconP.Height = NewHexInt(int64(h + 1))
			if err := cb(conn, t); err != nil {
				c.l.Debugf("MonitorBlock callback return err:%+v", err)
			}
		case WSEvent:
			c.l.Debugf("MonitorBlock WSEvent %s %+v", conn.Id, t)
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

func (c *Client) MonitorEvent(p *EventRequest, cb func(conn *jsonrpc.RecConn, v *EventNotification) error, errCb func(*jsonrpc.RecConn, error)) error {
	resp := &EventNotification{}
	return c.Monitor("/event", p, resp, nil, func(conn *jsonrpc.RecConn, v interface{}) {
		switch t := v.(type) {
		case *EventNotification:
			if err := cb(conn, t); err != nil {
				c.l.Debugf("MonitorEvent callback return err:%+v", err)
			}
		case error:
			errCb(conn, t)
		default:
			errCb(conn, fmt.Errorf("not supported type %T", t))
		}
	})
}

func (c *Client) Monitor(reqUrl string, reqPtr, respPtr, reconReqPtr interface{}, cb wsReadCallback) error {
	if cb == nil {
		return fmt.Errorf("callback function cannot be nil")
	}
	conn, err := c.wsConnect(reqUrl, nil)
	if err != nil {
		return chain.ErrConnectFail
	}
	defer func() {
		c.l.Debugf("Monitor finish %s", conn.Id)
		c.wsClose(conn)
	}()
	if err = c.wsRequest(conn, reqPtr); err != nil {
		return err
	}
	firstCbCalled := false
	cb(conn, WSEventInit)
	firstCbCalled = true
	elem := reflect.ValueOf(respPtr).Elem()
	for {
		v := reflect.New(elem.Type())
		ptr := v.Interface()
		if _, ok := c.conns[conn.Id]; !ok {
			c.l.Debugf("Monitor c.conns[%s] is nil", conn.Id)
			return nil
		}

		if !conn.IsConnected() {
			c.l.Debugf("Monitor c.conns[%s] not connected", conn.Id)
			firstCbCalled = false
			time.Sleep(conn.RecIntvlMin)
			continue
		}

		if firstCbCalled {
			if err := conn.ReadJSON(ptr); err != nil {
				c.l.Debugf("Monitor c.conns[%s] ReadJSON err:%+v", conn.Id, err)
				continue
			}
			cb(conn, ptr)
		} else {
			c.l.Debugf("Monitor c.conns[%s] reconnecting", conn.Id)
			if err = c.wsRequest(conn, reconReqPtr); err != nil {
				c.l.Debugf("Monitor c.conns[%s] request monitor err:%+v", conn.Id, err)
				continue
			}
			cb(conn, WSEventInit)
			firstCbCalled = true
		}
	}
}

func (c *Client) CloseMonitor(conn *jsonrpc.RecConn) {
	c.l.Debugf("CloseMonitor %s", conn.Id)
	c.wsClose(conn)
}

func (c *Client) CloseAllMonitor() {
	for _, conn := range c.conns {
		c.l.Debugf("CloseAllMonitor %s", conn.Id)
		c.wsClose(conn)
	}
}

type wsReadCallback func(*jsonrpc.RecConn, interface{})

func (c *Client) _addWsConn(conn *jsonrpc.RecConn) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	la := conn.Id
	c.conns[la] = conn
}

func (c *Client) _removeWsConn(conn *jsonrpc.RecConn) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	la := conn.Id
	_, ok := c.conns[la]
	if ok {
		delete(c.conns, la)
	}
}

func (c *Client) wsConnect(reqUrl string, reqHeader http.Header) (*jsonrpc.RecConn, error) {
	wsEndpoint := strings.Replace(c.Endpoint, "http", "ws", 1)
	conn := &jsonrpc.RecConn{
		KeepAliveTimeout: defaultKeepAliveInterval,
	}
	conn.Dial(wsEndpoint+reqUrl, reqHeader)
	c._addWsConn(conn)
	return conn, nil
}

type wsRequestError struct {
	error
	wsResp *WSResponse
}

func (c *Client) wsRequest(conn *jsonrpc.RecConn, reqPtr interface{}) error {
	if reqPtr == nil {
		log.Panicf("reqPtr cannot be nil")
	}
	var err error
	wsResp := &WSResponse{}
	if err = conn.WriteJSON(reqPtr); err != nil {
		return wsRequestError{fmt.Errorf("fail to WriteJSON err:%+v", err), nil}
	}

	if err = conn.ReadJSON(wsResp); err != nil {
		return wsRequestError{fmt.Errorf("fail to ReadJSON err:%+v", err), nil}
	}

	if wsResp.Code != 0 {
		return wsRequestError{
			fmt.Errorf("invalid WSResponse code:%d, message:%s", wsResp.Code, wsResp.Message),
			wsResp}
	}
	return nil
}

func (c *Client) wsClose(conn *jsonrpc.RecConn) {
	c._removeWsConn(conn)
	if err := conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, "")); err != nil {
		c.l.Debugf("fail to WriteMessage CloseNormalClosure err:%+v", err)
	}

	conn.Close()
}

func NewClient(uri string, l log.Logger) *Client {

	//TODO options {MaxRetrySendTx, MaxRetryGetResult, MaxIdleConnsPerHost, Debug, Dump}
	tr := &http.Transport{MaxIdleConnsPerHost: 1000}
	c := &Client{
		Client: jsonrpc.NewJsonRpcClient(&http.Client{Transport: tr}, uri),
		conns:  make(map[string]*jsonrpc.RecConn),
		l:      l,
	}
	opts := IconOptions{}
	opts.SetBool(IconOptionsDebug, true)
	c.CustomHeader[HeaderKeyIconOptions] = opts.ToHeaderValue()
	//c.Pre = func(req *http.Request) error {
	//	b, err := req.GetBody()
	//	if err != nil {
	//		return err
	//	}
	//	defer b.Close()
	//	m := make(map[string]interface{})
	//	if err = json.NewDecoder(b).Decode(&m); err != nil {
	//		return err
	//	}
	//	var bs []byte
	//	if bs, err = json.MarshalIndent(m, "", "  "); err != nil {
	//		return err
	//	}
	//	_, err = fmt.Fprintln(l.Writer(), string(bs))
	//	return err
	//}
	return c
}

const (
	HeaderKeyIconOptions = "Icon-Options"
	IconOptionsDebug     = "debug"
	IconOptionsTimeout   = "timeout"
)

type IconOptions map[string]string

func (opts IconOptions) Set(key, value string) {
	opts[key] = value
}
func (opts IconOptions) Get(key string) string {
	if opts == nil {
		return ""
	}
	v := opts[key]
	if len(v) == 0 {
		return ""
	}
	return v
}
func (opts IconOptions) Del(key string) {
	delete(opts, key)
}
func (opts IconOptions) SetBool(key string, value bool) {
	opts.Set(key, strconv.FormatBool(value))
}
func (opts IconOptions) GetBool(key string) (bool, error) {
	return strconv.ParseBool(opts.Get(key))
}
func (opts IconOptions) SetInt(key string, v int64) {
	opts.Set(key, strconv.FormatInt(v, 10))
}
func (opts IconOptions) GetInt(key string) (int64, error) {
	return strconv.ParseInt(opts.Get(key), 10, 64)
}
func (opts IconOptions) ToHeaderValue() string {
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

func NewIconOptionsByHeader(h http.Header) IconOptions {
	s := h.Get(HeaderKeyIconOptions)
	if s != "" {
		kvs := strings.Split(s, ",")
		m := make(map[string]string)
		for _, kv := range kvs {
			if kv != "" {
				idx := strings.Index(kv, "=")
				if idx > 0 {
					m[kv[:idx]] = kv[(idx + 1):]
				} else {
					m[kv] = ""
				}
			}
		}
		return m
	}
	return nil
}
