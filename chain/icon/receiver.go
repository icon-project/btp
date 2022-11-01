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
	"math/big"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/intconv"

	"github.com/icon-project/btp/common/log"
)

type Receiver struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	l   log.Logger
	opt struct {
	}
	req *BTPRequest
	bh  *BTPBlockHeader
}

var bigIntOne = big.NewInt(1)

func (r *Receiver) GetBTPBlockHeader(height int64, nid int64) ([]byte, error) {
	pr := &BTPBlockParam{Height: HexInt(intconv.FormatInt(height)), NetworkId: HexInt(intconv.FormatInt(nid))}
	hB64, err := r.c.GetBTPHeader(pr)
	if err != nil {
		return nil, err
	}
	h, err := base64.StdEncoding.DecodeString(hB64)
	if err != nil {
		return nil, err
	}
	return h, nil
}

func (r *Receiver) GetBTPMessage(height int64, nid int64) ([][]byte, error) {
	pr := &BTPBlockParam{Height: HexInt(intconv.FormatInt(height)), NetworkId: HexInt(intconv.FormatInt(nid))}
	mgs, err := r.c.GetBTPMessage(pr)
	if err != nil {
		return nil, err
	}
	result := make([][]byte, 0)
	for _, mg := range mgs {
		m, err := base64.StdEncoding.DecodeString(mg)
		if err != nil {
			return nil, err
		}
		result = append(result, m)
	}

	return result, nil
}

func (r *Receiver) GetBTPProof(height int64, nid int64) ([]byte, error) {
	pr := &BTPBlockParam{Height: HexInt(intconv.FormatInt(height)), NetworkId: HexInt(intconv.FormatInt(nid))}
	b64p, err := r.c.GetBTPProof(pr)
	if err != nil {
		return nil, err
	}
	proof, err := base64.StdEncoding.DecodeString(b64p)
	if err != nil {
		return nil, err
	}

	return proof, nil
}

func (r *Receiver) GetBTPNetworkInfo(nid int64) (*NetworkInfo, error) {
	p := &BTPNetworkInfoParam{Id: HexInt(intconv.FormatInt(nid))}
	b, err := r.c.GetBTPNetworkInfo(p)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func (r *Receiver) ReceiveLoop(height int64, networkId int64, cb func(bu *BTPBlockUpdate) error, scb func()) error {
	//s := r.dst.String()
	r.req = &BTPRequest{
		Height:    HexInt(intconv.FormatInt(height)),
		NetworkID: HexInt(intconv.FormatInt(networkId)),
		ProofFlag: false,
	}

	if height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	return r.c.MonitorBTP(r.req,
		func(conn *websocket.Conn, v *BTPNotification) error {
			var p []byte
			h, err := v.Header.Value()
			if err != nil {
				return err
			}
			if len(v.Proof) > 0 {
				p, err = base64.StdEncoding.DecodeString(v.Proof)
				if err != nil {
					return err
				}
			}

			if err != nil {
				return err
			}
			return cb(&BTPBlockUpdate{BTPBlockHeader: h, BTPBlockProof: p})
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

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) *Receiver {
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
