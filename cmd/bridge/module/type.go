/*
 * Copyright 2022 ICON Foundation
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

package module

type RelayMessage struct {
	ReceiptProofs []*ReceiptProof
}

type ReceiptProof struct {
	Index  int64
	Events []*Event
	Height int64
}

type Event struct { //EventDataBTPMessage
	Next     string
	Sequence int64
	Message  []byte
}

type Segment struct {
	TransactionParam  TransactionParam //possible byte array
	GetResultParam    GetResultParam
	TransactionResult TransactionResult

	//
	From          BtpAddress
	Height        int64
	EventSequence int64 //last sequence
	NumberOfEvent int
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

type Wallet interface {
	Address() string
}
type TransactionParam interface{}
type GetResultParam interface{}
type TransactionResult interface{}

type MonitorCallback func(*BMCLinkStatus) error

type Sender interface {
	Relay(segment *Segment) (GetResultParam, error)
	GetResult(p GetResultParam) (TransactionResult, error)
	MonitorLoop(cb MonitorCallback) error
	StopMonitorLoop()
	//FinalizeLatency() int
	TxSizeLimit() int
}

type ReceiveCallback func([]*ReceiptProof) error

type Receiver interface {
	ReceiveLoop(height, seq int64, cb ReceiveCallback, scb func()) error
	StopReceiveLoop()
}
