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

package bsc

import (
	"encoding/json"
	"fmt"
	"math"
	"math/big"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/ethereum/go-ethereum/core/types"

	"github.com/icon-project/btp/chain/bsc/binding"
	"github.com/icon-project/btp/common/wallet"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                   = txMaxDataSize / (1 + txOverheadScale)
	DefaultGetRelayResultInterval = time.Second
)

type sender struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	w   Wallet
	l   log.Logger
	opt struct {
	}

	bmc *binding.BMC

	evtLogRawFilter struct {
		addr      []byte
		signature []byte
		next      []byte
		seq       []byte
	}
	evtReq             *BlockRequest
	isFoundOffsetBySeq bool
	cb                 chain.ReceiveCallback

	mutex sync.Mutex
}

func (s *sender) newTransactionParam(prev string, rm *RelayMessage) (*TransactionParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}
	rmp := BMCRelayMethodParams{
		Prev: prev,
		//Messages: base64.URLEncoding.EncodeToString(b),
		Messages: string(b[:]),
	}
	p := &TransactionParam{
		Params: rmp,
	}
	return p, nil
}

func (s *sender) Relay(segment *chain.Segment) (chain.GetResultParam, error) {
	p := segment.TransactionParam.([]byte)
	t, err := s.c.NewTransactOpts(s.w.(*wallet.EvmWallet).Skey)
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
	return txh, nil

}

func (s *sender) GetResult(p chain.GetResultParam) (chain.TransactionResult, error) {
	if txh, ok := p.(*TransactionHashParam); ok {
		for {
			_, pending, err := s.c.GetTransaction(txh.Hash)
			if err != nil {
				return nil, err
			}
			if pending {
				<-time.After(DefaultGetRelayResultInterval)
				continue
			}
			tx, err := s.c.GetTransactionReceipt(txh.Hash)
			if err != nil {
				return nil, err
			}

			if tx.Status == 0 {
				revertMsg, err := s.c.GetRevertMessage(txh.Hash)
				if err != nil {
					return nil, err
				}
				code, err := strconv.Atoi(strings.Split(revertMsg, "|")[1])
				if err != nil {
					return nil, err
				}
				return tx, NewRevertError(code)
			}
			return tx, nil
		}
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (s *sender) GetStatus() (*chain.BMCLinkStatus, error) {
	var status binding.TypesLinkStats
	status, err := s.bmc.GetStatus(nil, s.src.String())
	if err != nil {
		s.l.Errorf("Error retrieving relay status from BMC")
		return nil, err
	}

	ls := &chain.BMCLinkStatus{}
	ls.TxSeq = status.TxSeq
	ls.RxSeq = status.RxSeq
	ls.Verifier.Height = status.Verifier.Height.Int64()
	ls.Verifier.Extra = status.Verifier.Extra
	ls.CurrentHeight = status.CurrentHeight.Int64()
	return ls, nil
}

func (s *sender) MonitorLoop(height int64, cb chain.MonitorCallback, scb func()) error {
	s.l.Debugf("MonitorLoop (sender) connected")
	br := &BlockRequest{
		Height: big.NewInt(height),
	}
	return s.c.MonitorBlock(br,
		func(v *BlockNotification) error {
			return cb(v.Height.Int64())
		})
}

func (s *sender) StopMonitorLoop() {
	s.c.CloseAllMonitor()
}
func (s *sender) FinalizeLatency() int {
	//on-the-next
	return 1
}

func (s *sender) TxSizeLimit() int {
	return int(math.Round(float64(txSizeLimit)))
}

func NewSender(src, dst chain.BtpAddress, w Wallet, endpoint string, opt map[string]interface{}, l log.Logger) chain.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w,
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	s.c = NewClient(endpoint, l)

	s.bmc, _ = binding.NewBMC(HexToAddress(s.dst.ContractAddress()), s.c.ethClient)

	return s
}
