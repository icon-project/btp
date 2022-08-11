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

package iconbridge

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math"
	"net/url"
	"strconv"
	"time"

	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/bridge/module/iconbridge/client"
	"github.com/icon-project/btp/cmd/bridge/module/iconbridge/wallet"

	"github.com/icon-project/btp/cmd/bridge/module"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	DefaultGetRelayResultInterval = time.Second
	DefaultRelayReSendInterval    = time.Second
	DefaultStepLimit              = 0x9502f900 //maxStepLimit(invoke), refer https://www.icondev.io/docs/step-estimation
)

var (
	txSizeLimit = int(math.Ceil(txMaxDataSize / (1 + txOverheadScale)))
)

type sender struct {
	c   *client.Client
	src module.BtpAddress
	dst module.BtpAddress
	w   wallet.Wallet
	l   log.Logger
	opt struct {
		StepLimit int64
	}
}

func (s *sender) newTransactionParam(method string, params interface{}) *client.TransactionParam {
	return &client.TransactionParam{
		Version:     client.NewHexInt(client.JsonrpcApiVersion),
		FromAddress: client.Address(s.w.Address()),
		ToAddress:   client.Address(s.dst.Account()),
		NetworkID:   client.HexInt(s.dst.NetworkID()),
		StepLimit:   client.NewHexInt(s.opt.StepLimit),
		DataType:    "call",
		Data: &client.CallData{
			Method: method,
			Params: params,
		},
	}
}

func (s *sender) sendFragment(msg []byte, idx int) (module.GetResultParam, error) {
	fmp := &client.BMCFragmentMethodParams{
		Prev:     s.src.String(),
		Messages: base64.URLEncoding.EncodeToString(msg),
		Index:    client.NewHexInt(int64(idx)),
	}
	p := s.newTransactionParam(client.BMCFragmentMethod, fmp)
	return s.sendTransaction(p)
}

func (s *sender) Relay(segment *module.Segment) (module.GetResultParam, error) {
	msg := segment.TransactionParam.([]byte)
	idx := len(msg) / txSizeLimit
	if idx == 0 {
		rmp := &client.BMCRelayMethodParams{
			Prev:     s.src.String(),
			Messages: base64.URLEncoding.EncodeToString(msg),
		}
		return s.sendTransaction(s.newTransactionParam(client.BMCRelayMethod, rmp))
	} else {
		ret, err := s.sendFragment(msg[:txSizeLimit], idx*-1)
		if err != nil {
			return nil, err
		}
		msg = msg[txSizeLimit:]
		for idx--; idx > 0; idx-- {
			if ret, err = s.sendFragment(msg[:txSizeLimit], idx); err != nil {
				return ret, err
			}
			msg = msg[txSizeLimit:]
		}
		if ret, err = s.sendFragment(msg[:], idx); err != nil {
			return ret, err
		}
		return ret, err
	}
}

func (s *sender) sendTransaction(p *client.TransactionParam) (module.GetResultParam, error) {
	thp := &client.TransactionHashParam{}
SignLoop:
	for {
		if err := s.c.SignTransaction(s.w, p); err != nil {
			return nil, err
		}
	SendLoop:
		for {
			txh, err := s.c.SendTransaction(p)
			if txh != nil {
				thp.Hash = *txh
			}
			if err != nil {
				if je, ok := err.(*jsonrpc.Error); ok {
					switch je.Code {
					case client.JsonrpcErrorCodeTxPoolOverflow:
						<-time.After(DefaultRelayReSendInterval)
						continue SendLoop
					case client.JsonrpcErrorCodeSystem:
						if subEc, err := strconv.ParseInt(je.Message[1:5], 0, 32); err == nil {
							switch subEc {
							case client.DuplicateTransactionError:
								s.l.Debugf("DuplicateTransactionError txh:%v", txh)
								return thp, nil
							case client.ExpiredTransactionError:
								continue SignLoop
							}
						}
					}
				}
				return nil, mapError(err)
			}
			return thp, nil
		}
	}
}

func (s *sender) GetResult(p module.GetResultParam) (module.TransactionResult, error) {
	if txh, ok := p.(*client.TransactionHashParam); ok {
		for {
			txr, err := s.c.GetTransactionResult(txh)
			if err != nil {
				if je, ok := err.(*jsonrpc.Error); ok {
					switch je.Code {
					case client.JsonrpcErrorCodePending, client.JsonrpcErrorCodeExecuting:
						<-time.After(DefaultGetRelayResultInterval)
						continue
					}
				}
			}
			return txr, mapErrorWithTransactionResult(txr, err)
		}
	} else {
		return nil, fmt.Errorf("fail to cast *TransactionHashParam %T", p)
	}
}

func (s *sender) GetStatus() (*module.BMCLinkStatus, error) {
	p := &client.CallParam{
		FromAddress: client.Address(s.w.Address()),
		ToAddress:   client.Address(s.dst.Account()),
		DataType:    "call",
		Data: client.CallData{
			Method: client.BMCGetStatusMethod,
			Params: client.BMCStatusParams{
				Target: s.src.String(),
			},
		},
	}
	bs := &client.BMCStatus{}
	err := mapError(s.c.Call(p, bs))
	if err != nil {
		return nil, err
	}
	ls := &module.BMCLinkStatus{}
	if ls.TxSeq, err = bs.TxSeq.Value(); err != nil {
		return nil, err
	}
	if ls.RxSeq, err = bs.RxSeq.Value(); err != nil {
		return nil, err
	}
	if ls.Verifier.Height, err = bs.Verifier.Height.Value(); err != nil {
		return nil, err
	}
	if ls.Verifier.Extra, err = bs.Verifier.Extra.Value(); err != nil {
		return nil, err
	}
	if ls.CurrentHeight, err = bs.CurrentHeight.Value(); err != nil {
		return nil, err
	}
	return ls, nil
}

func (s *sender) MonitorLoop(cb module.MonitorCallback) error {
	blk, err := s.c.GetLastBlock()
	if err != nil {
		return err
	}
	br := &client.BlockRequest{
		Height: client.NewHexInt(blk.Height),
	}
	return s.c.MonitorBlock(br,
		func(conn *websocket.Conn, v *client.BlockNotification) error {
			if h, err := v.Height.Value(); err != nil {
				return err
			} else {
				if bs, err := s.GetStatus(); err != nil {
					return err
				} else {
					if bs.CurrentHeight != h {
						s.l.Warnf("mismatch bmcstatus.currentHeight(%d) != blockNotification.height(%d)",
							bs.CurrentHeight, h)
					}
					return cb(bs)
				}
			}
		},
		func(conn *websocket.Conn) {
			s.l.Debugf("MonitorLoop connected %s", conn.LocalAddr().String())
		},
		func(conn *websocket.Conn, err error) {
			s.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
			_ = conn.Close()
		})
}

func (s *sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}

func (s *sender) TxSizeLimit() int {
	return txSizeLimit
}

func NewSender(src, dst module.BtpAddress, w module.Wallet, endpoint string, opt map[string]interface{}, l log.Logger) module.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w.(wallet.Wallet),
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	if s.opt.StepLimit <= 0 {
		s.opt.StepLimit = DefaultStepLimit
	}
	s.c = client.NewClient(endpoint, l)
	return s
}

func mapError(err error) error {
	if err != nil {
		switch re := err.(type) {
		case *jsonrpc.Error:
			//fmt.Printf("jrResp.Error:%+v", re)
			switch re.Code {
			case client.JsonrpcErrorCodeTxPoolOverflow:
				return module.ErrSendFailByOverflow
			case client.JsonrpcErrorCodeSystem:
				if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
					//TODO return JsonRPC Error
					switch subEc {
					case client.ExpiredTransactionError:
						return module.ErrSendFailByExpired
					case client.FutureTransactionError:
						return module.ErrSendFailByFuture
					case client.TransactionPoolOverflowError:
						return module.ErrSendFailByOverflow
					}
				}
			case client.JsonrpcErrorCodePending, client.JsonrpcErrorCodeExecuting:
				return module.ErrGetResultFailByPending
			}
		case *common.HttpError:
			fmt.Printf("*common.HttpError:%+v", re)
			return module.ErrConnectFail
		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				//fmt.Printf("*url.Error:%+v", re)
				return module.ErrConnectFail
			}
		}
	}
	return err
}

func mapErrorWithTransactionResult(txr *client.TransactionResult, err error) error {
	err = mapError(err)
	if err == nil && txr != nil && txr.Status != client.ResultStatusSuccess {
		fc, _ := txr.Failure.CodeValue.Value()
		if fc < client.ResultStatusFailureCodeRevert || fc > client.ResultStatusFailureCodeEnd {
			err = fmt.Errorf("failure with code:%s, message:%s",
				txr.Failure.CodeValue, txr.Failure.MessageValue)
		} else {
			err = module.NewRevertError(int(fc - client.ResultStatusFailureCodeRevert))
		}
	}
	return err
}
