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
	"strconv"
	"strings"
	"time"

	"github.com/ethereum/go-ethereum/common"

	"github.com/icon-project/btp/chain/eth2/client"
	iclient "github.com/icon-project/btp/chain/icon/client"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/types"
	"github.com/icon-project/btp/common/wallet"
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
	src types.BtpAddress
	dst types.BtpAddress
	w   wallet.Wallet
	l   log.Logger
	opt struct{}
	sc  chan types.SenderMessage

	isFoundOffsetBySeq bool

	c   *client.ExecutionLayer
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
	s.c, err = client.NewExecutionLayer(endpoint, l)
	if err != nil {
		l.Panicf("fail to connect to %s, %v", endpoint, err)
	}
	s.bmc, err = client.NewBMC(common.HexToAddress(s.dst.ContractAddress()), s.c.GetBackend())
	if err != nil {
		l.Panicf("fail to get instance of BMC %s, %v", s.dst.ContractAddress(), err)
	}
	return s
}

func (s *sender) Start() (<-chan types.SenderMessage, error) {
	go func() {
		s.SendStatus()
	}()

	return s.sc, nil
}

func (s *sender) Stop() {
	close(s.sc)
}

func (s *sender) GetStatus() (*types.BMCLinkStatus, error) {
	var status client.TypesLinkStatus
	status, err := s.bmc.GetStatus(nil, s.src.String())

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

func (s *sender) Relay(rm types.RelayMessage) (int, error) {
	t, err := s.c.NewTransactOpts(s.w.(*wallet.EvmWallet).Skey)
	if err != nil {
		return 0, err
	}

	// TODO bmc.handleFraqment
	tx, err := s.bmc.HandleRelayMessage(t, s.src.String(), rm.Bytes())
	if err != nil {
		s.l.Errorf("handleRelayMessage: ", err.Error())
		return 0, err
	}
	go s.result(rm.Id(), tx.Hash())
	return rm.Id(), nil
}

func (s *sender) GetMarginForLimit() int64 {
	return 0
}

func (s *sender) TxSizeLimit() int {
	return txSizeLimit
}

func (s *sender) result(id int, txHash common.Hash) {
	err := s.GetResult(txHash)
	if err != nil {
		s.l.Debugf("result fail rm id : %d ", id)

		if ec, ok := errors.CoderOf(err); ok {
			s.sc <- &types.RelayResult{
				Id:  id,
				Err: ec.ErrorCode(),
			}
		}
	} else {
		s.l.Debugf("result success rm id : %d ", id)
	}
	s.SendStatus()
}

func (s *sender) SendStatus() {
	bs, _ := s.GetStatus()
	s.sc <- bs
}

func (s *sender) GetResult(txHash common.Hash) error {
	for {
		_, pending, err := s.c.TransactionByHash(txHash)
		if err != nil {
			return err
		}
		if pending {
			<-time.After(DefaultGetRelayResultInterval)
			continue
		}
		receipt, err := s.c.TransactionReceipt(txHash)
		if err != nil {
			return err
		}

		if receipt.Status == 0 {
			revertMsg, err := s.c.GetRevertMessage(txHash)
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
}
