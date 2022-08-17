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
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/cmd/btp2/module/bsc"
	bscChain "github.com/icon-project/btp/cmd/btp2/module/bsc/chain"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	iconChain "github.com/icon-project/btp/cmd/btp2/module/icon/chain"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"path"
)

func NewLink(cfg *Config, srcWallet wallet.Wallet, dstWallet wallet.Wallet, modLevels map[string]string) error {
	var err error
	linkErrCh := make(chan error)

	srcLog := setLogger(cfg, srcWallet, modLevels)
	srcLog.Debugln(cfg.FilePath, cfg.BaseDir)
	if cfg.BaseDir == "" {
		cfg.BaseDir = path.Join(".", ".btp2", cfg.Src.Address.NetworkAddress())
	}
	if _, err = newChain(cfg.Src.Address.BlockChain(), cfg.Config, srcLog, srcWallet, linkErrCh); err != nil {
		return err
	}

	dstCfg := module.Config{
		Src:       cfg.Dst,
		Dst:       cfg.Src,
		ProofFlag: cfg.ProofFlag,
		Offset:    cfg.Offset,
	}

	dstLog := setLogger(cfg, dstWallet, modLevels)
	dstLog.Debugln(cfg.FilePath, cfg.BaseDir)
	if cfg.BaseDir == "" {
		cfg.BaseDir = path.Join(".", ".btp2", cfg.Dst.Address.NetworkAddress())
	}
	if _, err = newChain(cfg.Dst.Address.BlockChain(), dstCfg, dstLog, dstWallet, linkErrCh); err != nil {
		return err
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

func newChain(name string, cfg module.Config, l log.Logger, w wallet.Wallet, linkErrCh chan error) (module.Chain, error) {
	var chain module.Chain
	switch name {
	case "icon":
		chain = iconChain.NewChain(&cfg, l)
	case "bsc":
		chain = bscChain.NewChain(&cfg, l)
	default:
		return nil, fmt.Errorf("sender not supported for chain:%s", cfg.Dst.Address.BlockChain())
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

func newSender(s string, srcCfg module.BaseConfig, dstCfg module.BaseConfig, w wallet.Wallet, l log.Logger) module.Sender {
	var sender module.Sender

	switch s {
	case "icon":
		sender = icon.NewSender(srcCfg.Address, dstCfg.Address, w, dstCfg.Endpoint, srcCfg.Options, l)
	case "bsc":
		sender = bsc.NewSender(srcCfg.Address, dstCfg.Address, w, dstCfg.Endpoint, nil, l)
	default:
		l.Fatalf("Sender not supported for chain ", dstCfg.Address.BlockChain())
		return nil
	}

	return sender
}
