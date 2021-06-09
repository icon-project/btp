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

package base

import (
	"fmt"
	"strings"
)

/*-------------------Types--------------------------*/
type BtpAddress string

/*--------------------Private Functions-------------*/

func (ba BtpAddress) network() (string, string) {
	if s := ba.NetworkAddress(); s != "" {
		ss := strings.Split(s, ".")
		if len(ss) > 1 {
			return ss[0], ss[1]
		} else {
			return "", ss[0]
		}
	}
	return "", ""
}

/*-------------------Public Functions------------------*/

func (ba BtpAddress) Protocol() string {
	s := string(ba)
	if i := strings.Index(s, "://"); i > 0 {
		return s[:i]
	}
	return ""
}

func (ba BtpAddress) NetworkAddress() string {
	if ba.Protocol() != "" {
		ss := strings.Split(string(ba), "/")
		if len(ss) > 2 {
			return ss[2]
		}
	}
	return ""
}

func (ba BtpAddress) BlockChain() string {
	_, v := ba.network()
	return v
}

func (ba BtpAddress) NetworkID() string {
	n, _ := ba.network()
	return n
}

func (ba BtpAddress) ContractAddress() string {
	if ba.Protocol() != "" {
		ss := strings.Split(string(ba), "/")
		if len(ss) > 3 {
			return ss[3]
		}
	}
	return ""
}

func (ba BtpAddress) String() string {
	return string(ba)
}

func (ba *BtpAddress) Set(v string) error {
	*ba = BtpAddress(v)
	return nil
}

func (ba BtpAddress) Type() string {
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

	if len(ba.ContractAddress()) < 1 {
		return fmt.Errorf("empty contract address")
	}

	return nil
}
