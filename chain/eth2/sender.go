/*
 * Copyright 2023 ICON Foundation
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

package eth2

import (
	"encoding/json"
	"math"
	"math/big"
	"strconv"
	"strings"
	"sync"
	"time"

	api "github.com/attestantio/go-eth2-client/api/v1"
	"github.com/attestantio/go-eth2-client/spec/altair"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	etypes "github.com/ethereum/go-ethereum/core/types"

	"github.com/icon-project/btp/chain/eth2/client"
	iclient "github.com/icon-project/btp/chain/icon/client"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/types"
	"github.com/icon-project/btp/common/wallet"
)

const (
	txMaxDataSize       = 524288 //512 * 1024 // 512kB
	txOverheadScale     = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	GetTXResultInterval = SecondPerSlot * time.Second
)

var (
	txSizeLimit = int(math.Ceil(txMaxDataSize / (1 + txOverheadScale)))
)

type request struct {
	id     int
	txHash common.Hash
}

func (r request) ID() int {
	return r.id
}

func (r request) TxHash() common.Hash {
	return r.txHash
}

type sender struct {
	src  types.BtpAddress
	dst  types.BtpAddress
	w    wallet.Wallet
	l    log.Logger
	opt  struct{}
	sc   chan types.SenderMessage // send finalized data only
	reqs []*request
	mtx  sync.RWMutex

	cl  *client.ConsensusLayer
	el  *client.ExecutionLayer
	bmc *client.BMC
}

func NewSender(src, dst types.BtpAddress, w wallet.Wallet, endpoint string, opt map[string]interface{}, l log.Logger) types.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w,
		l:   l,
		sc:  make(chan types.SenderMessage),
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	s.cl, err = client.NewConsensusLayer(endpoint, l)
	if err != nil {
		l.Panicf("fail to connect to %s, %v", endpoint, err)
	}
	s.el, err = client.NewExecutionLayer(opt["execution"].(string), l)
	if err != nil {
		l.Panicf("fail to connect to %s, %v", endpoint, err)
	}
	s.bmc, err = client.NewBMC(common.HexToAddress(s.dst.ContractAddress()), s.el.GetBackend())
	if err != nil {
		l.Panicf("fail to get instance of BMC %s, %v", s.dst.ContractAddress(), err)
	}
	return s
}

func (s *sender) Start() (<-chan types.SenderMessage, error) {
	go func() {
		blockNumber, _ := s.cl.SlotToBlockNumber(0)
		s.sendStatus(blockNumber)
	}()

	go s.handleFinalityUpdate()

	return s.sc, nil
}

func (s *sender) Stop() {
	close(s.sc)
}

func (s *sender) GetStatus() (*types.BMCLinkStatus, error) {
	return s.getStatus(0)
}

func (s *sender) Relay(rm types.RelayMessage) (int, error) {
	t, err := s.el.NewTransactOpts(s.w.(*wallet.EvmWallet).Skey)
	if err != nil {
		return 0, err
	}

	tx, err := s.bmc.HandleRelayMessage(t, s.src.String(), rm.Bytes())
	if err != nil {
		return 0, err
	}
	s.addRequest(&request{id: rm.Id(), txHash: tx.Hash()})
	return rm.Id(), nil
}

func (s *sender) GetMarginForLimit() int64 {
	return 0
}

func (s *sender) TxSizeLimit() int {
	return txSizeLimit
}

func (s *sender) addRequest(req *request) {
	s.mtx.Lock()
	defer s.mtx.Unlock()
	s.reqs = append(s.reqs, req)
}

func (s *sender) clearRequest(index int) {
	s.mtx.Lock()
	defer s.mtx.Unlock()
	if index == -1 {
		s.l.Debugf("clear all requests")
		s.reqs = make([]*request, 0)
	} else if index == 0 {
		s.l.Debugf("clear no requests")
	} else {
		s.l.Debugf("clear requests to %d", index)
		s.reqs = s.reqs[index-1:]
	}
}

func (s *sender) sendStatus(blockNumber uint64) error {
	s.l.Debugf("sendStatus of %d", blockNumber)
	status, err := s.getStatus(blockNumber)
	if err != nil {
		return err
	}
	s.sc <- status
	return nil
}

func (s *sender) getStatus(bn uint64) (*types.BMCLinkStatus, error) {
	var status client.TypesLinkStatus
	var callOpts *bind.CallOpts
	if bn != 0 {
		callOpts = &bind.CallOpts{
			BlockNumber: big.NewInt(int64(bn)),
		}
	}
	status, err := s.bmc.GetStatus(callOpts, s.src.String())

	if err != nil {
		s.l.Errorf("Error retrieving relay status from BMC")
		return nil, err
	}

	ls := &types.BMCLinkStatus{}
	ls.TxSeq = status.TxSeq.Int64()
	ls.RxSeq = status.RxSeq.Int64()
	ls.Verifier.Height = status.Verifier.Height.Int64()
	ls.Verifier.Extra = status.Verifier.Extra
	return ls, nil
}

func (s *sender) handleFinalityUpdate() {
	for {
		if err := s.cl.Events([]string{client.TopicLCFinalityUpdate}, func(event *api.Event) {
			update := event.Data.(*altair.LightClientFinalityUpdate)
			s.l.Debugf("handle finality_update event slot:%d", update.FinalizedHeader.Beacon.Slot)
			blockNumber, err := s.cl.SlotToBlockNumber(update.FinalizedHeader.Beacon.Slot)
			if err != nil {
				s.l.Panicf("can't convert slot to block number. %d", update.FinalizedHeader.Beacon.Slot)
			}
			s.checkRelayResult(blockNumber)
		}); err != nil {
			s.l.Panicf("onError %v", err)
		}
	}
}

func (s *sender) checkRelayResult(to uint64) {
	var index int
	var req *request
	s.mtx.RLock()
	for index, req = range s.reqs {
		_, pending, err := s.el.TransactionByHash(req.TxHash())
		if err != nil {
			s.l.Panicf("can't get TX %#x. %v", req.TxHash(), err)
		}
		if pending {
			s.l.Debugf("TX %#x is not yet executed", req.TxHash())
			break
		}
		receipt, err := s.el.TransactionReceipt(req.TxHash())
		if err != nil {
			s.l.Panicf("can't get TX receipt for %#x. %v", req.TxHash(), err)
		}
		if to < receipt.BlockNumber.Uint64() {
			s.l.Debugf("%#x is not yet finalized", req.TxHash())
			break
		}
		err = s.receiptToRevertError(receipt)
		if err != nil {
			s.l.Debugf("result fail %v. %v", req, err)
			if ec, ok := errors.CoderOf(err); ok {
				s.sc <- &types.RelayResult{
					Id:  req.ID(),
					Err: ec.ErrorCode(),
				}
			} else {
				s.l.Panicf("can't convert receipt to revertLog. %v", err)
			}
			index = -1
			break
		} else {
			s.l.Debugf("result success %v", req)
		}
	}
	s.mtx.RUnlock()

	s.clearRequest(index)
	if index != -1 {
		s.sendStatus(to)
	}
}

func (s *sender) receiptToRevertError(receipt *etypes.Receipt) error {
	if receipt.Status == 0 {
		revertMsg, err := s.el.GetRevertMessage(receipt.TxHash)
		if err != nil {
			return err
		}
		msgs := strings.Split(revertMsg, ":")
		if len(msgs) > 2 {
			code, err := strconv.Atoi(strings.TrimLeft(msgs[1], " "))
			if err != nil {
				return err
			}
			return iclient.NewRevertError(code)
		} else {
			return iclient.NewRevertError(25)
		}
	}
	return nil
}
