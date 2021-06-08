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

package iconee

import (
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	"io"
	"net/http"
	"reflect"
	"strconv"
	"strings"
	"sync"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

type wsConnectError struct {
	error
	httpResp *http.Response
}

type wsRequestError struct {
	error
	wsResp *WSResponse
}
type wsReadCallback func(*websocket.Conn, interface{})

type api struct {
	*jsonrpc.Client
	conns  map[string]*websocket.Conn
	mtx    sync.Mutex
	logger log.Logger
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

func (a *api) monitor(reqUrl string, reqPtr, respPtr interface{}, cb wsReadCallback) error {
	if cb == nil {
		return fmt.Errorf("callback function cannot be nil")
	}
	conn, err := a.wsConnect(reqUrl, nil)
	if err != nil {
		return base.ErrConnectFail
	}
	defer func() {
		a.logger.Debugf("Monitor finish %s", conn.LocalAddr().String())
		a.wsClose(conn)
	}()
	if err = a.wsRequest(conn, reqPtr); err != nil {
		return err
	}
	cb(conn, WSEventInit)
	return a.wsReadJSONLoop(conn, respPtr, cb)
}

func (a *api) wsConnect(reqUrl string, reqHeader http.Header) (*websocket.Conn, error) {
	wsEndpoint := strings.Replace(a.Endpoint, "http", "ws", 1)
	conn, httpResp, err := websocket.DefaultDialer.Dial(wsEndpoint+reqUrl, reqHeader)
	if err != nil {
		wsErr := wsConnectError{error: err}
		wsErr.httpResp = httpResp
		return nil, wsErr
	}
	a._addWsConn(conn)
	return conn, nil
}

func (a *api) _addWsConn(conn *websocket.Conn) {
	a.mtx.Lock()
	defer a.mtx.Unlock()

	la := conn.LocalAddr().String()
	a.conns[la] = conn
}

func (a *api) wsReadJSONLoop(conn *websocket.Conn, respPtr interface{}, cb wsReadCallback) error {
	elem := reflect.ValueOf(respPtr).Elem()
	for {
		v := reflect.New(elem.Type())
		ptr := v.Interface()
		if _, ok := a.conns[conn.LocalAddr().String()]; !ok {
			a.logger.Debugf("wsReadJSONLoop c.conns[%s] is nil", conn.LocalAddr().String())
			return nil
		}
		if err := a.wsRead(conn, ptr); err != nil {
			a.logger.Debugf("wsReadJSONLoop c.conns[%s] ReadJSON err:%+v", conn.LocalAddr().String(), err)
			if cErr, ok := err.(*websocket.CloseError); !ok || cErr.Code != websocket.CloseNormalClosure {
				cb(conn, err)
			}
			return err
		}
		cb(conn, ptr)
	}
}

func (a *api) wsRead(conn *websocket.Conn, respPtr interface{}) error {
	mt, r, err := conn.NextReader()
	if err != nil {
		return err
	}
	if mt == websocket.CloseMessage {
		return io.EOF
	}
	return json.NewDecoder(r).Decode(respPtr)
}

func (a *api) wsClose(conn *websocket.Conn) {
	a._removeWsConn(conn)
	if err := conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, "")); err != nil {
		a.logger.Debugf("fail to WriteMessage CloseNormalClosure err:%+v", err)
	}
	if err := conn.Close(); err != nil {
		a.logger.Debugf("fail to Close err:%+v", err)
	}
}

func (a *api) _removeWsConn(conn *websocket.Conn) {
	a.mtx.Lock()
	defer a.mtx.Unlock()

	la := conn.LocalAddr().String()
	_, ok := a.conns[la]
	if ok {
		delete(a.conns, la)
	}
}

func (a *api) wsRequest(conn *websocket.Conn, reqPtr interface{}) error {
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

func (a *api) closeAllMonitor() {
	for _, conn := range a.conns {
		a.logger.Debugf("CloseAllMonitor %s", conn.LocalAddr().String())
		a.wsClose(conn)
	}
}

func (a *api) getBlockByHeight(p *BlockHeightParam) (*Block, error) {
	result := &Block{}
	if _, err := a.Do("icx_getBlockByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (a *api) getBlockHeaderByHeight(p *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_getBlockHeaderByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (a *api) getVotesByHeight(p *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_getVotesByHeight", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (a *api) getDataByHash(p *DataHashParam) ([]byte, error) {
	var result []byte
	_, err := a.Do("icx_getDataByHash", p, &result)
	if err != nil {
		return nil, err
	}
	return result, nil
}
func (a *api) getProofForResult(p *ProofResultParam) ([][]byte, error) {
	var result [][]byte
	if _, err := a.Do("icx_getProofForResult", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
func (a *api) getProofForEvents(p *ProofEventsParam) ([][][]byte, error) {
	var result [][][]byte
	if _, err := a.Do("icx_getProofForEvents", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (a *api) waitTransactionResult(p *TransactionHashParam) (*TransactionResult, error) {
	tr := &TransactionResult{}
	if _, err := a.Do("icx_waitTransactionResult", p, tr); err != nil {
		return nil, err
	}
	return tr, nil
}

func (a *api) getTransactionResult(p *TransactionHashParam) (*TransactionResult, error) {
	tr := &TransactionResult{}
	if _, err := a.Do("icx_getTransactionResult", p, tr); err != nil {
		return nil, err
	}
	return tr, nil
}

func (a *api) call(p *CallParam, r interface{}) error {
	if _, err := a.Do("icx_call", p, r); err != nil {
		return err
	}
	return nil
}

func (a *api) sendTransaction(p *TransactionParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_sendTransaction", p, &result); err != nil {
		return nil, err
	}
	return result, nil
}
