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
	"os"

	"github.com/icon-project/btp/cmd/btpsimple/module/icon"
	"github.com/icon-project/btp/common/cli"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/intconv"
	"github.com/icon-project/btp/common/log"
	"github.com/spf13/cobra"
)

var rootCmd, rootVc = cli.NewCommand(nil, nil, "iconvalidators", "ICON Validators for ICON BMV on other chains")

func main() {
	client := icon.NewClient(os.Getenv("GOLOOP_RPC_URI"), log.New())

	rootCmd.AddCommand(&cobra.Command{
		Use:  "build",
		Args: cli.ArgsWithDefaultErrorFunc(cobra.ExactArgs(1)),
		RunE: func(cmd *cobra.Command, args []string) error {
			height, err := intconv.ParseInt(args[0], 64)
			if err != nil {
				return err
			}

			previousBlockHeaderInByte, err := client.GetBlockHeaderByHeight(&icon.BlockHeightParam{
				Height: icon.NewHexInt(height - 1),
			})
			if err != nil {
				panic(err)
			}
			var previousBlockHeader icon.BlockHeader
			_, err = codec.RLP.UnmarshalFromBytes(previousBlockHeaderInByte, &previousBlockHeader)
			if err != nil {
				panic(err)
			}

			validatorsInByte, err := client.GetDataByHash(&icon.DataHashParam{
				Hash: icon.NewHexBytes(previousBlockHeader.NextValidatorsHash),
			})

			if err != nil {
				panic(err)
			}

			return cli.JsonPrettyPrintln(os.Stdout, fmt.Sprintf("0x%x", validatorsInByte))
		},
	})

	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
}
