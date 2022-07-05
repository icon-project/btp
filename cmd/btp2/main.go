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
	"encoding/json"
	"fmt"
	"github.com/icon-project/btp/cmd/btp2/module"
	"github.com/icon-project/btp/cmd/btp2/module/icon"
	iconChain "github.com/icon-project/btp/cmd/btp2/module/icon/chain"
	"io/ioutil"
	stdlog "log"
	"os"
	"path"
	"path/filepath"
	"strings"

	"github.com/spf13/cobra"

	"github.com/icon-project/btp/common/cli"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

var (
	version = "unknown"
	build   = "unknown"
)

const (
	DefaultKeyStorePass = "btp2"
)

type Config struct {
	module.Config `json:",squash"` //instead of `mapstructure:",squash"`
	KeyStoreData  json.RawMessage `json:"key_store"`
	KeyStorePass  string          `json:"key_password,omitempty"`
	KeySecret     string          `json:"key_secret,omitempty"`

	LogLevel     string               `json:"log_level"`
	ConsoleLevel string               `json:"console_level"`
	LogForwarder *log.ForwarderConfig `json:"log_forwarder,omitempty"`
	LogWriter    *log.WriterConfig    `json:"log_writer,omitempty"`
}

func (c *Config) Wallet() (wallet.Wallet, error) {
	pw, err := c.resolvePassword()
	if err != nil {
		return nil, err
	}
	return wallet.NewFromKeyStore(c.KeyStoreData, pw)
}

func (c *Config) resolvePassword() ([]byte, error) {
	if c.KeySecret != "" {
		return ioutil.ReadFile(c.KeySecret)
	} else {
		if c.KeyStorePass == "" {
			return []byte(DefaultKeyStorePass), nil
		} else {
			return []byte(c.KeyStorePass), nil
		}
	}
}

func (c *Config) EnsureWallet() error {
	pw, err := c.resolvePassword()
	if err != nil {
		return err
	}
	if len(c.KeyStoreData) < 1 {
		priK, _ := crypto.GenerateKeyPair()
		if ks, err := wallet.EncryptKeyAsKeyStore(priK, pw); err != nil {
			return err
		} else {
			c.KeyStoreData = ks
		}
	} else {
		if _, err := wallet.DecryptKeyStore(c.KeyStoreData, pw); err != nil {
			return errors.Errorf("fail to decrypt KeyStore err=%+v", err)
		}
	}
	return nil
}

var logoLines = []string{
	"  ____ _____ ____    ____      _",
	" | __ )_   _|  _ \\  |  _ \\ ___| | __ _ _   _",
	" |  _ \\ | | | |_) | | |_) / _ \\ |/ _` | | | |",
	" | |_) || | |  __/  |  _ <  __/ | (_| | |_| |",
	" |____/ |_| |_|     |_| \\_\\___|_|\\__,_|\\__, |",
	"                                       |___/ ",
}

func main() {
	rootCmd, rootVc := cli.NewCommand(nil, nil, "btp2", "BTP Relay CLI")
	cfg := &Config{}
	rootCmd.Long = "Command Line Interface of Relay for Blockchain Transmission Protocol"
	cli.SetEnvKeyReplacer(rootVc, strings.NewReplacer(".", "_"))
	//rootVc.Debug()

	rootCmd.AddCommand(&cobra.Command{
		Use:   "version",
		Short: "Print btp2 version",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("btp2 version", version, build)
		},
	})

	rootCmd.PersistentPreRunE = func(cmd *cobra.Command, args []string) error {
		baseDir := rootVc.GetString("base_dir")
		logfile := rootVc.GetString("log_writer.filename")
		cfg.FilePath = rootVc.GetString("config")
		if cfg.FilePath != "" {
			f, err := os.Open(cfg.FilePath)
			if err != nil {
				return fmt.Errorf("fail to open config file=%s err=%+v", cfg.FilePath, err)
			}
			rootVc.SetConfigType("json")
			err = rootVc.ReadConfig(f)
			if err != nil {
				return fmt.Errorf("fail to read config file=%s err=%+v", cfg.FilePath, err)
			}
			cfg.FilePath, _ = filepath.Abs(cfg.FilePath)
		}
		if err := rootVc.Unmarshal(&cfg, cli.ViperDecodeOptJson); err != nil {
			return fmt.Errorf("fail to unmarshall config from env err=%+v", err)
		}
		if baseDir != "" {
			cfg.BaseDir = cfg.ResolveRelative(baseDir)
		}
		if logfile != "" {
			cfg.LogWriter.Filename = cfg.ResolveRelative(logfile)
		}
		return nil
	}
	rootPFlags := rootCmd.PersistentFlags()
	rootPFlags.String("src.address", "", "BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC)")
	rootPFlags.String("src.endpoint", "", "Endpoint of source blockchain")
	rootPFlags.StringToString("src.options", nil, "Options, comma-separated 'key=value'")
	rootPFlags.String("dst.address", "", "BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC)")
	rootPFlags.String("dst.endpoint", "", "Endpoint of destination blockchain")
	rootPFlags.StringToString("dst.options", nil, "Options, comma-separated 'key=value'")

	//BTP2.0
	rootPFlags.Int64("ntid", 1, "network type id")
	rootPFlags.Int64("nid", 3, "network id")
	rootPFlags.Int64("proofFlag", 1, "btp2.0 notification proof flag")

	rootPFlags.Int64("offset", 0, "Offset of MTA")
	rootPFlags.String("key_store", "", "KeyStore")
	rootPFlags.String("key_password", "", "Password of KeyStore")
	rootPFlags.String("key_secret", "", "Secret(password) file for KeyStore")
	//
	rootPFlags.String("base_dir", "", "Base directory for data")
	rootPFlags.StringP("config", "c", "", "Parsing configuration file")
	//
	rootPFlags.String("log_level", "debug", "Global log level (trace,debug,info,warn,error,fatal,panic)")
	rootPFlags.String("console_level", "trace", "Console log level (trace,debug,info,warn,error,fatal,panic)")
	//
	rootPFlags.String("log_forwarder.vendor", "", "LogForwarder vendor (fluentd,logstash)")
	rootPFlags.String("log_forwarder.address", "", "LogForwarder address")
	rootPFlags.String("log_forwarder.level", "info", "LogForwarder level")
	rootPFlags.String("log_forwarder.name", "", "LogForwarder name")
	rootPFlags.StringToString("log_forwarder.options", nil, "LogForwarder options, comma-separated 'key=value'")
	//
	rootPFlags.String("log_writer.filename", "", "Log file name (rotated files resides in same directory)")
	rootPFlags.Int("log_writer.maxsize", 100, "Maximum log file size in MiB")
	rootPFlags.Int("log_writer.maxage", 0, "Maximum age of log file in day")
	rootPFlags.Int("log_writer.maxbackups", 0, "Maximum number of backups")
	rootPFlags.Bool("log_writer.localtime", false, "Use localtime on rotated log file instead of UTC")
	rootPFlags.Bool("log_writer.compress", false, "Use gzip on rotated log file")
	cli.BindPFlags(rootVc, rootPFlags)
	err := cli.MarkAnnotationCustom(rootPFlags, "src.address", "dst.address", "src.endpoint", "dst.endpoint")
	if err != nil {
		return
	}
	saveCmd := &cobra.Command{
		Use:   "save [file]",
		Short: "Save configuration",
		Args:  cli.ArgsWithDefaultErrorFunc(cobra.ExactArgs(1)),
		PreRunE: func(cmd *cobra.Command, args []string) error {
			if err := cfg.EnsureWallet(); err != nil {
				return fmt.Errorf("fail to ensure src wallet err:%+v", err)
			} else {
				cfg.KeyStorePass = ""
			}
			return nil
		},
		RunE: func(cmd *cobra.Command, args []string) error {
			saveFilePath := args[0]
			cfg.FilePath, _ = filepath.Abs(saveFilePath)
			cfg.BaseDir = cfg.ResolveRelative(cfg.BaseDir)

			if cfg.LogWriter != nil {
				cfg.LogWriter.Filename = cfg.ResolveRelative(cfg.LogWriter.Filename)
			}

			if err := cli.JsonPrettySaveFile(saveFilePath, 0644, cfg); err != nil {
				return err
			}
			cmd.Println("Save configuration to", saveFilePath)
			if saveKeyStore, _ := cmd.Flags().GetString("save_key_store"); saveKeyStore != "" {
				if err := cli.JsonPrettySaveFile(saveKeyStore, 0600, cfg.KeyStoreData); err != nil {
					return err
				}
			}
			return nil
		},
	}
	rootCmd.AddCommand(saveCmd)
	saveCmd.Flags().String("save_key_store", "", "KeyStore File path to save")

	startCmd := &cobra.Command{
		Use:   "start",
		Short: "Start server",
		PreRunE: func(cmd *cobra.Command, args []string) error {
			return cli.ValidateFlagsWithViper(rootVc, cmd.Flags())
		},
		RunE: func(cmd *cobra.Command, args []string) error {
			for _, l := range logoLines {
				log.Println(l)
			}
			log.Printf("Version : %s", version)
			log.Printf("Build   : %s", build)

			var (
				err error
				w   wallet.Wallet
			)
			if w, err = cfg.Wallet(); err != nil {
				return err
			}
			modLevels, _ := cmd.Flags().GetStringToString("mod_level")
			l := setLogger(cfg, w, modLevels)
			l.Debugln(cfg.FilePath, cfg.BaseDir)
			if cfg.BaseDir == "" {
				cfg.BaseDir = path.Join(".", ".btp2", cfg.Src.Address.NetworkAddress())
			}

			//icon -> icon
			sh := iconChain.NewChain(&cfg.Config, w, l)
			s := icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Src.Options, l)

			//icon -> bsc
			//sh := iconChain.NewChain(&cfg.Config, w, l)
			//s := bsc.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Src.Options, l)

			//bsc -> icon
			//sh := bscChain.NewChain(&cfg.Config, w, l)
			//s := icon.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Src.Options, l)

			return sh.Serve(s)
		},
	}
	rootCmd.AddCommand(startCmd)
	startFlags := startCmd.Flags()
	startFlags.StringToString("mod_level", nil, "Set console log level for specific module ('mod'='level',...)")
	startFlags.String("cpuprofile", "", "CPU Profiling data file")
	startFlags.String("memprofile", "", "Memory Profiling data file")
	startFlags.MarkHidden("mod_level")

	cli.BindPFlags(rootVc, startFlags)

	genMdCmd := cli.NewGenerateMarkdownCommand(rootCmd, rootVc)
	genMdCmd.Hidden = true

	rootCmd.SilenceUsage = true
	if err := rootCmd.Execute(); err != nil {
		fmt.Printf("%+v", err)
		os.Exit(1)
	}
}

func setLogger(cfg *Config, w wallet.Wallet, modLevels map[string]string) log.Logger {
	l := log.WithFields(log.Fields{log.FieldKeyWallet: w.Address()[2:]})
	log.SetGlobalLogger(l)
	stdlog.SetOutput(l.WriterLevel(log.WarnLevel))
	if cfg.LogWriter != nil {
		if cfg.LogWriter.Filename == "" {
			log.Debugln("LogWriterConfig filename is empty string, will be ignore")
		} else {
			var lwCfg log.WriterConfig
			lwCfg = *cfg.LogWriter
			lwCfg.Filename = cfg.ResolveAbsolute(lwCfg.Filename)
			w, err := log.NewWriter(&lwCfg)
			if err != nil {
				log.Panicf("Fail to make writer err=%+v", err)
			}
			err = l.SetFileWriter(w)
			if err != nil {
				log.Panicf("Fail to set file l err=%+v", err)
			}
		}
	}

	if lv, err := log.ParseLevel(cfg.LogLevel); err != nil {
		log.Panicf("Invalid log_level=%s", cfg.LogLevel)
	} else {
		l.SetLevel(lv)
	}
	if lv, err := log.ParseLevel(cfg.ConsoleLevel); err != nil {
		log.Panicf("Invalid console_level=%s", cfg.ConsoleLevel)
	} else {
		l.SetConsoleLevel(lv)
	}

	for mod, lvStr := range modLevels {
		if lv, err := log.ParseLevel(lvStr); err != nil {
			log.Panicf("Invalid mod_level mod=%s level=%s", mod, lvStr)
		} else {
			l.SetModuleLevel(mod, lv)
		}
	}

	if cfg.LogForwarder != nil {
		if cfg.LogForwarder.Vendor == "" && cfg.LogForwarder.Address == "" {
			log.Debugln("LogForwarderConfig vendor and address is empty string, will be ignore")
		} else {
			if err := log.AddForwarder(cfg.LogForwarder); err != nil {
				log.Fatalf("Invalid log_forwarder err:%+v", err)
			}
		}
	}

	return l
}
