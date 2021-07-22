package wallet

import (
	"crypto/ecdsa"

	"github.com/ethereum/go-ethereum/accounts/keystore"
)

func DecryptEvmKeyStore(ksData, pw []byte) (*ecdsa.PrivateKey, error) {
	key, err := keystore.DecryptKey(ksData, string(pw))
	if err != nil {
		return nil, err
	}
	return key.PrivateKey, nil
}
