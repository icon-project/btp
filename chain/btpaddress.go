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
	"fmt"
	"strings"
)

type BtpAddress string

func (a BtpAddress) Protocol() string {
	s := string(a)
	if i := strings.Index(s, "://"); i > 0 {
		return s[:i]
	}
	return ""
}
func (a BtpAddress) NetworkAddress() string {
	if a.Protocol() != "" {
		ss := strings.Split(string(a), "/")
		if len(ss) > 2 {
			return ss[2]
		}
	}
	return ""
}
func (a BtpAddress) network() (string, string) {
	if s := a.NetworkAddress(); s != "" {
		ss := strings.Split(s, ".")
		if len(ss) > 1 {
			return ss[0], ss[1]
		} else {
			return "", ss[0]
		}
	}
	return "", ""
}
func (a BtpAddress) BlockChain() string {
	_, v := a.network()
	return v
}
func (a BtpAddress) NetworkID() string {
	n, _ := a.network()
	return n
}
func (a BtpAddress) Account() string {
	if a.Protocol() != "" {
		ss := strings.Split(string(a), "/")
		if len(ss) > 3 {
			return ss[3]
		}
	}
	return ""
}

func (a BtpAddress) ContractAddress() string {
	if a.Protocol() != "" {
		ss := strings.Split(string(a), "/")
		if len(ss) > 3 {
			return ss[3]
		}
	}
	return ""
}

func (a BtpAddress) String() string {
	return string(a)
}

func (a *BtpAddress) Set(v string) error {
	*a = BtpAddress(v)
	return nil
}

func (a BtpAddress) Type() string {
	return "BtpAddress"
}

func ValidateBtpAddress(ba BtpAddress) error {
	switch p := ba.Protocol(); p {
	case "btp":
	default:
		return fmt.Errorf("not supported protocol:%s", p)
	}
	switch v := ba.BlockChain(); v {
	case "icon":
	case "iconee":
	default:
		return fmt.Errorf("not supported blockchain:%s", v)
	}
	if len(ba.Account()) < 1 {
		return fmt.Errorf("empty account")
	}
	return nil
}
