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

package chain

import (
	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common/config"
)

type BaseConfig struct {
	Address  base.BtpAddress `json:"address"`
	Endpoint string `json:"endpoint"`
	Options  map[string]interface{} `json:"options,omitempty"`
}

type Config struct {
	config.FileConfig `json:",squash"` //instead of `mapstructure:",squash"`
	Src BaseConfig `json:"src"`
	Dst BaseConfig `json:"dst"`

	Offset int64 `json:"offset"`
}
