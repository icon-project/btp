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

package account

import (
	"crypto/ed25519"
	"crypto/rand"
	"encoding/hex"
	"errors"

	"github.com/btcsuite/btcutil/base58"
)

func GenerateKey() ([]byte, []byte, error) {
	publicKey, privateKey, err := ed25519.GenerateKey(rand.Reader)

	if err != nil {
		return nil, nil, err
	}

	return privateKey.Seed(), publicKey, nil
}
func GeneratePubKeyBySeed(seed []byte) ([]byte, []byte, error) {
	privateKey := ed25519.NewKeyFromSeed(seed)
	publicKey := privateKey.Public().(ed25519.PublicKey)

	if len(publicKey) != 32 {
		return nil, nil, errors.New("public key length is not equal 32")
	}

	return publicKey, privateKey, nil
}

func GeneratePubKeyByBase58(b58Key string) ([]byte, []byte, error) {
	seed := base58.Decode(b58Key)

	if len(seed) == 0 {
		return nil, nil, errors.New("base 58 decode error")
	}

	if len(seed) != 32 {
		return nil, nil, errors.New("seed length is not equal 32")
	}

	return GeneratePubKeyBySeed(seed)
}

func PublicKeyToString(pub []byte) string {
	publicKey := base58.Encode(pub)
	return "ed25519:" + publicKey
}

func KeyToAddress(key []byte) string {
	return hex.EncodeToString(key)
}
