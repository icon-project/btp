package icon

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"reflect"
	"strings"
	"sync"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

type api struct {
	*jsonrpc.Client
	conns  map[string]*websocket.Conn
	mtx    sync.Mutex
	logger log.Logger

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
