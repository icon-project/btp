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

package chain

import (
	"strings"

	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/p2p"
)

const (
	P2PScheme        = "bmrp"
	P2PAddressPrefix = P2PScheme + "://"
)

func ResolveP2PAddress(address string) string {
	if IsP2PAddress(address) {
		address = address[len(P2PAddressPrefix):]
	}
	return address
}

func IsP2PAddress(address string) bool {
	return strings.HasPrefix(address, P2PAddressPrefix)
}

var (
	ProtoGetLinkStatus = p2p.ProtocolInfo(0x0100)
	ProtoRelay         = p2p.ProtocolInfo(0x0200)
)

type GetLinkStatusRequest struct {
	Link string
}

type GetLinkStatusResponse struct {
	Link       string
	LinkStatus *module.BMCLinkStatus
	Error      string
}

type RelayRequest struct {
	From          string
	BlockUpdate   *module.BlockUpdate
	ReceiptProofs []*module.ReceiptProof
}

type RelayResponse struct {
	Error string
}
