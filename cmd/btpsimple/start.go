package main

import (
	"path"

	"github.com/icon-project/btp/btp"
	"github.com/icon-project/btp/common/cli"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"github.com/spf13/cobra"
)

func init() {
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
				cfg.BaseDir = path.Join(".", ".btpsimple", cfg.Src.Address.NetworkAddress())
			}

			var sr *btp.BTP
			if sr, err = btp.New(&cfg.Config, w, l); err != nil {
				return err
			}
			return sr.Serve()
		},
	}

	rootCmd.AddCommand(startCmd)

	startFlags := startCmd.Flags()
	startFlags.StringToString("mod_level", nil, "Set console log level for specific module ('mod'='level',...)")
	startFlags.String("cpuprofile", "", "CPU Profiling data file")
	startFlags.String("memprofile", "", "Memory Profiling data file")
	startFlags.MarkHidden("mod_level")

	cli.BindPFlags(rootVc, startFlags)
}
