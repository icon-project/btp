package chain

import (
	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/cmd/btpsimple/module/bsc"
	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

/* Adapted from ICONDAO branch
* TODO: figure out how to implement simple plugin system in golang.

Using importer, target blockchain package can be programmatically imported, this will eliminate the need for switch
statement and package imports for each blockchain. It's a minor improvement at this point, considering we don't have
that many networks yet in BTP.

@see https://pkg.go.dev/go/importer
*/
func newSenderAndReceiver(cfg *Config, w wallet.Wallet, l log.Logger) (module.Sender, module.Receiver) {
	var sender module.Sender
	var receiver module.Receiver

	switch cfg.Dst.Address.BlockChain() {
	case "icon":
		sender = icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Dst.Options, l)
	case "bsc":
		sender = bsc.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, nil, l)
	default:
		l.Fatalf("Sender not supported for chain ", cfg.Dst.Address.BlockChain())
		return nil, nil
	}

	switch cfg.Src.Address.BlockChain() {
	case "icon":
		receiver = icon.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, nil, l)
	case "bsc":
		receiver = bsc.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Dst.Options, l)
	default:
		l.Fatalf("Receiver not supported for chain ", cfg.Src.Address.BlockChain())
		return nil, nil
	}

	return sender, receiver
}
