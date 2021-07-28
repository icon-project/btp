package pra

import (
	"testing"

	"github.com/icon-project/btp/common/log"
)

func TestNewRelayReceiver(t *testing.T) {
	// t.Skip("Manual testing only")
	NewRelayReceiver("wss://wss-relay.testnet.moonbeam.network/", "http://goloop.linhnc.info/api/v3/icondao", "cx4fd0306a26d8b812cf9cc5aacb68e2dab18fa14d", log.New())
}
