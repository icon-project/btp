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

package link

import "github.com/icon-project/btp/common/types"

type MessageItemType int

const (
	TypeBlockUpdate MessageItemType = iota + 1
	TypeBlockProof
	TypeMessageProof
)

type RelayMessageItem interface {
	Type() MessageItemType
	Precedency() int
	Bytes() []byte
	Len() int64
}

type BlockUpdate interface {
	RelayMessageItem
	SrcHeight() int64
	TargetHeight() int64
}

type BlockProof interface {
	RelayMessageItem
	TargetHeight() int64
}

type MessageProof interface {
	RelayMessageItem
}

type RelayMessageList []RelayMessageItem
type ReceiveCallback func(bml RelayMessageList) error

type Receiver interface {
	BuildBlockUpdate(bs *types.BMCLinkStatus, limit int64) ([]BlockUpdate, error)
	BuildBlockProof(height int64) (BlockProof, error)
	BuildMessageProof(bs *types.BMCLinkStatus, limit int64) ([]MessageProof, error)
}
