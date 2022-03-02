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

package utils

import (
	"fmt"
	"math/big"
)

func BigIntToUintBytes(i *big.Int, bytelen int) ([]byte, error) {
	if i.Sign() < 0 {
		return nil, fmt.Errorf("cannot encode a negative big.Int into an unsigned integer")
	}

	max := big.NewInt(0).Exp(big.NewInt(2), big.NewInt(int64(bytelen*8)), nil)
	if i.CmpAbs(max) > 0 {
		return nil, fmt.Errorf("cannot encode big.Int to []byte: given big.Int exceeds highest number "+
			"%v for an uint with %v bits", max, bytelen*8)
	}

	result := make([]byte, bytelen)
	bytes := i.Bytes()
	copy(result[len(result)-len(bytes):], bytes)
	return result, nil
}

// Reverse reverses bytes in place (manipulates the underlying array)
func Reverse(b []byte) {
	for i, j := 0, len(b)-1; i < j; i, j = i+1, j-1 {
		b[i], b[j] = b[j], b[i]
	}
}
