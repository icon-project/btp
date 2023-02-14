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

package ethbr

import (
	"encoding/json"
	"math"
	"strconv"
	"strings"
	"time"

	"github.com/ethereum/go-ethereum/core/types"

	btpTypes "github.com/icon-project/btp/common/types"

	"github.com/icon-project/btp/chain/ethbr/binding"
	"github.com/icon-project/btp/chain/ethbr/client"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

const (
	txMaxDataSize                 = 524288 //512 * 1024 // 512kB
	txOverheadScale               = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	DefaultGetRelayResultInterval = time.Second
)

var (
	txSizeLimit = int(math.Ceil(txMaxDataSize / (1 + txOverheadScale)))
)

type sender struct {
	c   *client.Client
	src btpTypes.BtpAddress
	dst btpTypes.BtpAddress
	w   client.Wallet
	l   log.Logger
	opt struct {
	}
	bmc                *binding.BMC
	sc                 chan btpTypes.SenderMessage
	isFoundOffsetBySeq bool
}

func NewSender(src, dst btpTypes.BtpAddress, w client.Wallet, endpoint string, opt map[string]interface{}, l log.Logger) btpTypes.Sender {
	s := &sender{
		src: src,
		dst: dst,
		w:   w,
		l:   l,
		sc:  make(chan btpTypes.SenderMessage),
	}

	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &s.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}

	s.c = client.NewClient(endpoint, l)

	s.bmc, _ = binding.NewBMC(client.HexToAddress(s.dst.ContractAddress()), s.c.GetEthClient())

	return s
}

func (s *sender) Start() (<-chan btpTypes.SenderMessage, error) {
	return s.sc, nil
}

func (s *sender) Stop() {
	close(s.sc)
}
func (s *sender) GetStatus() (*btpTypes.BMCLinkStatus, error) {
	var status binding.TypesLinkStats
	status, err := s.bmc.GetStatus(nil, s.src.String())
	if err != nil {
		s.l.Errorf("Error retrieving relay status from BMC")
		return nil, err
	}

	ls := &btpTypes.BMCLinkStatus{}
	ls.TxSeq = status.TxSeq.Int64()
	ls.RxSeq = status.RxSeq.Int64()
	ls.Verifier.Height = status.Verifier.Height.Int64()
	ls.Verifier.Extra = status.Verifier.Extra

	return ls, nil
}

func (s *sender) SendStatus() {
	bs, _ := s.GetStatus()
	s.sc <- bs
}

func (s *sender) GetMarginForLimit() int64 {
	return 0
}

func (s *sender) Relay(rm btpTypes.RelayMessage) (int, error) {
	thp, err := s._relay(rm)
	if err != nil {
		return 0, err
	}
	go s.result(rm.Id(), thp)
	return rm.Id(), nil
}

func (s *sender) result(id int, txh *client.TransactionHashParam) {
	_, err := s.GetResult(txh)
	if err != nil {
		s.l.Debugf("result fail rm id : %d ", id)

		if ec, ok := errors.CoderOf(err); ok {
			s.sc <- &btpTypes.RelayResult{
				Id:  id,
				Err: ec.ErrorCode(),
			}
		}
	} else {
		s.l.Debugf("result success rm id : %d ", id)
	}

	s.SendStatus()
}

func (s *sender) GetResult(txh *client.TransactionHashParam) (*types.Receipt, error) {
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
			msgs := strings.Split(revertMsg, ":")
			if len(msgs) > 2 {
				code, err := strconv.Atoi(strings.TrimLeft(msgs[1], " "))
				if err != nil {
					return nil, err
				}
				return tx, client.NewRevertError(code)
			} else {
				return nil, client.NewRevertError(25)
			}

		}
		return tx, nil
	}
}

func (s *sender) TxSizeLimit() int {
	return txSizeLimit
}

func (s *sender) _relay(rm btpTypes.RelayMessage) (*client.TransactionHashParam, error) {

	t, err := s.c.NewTransactOpts(s.w.(*wallet.EvmWallet).Skey)
	if err != nil {
		return nil, err
	}

	var tx *types.Transaction

	tx, err = s.bmc.HandleRelayMessage(t, s.src.String(), rm.Bytes()[:])
	if err != nil {
		s.l.Errorf("handleRelayMessage: ", err.Error())
		return nil, err
	}
	txh := tx.Hash()
	return &client.TransactionHashParam{txh}, nil
}
