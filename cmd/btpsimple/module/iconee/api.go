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
	"io"
	"net/http"
	"reflect"
	"strconv"
	"strings"
	"sync"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

/*---------constants---------------*/
const (
	HeaderKeyIconOptions = "Icon-Options"
	IconOptionsDebug     = "debug"
	IconOptionsTimeout   = "timeout"
)

/*-----------struct----------------*/

type wsConnectError struct {
	error
	httpResp *http.Response
}

type wsRequestError struct {
	error
	wsResp *WSResponse
}

type api struct {
	*jsonrpc.Client
	conns  map[string]*websocket.Conn
	mtx    sync.Mutex
	logger log.Logger
}

type wsReadCallback func(*websocket.Conn, interface{})
type IconOptions map[string]string

/*---------Public functions----------------*/

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

func NewIconOptionsByHeader(header http.Header) IconOptions {
	s := header.Get(HeaderKeyIconOptions)

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

/*----------Private functions----------------*/
func (a *api) monitor(reqUrl string, requestPointer, responsePointer interface{}, callback wsReadCallback) error {
	if callback == nil {
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

	if err = a.wsRequest(conn, requestPointer); err != nil {
		return err
	}

	callback(conn, WSEventInit)
	return a.wsReadJSONLoop(conn, responsePointer, callback)
}

func (a *api) wsConnect(reqUrl string, reqHeader http.Header) (*websocket.Conn, error) {
	wsEndpoint := strings.Replace(a.Endpoint, "http", "ws", 1)
	conn, httpResponse, err := websocket.DefaultDialer.Dial(wsEndpoint+reqUrl, reqHeader)
	if err != nil {
		wsErr := wsConnectError{error: err}
		wsErr.httpResp = httpResponse
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

func (a *api) wsReadJSONLoop(conn *websocket.Conn, responsePointer interface{}, callback wsReadCallback) error {
	elem := reflect.ValueOf(responsePointer).Elem()

	for {
		v := reflect.New(elem.Type())
		poniter := v.Interface()
		if _, ok := a.conns[conn.LocalAddr().String()]; !ok {
			a.logger.Debugf("wsReadJSONLoop c.conns[%s] is nil", conn.LocalAddr().String())
			return nil
		}

		if err := a.wsRead(conn, poniter); err != nil {
			a.logger.Debugf("wsReadJSONLoop c.conns[%s] ReadJSON err:%+v", conn.LocalAddr().String(), err)
			if cErr, ok := err.(*websocket.CloseError); !ok || cErr.Code != websocket.CloseNormalClosure {
				callback(conn, err)
			}
			return err
		}

		callback(conn, poniter)
	}
}

func (a *api) wsRead(conn *websocket.Conn, responsePoniter interface{}) error {
	messageType, reader, err := conn.NextReader()
	if err != nil {
		return err
	}

	if messageType == websocket.CloseMessage {
		return io.EOF
	}

	return json.NewDecoder(reader).Decode(responsePoniter)
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

func (a *api) wsRequest(conn *websocket.Conn, requestPointer interface{}) error {
	if requestPointer == nil {
		log.Panicf("reqPtr cannot be nil")
	}

	var err error
	wsResponse := &WSResponse{}
	if err = conn.WriteJSON(requestPointer); err != nil {
		return wsRequestError{fmt.Errorf("fail to WriteJSON err:%+v", err), nil}
	}

	if err = conn.ReadJSON(wsResponse); err != nil {
		return wsRequestError{fmt.Errorf("fail to ReadJSON err:%+v", err), nil}
	}

	if wsResponse.Code != 0 {
		return wsRequestError{
			fmt.Errorf("invalid WSResponse code:%d, message:%s", wsResponse.Code, wsResponse.Message),
			wsResponse}
	}

	return nil
}

func (a *api) closeAllMonitor() {
	for _, conn := range a.conns {
		a.logger.Debugf("CloseAllMonitor %s", conn.LocalAddr().String())
		a.wsClose(conn)
	}
}

func (a *api) getBlockByHeight(blockHeightParam *BlockHeightParam) (*Block, error) {
	result := &Block{}
	if _, err := a.Do("icx_getBlockByHeight", blockHeightParam, &result); err != nil {
		return nil, err
	}

	return result, nil
}

func (a *api) getBlockHeaderByHeight(blockHeightParam *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_getBlockHeaderByHeight", blockHeightParam, &result); err != nil {
		return nil, err
	}

	return result, nil
}

func (a *api) getVotesByHeight(blockHeightParam *BlockHeightParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_getVotesByHeight", blockHeightParam, &result); err != nil {
		return nil, err
	}

	return result, nil
}

func (a *api) getDataByHash(dataHashParam *DataHashParam) ([]byte, error) {
	var result []byte
	_, err := a.Do("icx_getDataByHash", dataHashParam, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (a *api) getProofForResult(proofResultParam *ProofResultParam) ([][]byte, error) {
	var result [][]byte
	if _, err := a.Do("icx_getProofForResult", proofResultParam, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (a *api) getProofForEvents(proofEventsParam *ProofEventsParam) ([][][]byte, error) {
	var result [][][]byte
	if _, err := a.Do("icx_getProofForEvents", proofEventsParam, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (a *api) waitTransactionResult(transactionHashParam *TransactionHashParam) (*TransactionResult, error) {
	transactionResult := &TransactionResult{}
	if _, err := a.Do("icx_waitTransactionResult", transactionHashParam, transactionResult); err != nil {
		return nil, err
	}

	return transactionResult, nil
}

func (a *api) getTransactionResult(transactionHashParam *TransactionHashParam) (*TransactionResult, error) {
	transactionResult := &TransactionResult{}
	if _, err := a.Do("icx_getTransactionResult", transactionHashParam, transactionResult); err != nil {
		return nil, err
	}

	return transactionResult, nil
}

func (a *api) call(callParam *CallParam, r interface{}) error {
	if _, err := a.Do("icx_call", callParam, r); err != nil {
		return err
	}

	return nil
}

func (a *api) sendTransaction(transactionParam *TransactionParam) ([]byte, error) {
	var result []byte
	if _, err := a.Do("icx_sendTransaction", transactionParam, &result); err != nil {
		return nil, err
	}

	return result, nil
}
