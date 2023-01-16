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

// Package sort provides primitives for sorting slices and user-defined
// collections.
package link

import "github.com/icon-project/btp/common/types"

type MessageItemType int

const (
	TypeBlockUpdate MessageItemType = iota + 1
	TypeBlockProof
	TypeMessageProof
)

type ReceiveStatus interface {
	Height() int64
	Seq() int64
}

type RelayMessageItem interface {
	Type() MessageItemType
	Bytes() []byte
	Len() int64
}

type BlockUpdate interface {
	BlockProof
	SrcHeight() int64
	TargetHeight() int64
}

type BlockProof interface {
	RelayMessageItem
	ProofHeight() int64
}

type MessageProof interface {
	RelayMessageItem
	StartSeqNum() int64
	LastSeqNum() int64
}

type Receiver interface {
	Start(bs *types.BMCLinkStatus) (<-chan ReceiveStatus, error)
	Stop()
	GetStatus() (ReceiveStatus, error)
	GetMarginForLimit() int64
	BuildBlockUpdate(bs *types.BMCLinkStatus, limit int64) ([]BlockUpdate, error)
	BuildBlockProof(bs *types.BMCLinkStatus, height int64) (BlockProof, error)
	BuildMessageProof(seq int64, limit int64) (MessageProof, error)
	GetHeightForSeq(seq int64) int64
	BuildRelayMessage(rmis []RelayMessageItem) ([]byte, error)
}
