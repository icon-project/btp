package main

import (
	"fmt"
	"path"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/chain/icon/bridge"
	"github.com/icon-project/btp/chain/icon/btp2"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/types"
	"github.com/icon-project/btp/common/wallet"
)

func NewLink(cfg *Config, srcWallet wallet.Wallet, dstWallet wallet.Wallet, modLevels map[string]string) error {
	var err error
	linkErrCh := make(chan error)

	switch cfg.Direction {
	case FrontDirection:
		srcLog := setLogger(cfg, srcWallet, modLevels)
		srcLog.Debugln(cfg.FilePath, cfg.BaseDir)
		if cfg.BaseDir == "" {
			cfg.BaseDir = path.Join(".", ".btp2", cfg.Src.Address.NetworkAddress())
		}
		if _, err = newLink(cfg.Src.Address.BlockChain(), cfg.Config, srcLog, dstWallet, linkErrCh); err != nil {
			return err
		}

	case ReverseDirection:
		dstCfg := chain.Config{
			Src: cfg.Dst,
			Dst: cfg.Src,
		}

		dstLog := setLogger(cfg, dstWallet, modLevels)
		dstLog.Debugln(cfg.FilePath, cfg.BaseDir)
		if cfg.BaseDir == "" {
			cfg.BaseDir = path.Join(".", ".btp2", cfg.Dst.Address.NetworkAddress())
		}
		if _, err = newLink(cfg.Dst.Address.BlockChain(), dstCfg, dstLog, srcWallet, linkErrCh); err != nil {
			return err
		}
	case BothDirection:
		srcLog := setLogger(cfg, srcWallet, modLevels)
		srcLog.Debugln(cfg.FilePath, cfg.BaseDir)
		if cfg.BaseDir == "" {
			cfg.BaseDir = path.Join(".", ".btp2", cfg.Src.Address.NetworkAddress())
		}
		if _, err = newLink(cfg.Src.Address.BlockChain(), cfg.Config, srcLog, srcWallet, linkErrCh); err != nil {
			return err
		}

		dstCfg := chain.Config{
			Src: cfg.Dst,
			Dst: cfg.Src,
		}

		dstLog := setLogger(cfg, dstWallet, modLevels)
		dstLog.Debugln(cfg.FilePath, cfg.BaseDir)
		if cfg.BaseDir == "" {
			cfg.BaseDir = path.Join(".", ".btp2", cfg.Dst.Address.NetworkAddress())
		}
		if _, err = newLink(cfg.Dst.Address.BlockChain(), dstCfg, dstLog, dstWallet, linkErrCh); err != nil {
			return err
		}
	default:
		return fmt.Errorf("Not supported direction:%s", cfg.Direction)
	}

	for {
		select {
		case err := <-linkErrCh:
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func newLink(name string, cfg chain.Config, l log.Logger, w wallet.Wallet, linkErrCh chan error) (types.Link, error) {
	var lk types.Link
	switch name {
	case ICON:
		r := newReceiver("BTP2", cfg, l)
		lk = link.NewLink(&cfg, r, l)
	default:
		return nil, fmt.Errorf("Not supported for chain:%s", name)
	}

	go func() {
		err := lk.Start(newSender(cfg.Dst.Address.BlockChain(), cfg.Src, cfg.Dst, w, l))
		select {
		case linkErrCh <- err:
		default:
		}
	}()

	return lk, nil
}

func newReceiver(s string, cfg chain.Config, l log.Logger) link.Receiver {
	var receiver link.Receiver

	switch s {
	case "BTP2":
		receiver = btp2.NewBTP2(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, l)
	case "BRIDGE":
		receiver = bridge.NewBridge(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, l)
	default:
		l.Fatalf("Not supported for chain:%s", s)
		return nil
	}

	return receiver
}

func newSender(s string, srcCfg chain.BaseConfig, dstCfg chain.BaseConfig, w wallet.Wallet, l log.Logger) types.Sender {
	var sender types.Sender

	switch s {
	case ICON:
		sender = icon.NewSender(srcCfg.Address, dstCfg.Address, w, dstCfg.Endpoint, srcCfg.Options, l)
	default:
		l.Fatalf("Not supported for chain:%s", s)
		return nil
	}

	return sender
}
