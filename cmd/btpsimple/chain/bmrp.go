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
	BlockProof    *module.BlockProof
	ReceiptProofs []*module.ReceiptProof
}

type RelayResponse struct {
	Error string
}
