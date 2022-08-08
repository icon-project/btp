package main

import (
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/cmd/btp2/module/bsc"
	bscChain "github.com/icon-project/btp/cmd/btp2/module/bsc/chain"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	iconChain "github.com/icon-project/btp/cmd/btp2/module/icon/chain"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

type Link struct {
	sender   module.Chain
	receiver module.Chain
}

func NewLink(cfg *Config, w wallet.Wallet, l log.Logger) Link {
	var link Link

	link.sender = newChain(cfg.Src.Address.BlockChain(), cfg, w, l)

	dstCfg := module.Config{
		Src:       cfg.Dst,
		Dst:       cfg.Src,
		Nid:       cfg.Nid,
		Ntid:      cfg.Ntid,
		ProofFlag: cfg.ProofFlag,
		Offset:    cfg.Offset,
	}
	cfg.Config = dstCfg
	link.receiver = newChain(cfg.Dst.Address.BlockChain(), cfg, w, l)

	return link
}

func newChain(name string, cfg *Config, w wallet.Wallet, l log.Logger) module.Chain {
	var chain module.Chain
	switch name {
	case "icon":
		chain = iconChain.NewChain(&cfg.Config, l)
	case "bsc":
		chain = bscChain.NewChain(&cfg.Config, l)
	default:
		l.Fatalf("Sender not supported for chain ", cfg.Dst.Address.BlockChain())
		return nil
	}
	chain.Serve(newSender(cfg.Dst.Address.BlockChain(), cfg, w, l))
	return chain
}

func newSender(s string, cfg *Config, w wallet.Wallet, l log.Logger) module.Sender {
	var sender module.Sender

	switch s {
	case "icon":
		sender = icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Src.Options, l)
	case "bsc":
		sender = bsc.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, nil, l)
	default:
		l.Fatalf("Sender not supported for chain ", cfg.Dst.Address.BlockChain())
		return nil
	}

	return sender
}
