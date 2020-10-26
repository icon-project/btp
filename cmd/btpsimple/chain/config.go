package chain

import (
	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/common/config"
)

type BaseConfig struct {
	Address  module.BtpAddress `json:"address"`
	Endpoint string `json:"endpoint"`
	Options  map[string]interface{} `json:"options,omitempty"`
}

type Config struct {
	config.FileConfig `json:",squash"` //instead of `mapstructure:",squash"`
	Src BaseConfig `json:"src"`
	Dst BaseConfig `json:"dst"`

	Offset int64 `json:"offset"`
}
