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

package types

import (
	"math/big"
)

var BigIntOne = big.NewInt(1)

type RelayMessage interface {
	Bytes() []byte
	Size() int64
}

type Segment struct {
	TransactionParam  TransactionParam //possible byte array
	GetResultParam    GetResultParam
	TransactionResult TransactionResult

	Height              int64
	NumberOfBlockUpdate int
	EventSequence       *big.Int
	NumberOfEvent       int
}

type BMCLinkStatus struct {
	TxSeq    int64
	RxSeq    int64
	Verifier struct {
		Height int64
		Extra  []byte
	}
	CurrentHeight int64
}

type TransactionParam interface{}
type GetResultParam interface{}
type TransactionResult interface{}

type Wallet interface {
	Address() string
	Sign(interface{}) ([]byte, error)
	PrivateKey() interface{}
}

type Link interface {
	Start(sender Sender) error
	Stop()
}

type SenderCallback interface {
	OnStatusUpdate(bs *BMCLinkStatus)
	OnRelayResult(id int, err error)
}

type Sender interface {
	Start(cb SenderCallback)
	Stop()
	Relay(rm RelayMessage) (int, error)
	TxSizeLimit() int
}
