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

package evmbridge

import (
	"encoding/json"
	"fmt"
	"math"
	"sync"
	"time"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/icon-project/btp/cmd/bridge/module/evmbridge/client"

	"github.com/icon-project/btp/cmd/bridge/module"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                   = txMaxDataSize / (1 + txOverheadScale)
	DefaultGetRelayResultInterval = time.Second
	DefaultRelayReSendInterval    = time.Second
)

type sender struct {
	c   *client.Client
	src module.BtpAddress
	dst module.BtpAddress
	w   *EvmWallet
	l   log.Logger
	opt struct {
	}

	bmc *client.BMC

	evtLogRawFilter struct {
		addr      []byte
		signature []byte
		next      []byte
		seq       []byte
	}
	evtReq             *client.BlockRequest
	isFoundOffsetBySeq bool
	cb                 module.ReceiveCallback

	mutex sync.Mutex
}

func (s *sender) Relay(segment *module.Segment) (module.GetResultParam, error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	p := segment.TransactionParam.([]byte)

	t, err := s.c.NewTransactOpts(s.w.PrivateKey)
	if err != nil {
		return nil, err
	}

	var tx *types.Transaction
	tx, err = s.bmc.HandleRelayMessage(t, s.src.String(), p[:])
	if err != nil {
		s.l.Errorf("handleRelayMessage: ", err.Error())
		return nil, err
	}
	txh := tx.Hash()
	//s.l.Debugf("HandleRelayMessage tx hash:%s, prev %s, msg: %s", thp.Hash, rmp.Prev, base64.URLEncoding.EncodeToString([]byte(rmp.Messages)))
	return txh, nil
}

func (s *sender) GetResult(p module.GetResultParam) (module.TransactionResult, error) {
	if txh, ok := p.(common.Hash); ok {
		for {
			_, pending, err := s.c.GetTransaction(txh)
			if err != nil {
				return nil, err
			}
			if pending {
				<-time.After(DefaultGetRelayResultInterval)
				continue
			}
			tx, err := s.c.GetTransactionReceipt(txh)
			if err != nil {
				return nil, err
			}
			return tx, nil //mapErrorWithTransactionResult(&types.Receipt{}, err) // TODO: map transaction.js result error
		}
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (s *sender) GetStatus() (*module.BMCLinkStatus, error) {
	var status client.TypesLinkStats
	status, err := s.bmc.GetStatus(nil, s.src.String())

	if err != nil {
		s.l.Errorf("Error retrieving relay status from BMC")
		return nil, err
	}

	ls := &module.BMCLinkStatus{}
	ls.TxSeq = status.TxSeq.Int64()
	ls.RxSeq = status.RxSeq.Int64()
	ls.Verifier.Height = status.Verifier.Height.Int64()
	ls.Verifier.Extra = status.Verifier.Extra
	ls.CurrentHeight = status.CurrentHeight.Int64()
	return ls, nil
}

func (s *sender) MonitorLoop(cb module.MonitorCallback) error {
	return s.c.Monitor(
		func(bh *types.Header) error {
			if bs, err := s.GetStatus(); err != nil {
				return err
			} else {
				if bs.CurrentHeight != bh.Number.Int64() {
					s.l.Warnf("mismatch bmcstatus.currentHeight(%d) != blockNotification.height(%d)",
						bs.CurrentHeight, bh.Number.Int64())
				}
				return cb(bs)
			}
		})
}

func (s *sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}

func (s *sender) TxSizeLimit() int {
	return int(math.Round(float64(txSizeLimit)))
}

func NewSender(src, dst module.BtpAddress, w module.Wallet, endpoint string, opt map[string]interface{}, l log.Logger) module.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w.(*EvmWallet),
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	s.c = client.NewClient(endpoint, l)

	s.bmc, _ = client.NewBMC(common.HexToAddress(s.dst.ContractAddress()), s.c.GetBackend())

	return s
}
