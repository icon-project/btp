package wallet

import (
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/crypto"
)

type Wallet interface {
	Address() string
	Sign(data []byte) ([]byte, error)
	PublicKey() []byte
	ECDH(pubKey []byte) ([]byte, error)
}

type softwareWallet struct {
	skey *crypto.PrivateKey
	pkey *crypto.PublicKey
}

func (w *softwareWallet) Address() string {
	return common.NewAccountAddressFromPublicKey(w.pkey).String()
}

func (w *softwareWallet) Sign(data []byte) ([]byte, error) {
	sig, err := crypto.NewSignature(data, w.skey)
	if err != nil {
		return nil, err
	}
	return sig.SerializeRSV()
}

func (w *softwareWallet) PublicKey() []byte {
	return w.pkey.SerializeCompressed()
}

func (w *softwareWallet) ECDH(pubKey []byte) ([]byte, error) {
	pkey, err := crypto.ParsePublicKey(pubKey)
	if err != nil {
		return nil, err
	}
	return w.skey.ECDH(pkey), nil
}

func New() *softwareWallet {
	sk, pk := crypto.GenerateKeyPair()
	return &softwareWallet{
		skey: sk,
		pkey: pk,
	}
}

func NewIcxWalletFromPrivateKey(sk *crypto.PrivateKey) (*softwareWallet, error) {
	pk := sk.PublicKey()
	return &softwareWallet{
		skey: sk,
		pkey: pk,
	}, nil
}
