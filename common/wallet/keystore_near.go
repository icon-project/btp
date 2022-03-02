package wallet

import (
	"github.com/hugobyte/keygen/keystore/near"
	"golang.org/x/crypto/ed25519"
)

func DecryptNearKeyStore(ksData, pw []byte) (*ed25519.PrivateKey, error) {
	key, err := near.DecryptKey(ksData, string(pw))
	if err != nil {
		return nil, err
	}
	return &key, nil
}
