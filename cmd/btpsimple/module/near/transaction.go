package near

import (
	"crypto"
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"fmt"

	"github.com/near/borsh-go"
)

const (
	ED25519 = 0
)

func fullAccessKey() AccessKey {
	return AccessKey{
		Nonce: 0,
		Permission: AccessKeyPermission{
			Enum:       1,
			FullAccess: 1,
		},
	}
}

func PublicKeyFromEd25519(pk ed25519.PublicKey) PublicKey {
	var publicKey PublicKey
	publicKey.KeyType = ED25519
	copy(publicKey.Data[:], pk)
	return publicKey
}

func SignTransaction(tx *Transaction, privKey ed25519.PrivateKey) ([]byte, error) {
	data, err := borsh.Serialize(*tx)

	if err != nil {
		return nil, err
	}

	presignedData := sha256.Sum256(data)

	sign, err := privKey.Sign(rand.Reader, presignedData[:], crypto.Hash(0))

	if err != nil {
		return nil, err
	}

	if len(sign) != 64 {
		return nil, fmt.Errorf("sign error,length is not equal 64,length=%d", len(sign))
	}

	return sign, nil
}

func CreateSignatureTransaction(tx *Transaction, sig []byte) (*SignedTransaction, error) {

	var signature Signature
	signature.KeyType = ED25519
	copy(signature.Data[:], sig)

	var signedTransaction SignedTransaction
	signedTransaction.Transaction = *tx
	signedTransaction.Signature = signature

	return &signedTransaction, nil
}
