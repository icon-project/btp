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

package main

import (
	"fmt"
	"path"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/bsc"
	bscChain "github.com/icon-project/btp/chain/bsc"
	"github.com/icon-project/btp/chain/icon"
	iconChain "github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/common/log"
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
		if _, err = newChain(cfg.Src.Address.BlockChain(), cfg.Config, srcLog, srcWallet, linkErrCh); err != nil {
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
		if _, err = newChain(cfg.Dst.Address.BlockChain(), dstCfg, dstLog, dstWallet, linkErrCh); err != nil {
			return err
		}
	case BothDirection:
		srcLog := setLogger(cfg, srcWallet, modLevels)
		srcLog.Debugln(cfg.FilePath, cfg.BaseDir)
		if cfg.BaseDir == "" {
			cfg.BaseDir = path.Join(".", ".btp2", cfg.Src.Address.NetworkAddress())
		}
		if _, err = newChain(cfg.Src.Address.BlockChain(), cfg.Config, srcLog, srcWallet, linkErrCh); err != nil {
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
		if _, err = newChain(cfg.Dst.Address.BlockChain(), dstCfg, dstLog, dstWallet, linkErrCh); err != nil {
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

func newChain(name string, cfg chain.Config, l log.Logger, w wallet.Wallet, linkErrCh chan error) (chain.Chain, error) {
	var chain chain.Chain
	switch name {
	case ICON:
		chain = iconChain.NewChain(&cfg, l)
	case ETH:
		chain = bscChain.NewChain(&cfg, l)
	default:
		return nil, fmt.Errorf("Not supported for chain:%s", name)
	}

	go func() {
		err := chain.Serve(newSender(cfg.Src.Address.BlockChain(), cfg.Src, cfg.Dst, w, l))
		select {
		case linkErrCh <- err:
		default:
		}
	}()

	return chain, nil
}

func newSender(s string, srcCfg chain.BaseConfig, dstCfg chain.BaseConfig, w wallet.Wallet, l log.Logger) chain.Sender {
	var sender chain.Sender

	switch s {
	case ICON:
		sender = icon.NewSender(srcCfg.Address, dstCfg.Address, w, dstCfg.Endpoint, srcCfg.Options, l)
	case ETH:
		sender = bsc.NewSender(srcCfg.Address, dstCfg.Address, w, dstCfg.Endpoint, nil, l)
	default:
		l.Fatalf("Not supported for chain:%s", s)
		return nil
	}

	return sender
}
