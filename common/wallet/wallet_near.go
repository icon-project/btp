package wallet

import (
	"crypto"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/hex"
	"log"
)

type NearWallet struct {
	Skey *ed25519.PrivateKey
	Pkey *ed25519.PublicKey
}

func (w *NearWallet) PublicKey() []byte {
	pubKey := w.Skey.Public().(ed25519.PublicKey)

	return pubKey
}

func (w *NearWallet) Address() string {

	pubBytes := w.PublicKey()
	if len(pubBytes) != 32 {

		log.Panic("pubkey is incorrect size")

	}

	address := hex.EncodeToString(pubBytes)

	return address

}

func (w *NearWallet) Sign(data []byte) ([]byte, error) {

	signature, err := w.Skey.Sign(rand.Reader, data, crypto.Hash(0))
	if err != nil {
		return nil, err
	}

	return signature, nil
}

func (w *NearWallet) ECDH(pubkey []byte) ([]byte, error) {
	//Need to be implemnted
	return nil, nil
}

func NewNearwalletFromPrivateKey(sk *ed25519.PrivateKey) (*NearWallet, error) {

	pkey := sk.Public().(ed25519.PublicKey)
	return &NearWallet{
		Skey: sk,
		Pkey: &pkey,
	}, nil
}
