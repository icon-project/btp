package main

import (
	"fmt"
	"path/filepath"

	"github.com/icon-project/btp/common/cli"
	"github.com/spf13/cobra"
)

func init() {
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
	savePFlags := saveCmd.PersistentFlags()
	saveCmd.Flags().String("save_key_store", "", "KeyStore File path to save")

	cli.BindPFlags(rootVc, savePFlags)
	cli.MarkAnnotationCustom(savePFlags, "src.address", "dst.address", "src.endpoint", "dst.endpoint")

	rootCmd.AddCommand(saveCmd)
}
