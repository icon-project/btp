package chain

import (
	"github.com/icon-project/btp/cmd/btpsimple/module/base"
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
func newSenderAndReceiver(cfg *Config, w wallet.Wallet, l log.Logger) (base.Sender, base.Receiver) {
	var sender base.Sender
	var receiver base.Receiver

	switch cfg.Src.Address.BlockChain() {
	case "icon":
		receiver = icon.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, nil, l)
	case "bsc":
		receiver = bsc.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Dst.Options, l)
	default:
		receiver = GetReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Src.Options, l)	
	}

	switch cfg.Dst.Address.BlockChain() {
	case "icon":
		sender = icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Dst.Options, l)
	case "bsc":
		sender = bsc.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, nil, l)
	default:
		sender = GetSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Dst.Options, l)
	}

	return sender, receiver
}


func GetReceiver(source, destination base.BtpAddress, endpoint string, options map[string]interface{}, logger log.Logger) base.Receiver {
	client, err := base.GetClient(source.BlockChain())

	if err != nil {
		logger.Panic(err)
	}

	client.Initialize(endpoint, logger)
	receiver := base.NewReceiver(source, destination, endpoint, options, logger, client)

	return receiver
}

func GetSender(source, destination base.BtpAddress, wallet base.Wallet, endpoint string, options map[string]interface{}, logger log.Logger) base.Sender {
	client, err := base.GetClient(destination.BlockChain())

	if err != nil {
		logger.Panic(err)
	}

	client.Initialize(endpoint, logger)
	sender := base.NewSender(source, destination, wallet, endpoint, options, logger, client)

	return sender
}
