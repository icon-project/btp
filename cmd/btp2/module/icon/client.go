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
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"reflect"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	DefaultSendTransactionRetryInterval        = 3 * time.Second         //3sec
	DefaultGetTransactionResultPollingInterval = 1500 * time.Millisecond //1.5sec
)

type Wallet interface {
	Sign(data []byte) ([]byte, error)
	Address() string
}

type Client struct {
	*jsonrpc.Client
	conns map[string]*websocket.Conn
	l     log.Logger
	mtx   sync.Mutex
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
			case module.ErrSendFailByOverflow:
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

func (c *Client) GetBTPHeader(p *BTPBlockParam) (string, error) {
	var header string
	if _, err := c.Do("btp_getHeader", p, &header); err != nil {
		return "", err
	}
	return header, nil
}

func (c *Client) GetBTPMessage(p *BTPBlockParam) ([]string, error) {
	var result []string
	if _, err := c.Do("btp_getMessages", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (c *Client) GetBTPProof(p *BTPBlockParam) (string, error) {
	var result string
	if _, err := c.Do("btp_getProof", p, &result); err != nil {
		return "", err
	}
	return result, nil
}

func (c *Client) GetBTPNetworkInfo(p *BTPNetworkInfoParam) (*NetworkInfo, error) {
	result := &NetworkInfo{}
	if _, err := c.Do("btp_getNetworkInfo", p, &result); err != nil {
		return nil, err
	}
	return result, nil
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

func (c *Client) MonitorBTP(p *BTPRequest, cb func(conn *websocket.Conn, v *BTPNotification) error, scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	resp := &BTPNotification{}
	return c.Monitor("/btp", p, resp, func(conn *websocket.Conn, v interface{}) {
		switch t := v.(type) {
		case *BTPNotification:
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

func (c *Client) MonitorEvent(p *EventRequest, cb func(conn *websocket.Conn, v *EventNotification) error, errCb func(*websocket.Conn, error)) error {
	resp := &EventNotification{}
	return c.Monitor("/event", p, resp, func(conn *websocket.Conn, v interface{}) {
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

func (c *Client) Monitor(reqUrl string, reqPtr, respPtr interface{}, cb wsReadCallback) error {
	if cb == nil {
		return fmt.Errorf("callback function cannot be nil")
	}
	conn, err := c.wsConnect(reqUrl, nil)
	if err != nil {
		return module.ErrConnectFail
	}
	defer func() {
		c.l.Debugf("Monitor finish %s", conn.LocalAddr().String())
		c.wsClose(conn)
	}()
	if err = c.wsRequest(conn, reqPtr); err != nil {
		return err
	}
	cb(conn, WSEventInit)
	return c.wsReadJSONLoop(conn, respPtr, cb)
}

func (c *Client) CloseMonitor(conn *websocket.Conn) {
	c.l.Debugf("CloseMonitor %s", conn.LocalAddr().String())
	c.wsClose(conn)
}

func (c *Client) CloseAllMonitor() {
	for _, conn := range c.conns {
		c.l.Debugf("CloseAllMonitor %s", conn.LocalAddr().String())
		c.wsClose(conn)
	}
}

type wsReadCallback func(*websocket.Conn, interface{})

func (c *Client) _addWsConn(conn *websocket.Conn) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	la := conn.LocalAddr().String()
	c.conns[la] = conn
}

func (c *Client) _removeWsConn(conn *websocket.Conn) {
	c.mtx.Lock()
	defer c.mtx.Unlock()

	la := conn.LocalAddr().String()
	_, ok := c.conns[la]
	if ok {
		delete(c.conns, la)
	}
}

type wsConnectError struct {
	error
	httpResp *http.Response
}

func (c *Client) wsConnect(reqUrl string, reqHeader http.Header) (*websocket.Conn, error) {
	wsEndpoint := strings.Replace(c.Endpoint, "http", "ws", 1)
	conn, httpResp, err := websocket.DefaultDialer.Dial(wsEndpoint+reqUrl, reqHeader)
	if err != nil {
		wsErr := wsConnectError{error: err}
		wsErr.httpResp = httpResp
		return nil, wsErr
	}
	c._addWsConn(conn)
	return conn, nil
}

type wsRequestError struct {
	error
	wsResp *WSResponse
}

func (c *Client) wsRequest(conn *websocket.Conn, reqPtr interface{}) error {
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

func (c *Client) wsClose(conn *websocket.Conn) {
	c._removeWsConn(conn)
	if err := conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, "")); err != nil {
		c.l.Debugf("fail to WriteMessage CloseNormalClosure err:%+v", err)
	}
	if err := conn.Close(); err != nil {
		c.l.Debugf("fail to Close err:%+v", err)
	}
}

func (c *Client) wsRead(conn *websocket.Conn, respPtr interface{}) error {
	mt, r, err := conn.NextReader()
	if err != nil {
		return err
	}
	if mt == websocket.CloseMessage {
		return io.EOF
	}
	return json.NewDecoder(r).Decode(respPtr)
}

func (c *Client) wsReadJSONLoop(conn *websocket.Conn, respPtr interface{}, cb wsReadCallback) error {
	elem := reflect.ValueOf(respPtr).Elem()
	for {
		v := reflect.New(elem.Type())
		ptr := v.Interface()
		if _, ok := c.conns[conn.LocalAddr().String()]; !ok {
			c.l.Debugf("wsReadJSONLoop c.conns[%s] is nil", conn.LocalAddr().String())
			return nil
		}
		if err := c.wsRead(conn, ptr); err != nil {
			c.l.Debugf("wsReadJSONLoop c.conns[%s] ReadJSON err:%+v", conn.LocalAddr().String(), err)
			if cErr, ok := err.(*websocket.CloseError); !ok || cErr.Code != websocket.CloseNormalClosure {
				cb(conn, err)
			}
			return err
		}
		cb(conn, ptr)
	}
}

func NewClient(uri string, l log.Logger) *Client {
	//TODO options {MaxRetrySendTx, MaxRetryGetResult, MaxIdleConnsPerHost, Debug, Dump}
	tr := &http.Transport{MaxIdleConnsPerHost: 1000}
	c := &Client{
		Client: jsonrpc.NewJsonRpcClient(&http.Client{Transport: tr}, uri),
		conns:  make(map[string]*websocket.Conn),
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
