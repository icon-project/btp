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
	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/common/intconv"
	"math/big"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

const (
	EventSignature      = "Message(str,int,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type Receiver struct {
	c   *Client
	src module.BtpAddress
	dst module.BtpAddress
	l   log.Logger
	opt struct {
	}
	req *BTPRequest
	bh  *BTPBlockHeader
}

var bigIntOne = big.NewInt(1)

func (r *Receiver) GetBTPBlockHeader(v *BTPNotification) (*BTPBlockHeader, error) {
	var bh BTPBlockHeader
	hv, _ := v.Header.Value()
	_, err := codec.RLP.UnmarshalFromBytes(hv, &bh)
	if err != nil {
		return nil, err
	}
	return &bh, nil
}
func (r *Receiver) GetBTPMessage(bh *BTPBlockHeader, nid int64) ([][]byte, error) {
	p := &BTPBlockParam{Height: HexInt(intconv.FormatInt(bh.MainHeight)), NetworkId: HexInt(intconv.FormatInt(nid))}
	b, err := r.c.GetBTPMessage(p)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func (r *Receiver) GetBTPProof(bh *BTPBlockHeader, nid int64) ([]byte, error) {
	p := &BTPBlockParam{Height: HexInt(intconv.FormatInt(bh.MainHeight)), NetworkId: HexInt(intconv.FormatInt(nid))}
	b, err := r.c.GetBTPProof(p)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func (r *Receiver) GetBTPNetworkInfo(nid int64) (*NetworkInfo, error) {
	p := &BTPNetworkInfoParam{NetworkId: HexInt(intconv.FormatInt(nid))}
	b, err := r.c.GetBTPNetworkInfo(p)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func (r *Receiver) ReceiveLoop(height int64, networkId int64, proofFlag int64, cb func(bu *BTPBlockHeader) error, scb func()) error {
	//s := r.dst.String()
	r.req = &BTPRequest{
		Height:    HexInt(intconv.FormatInt(height)),
		NetworkID: HexInt(intconv.FormatInt(networkId)),
		ProofFlag: HexInt(intconv.FormatInt(proofFlag)),
	}

	if height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	return r.c.MonitorBTP(r.req,
		func(conn *websocket.Conn, v *BTPNotification) error {
			var bh BTPBlockHeader
			h, err := v.Header.Value()
			if err != nil {
				return err
			}
			_, err = codec.RLP.UnmarshalFromBytes(h, &bh)
			if len(v.Proof) > 0 {
				p, err := base64.URLEncoding.DecodeString(v.Proof)
				if err != nil {
					return err
				}
				bh.Proof = p
			}
			if err != nil {
				return err
			}
			cb(&bh)
			return nil
		},
		func(conn *websocket.Conn) {
			r.l.Debugf("ReceiveLoop connected %s", conn.LocalAddr().String())
			if scb != nil {
				scb()
			}
		},
		func(conn *websocket.Conn, err error) {
			r.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
			_ = conn.Close()
		})
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}

func NewReceiver(src, dst module.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) *Receiver {
	r := &Receiver{
		src: src,
		dst: dst,
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &r.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	r.c = NewClient(endpoint, l)
	return r
}
