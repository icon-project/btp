package wallet

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"fmt"
	"io"

	"github.com/icon-project/btp/common/crypto"
)

const (
	EncryptSaltSize = 4
	HKDFInfo        = "BTP_SECRET_HKDF"
	AES128KeySize   = 16
	HKDFKeySize     = 2 * AES128KeySize
)

type Encrypted struct {
	Param      []byte `json:"param"`
	CipherText []byte `json:"cipher_text"`
}

func (e *Encrypted) Aes128CTRStream(w Wallet) (cipher.Stream, error) {
	if len(e.Param) < EncryptSaltSize {
		return nil, fmt.Errorf("invalid param length")
	}
	salt := e.Param[:EncryptSaltSize]
	pubKey := e.Param[EncryptSaltSize:]
	var err error
	var secret, key []byte
	if secret, err = w.ECDH(pubKey); err != nil {
		return nil, err
	}
	if key, err = crypto.HKDF(secret, salt, []byte(HKDFInfo), HKDFKeySize); err != nil {
		return nil, err
	}
	var blk cipher.Block
	if blk, err = aes.NewCipher(key[:AES128KeySize]); err != nil {
		return nil, err
	}
	return cipher.NewCTR(blk, key[AES128KeySize:]), nil
}

func (e *Encrypted) Decrypt(w Wallet) ([]byte, error) {
	s, err := e.Aes128CTRStream(w)
	if err != nil {
		return nil, err
	}
	b := make([]byte, len(e.CipherText))
	s.XORKeyStream(b, e.CipherText)
	return b, nil
}

func (e *Encrypted) Encrypt(w Wallet, b []byte) error {
	s, err := e.Aes128CTRStream(w)
	if err != nil {
		return err
	}
	s.XORKeyStream(e.CipherText, b)
	return nil
}

func NewEncrypted(w Wallet, pubKey, b []byte) (*Encrypted, error) {
	e := &Encrypted{
		Param:      make([]byte, EncryptSaltSize+len(pubKey)),
		CipherText: make([]byte, len(b)),
	}
	if _, err := io.ReadFull(rand.Reader, e.Param[:EncryptSaltSize]); err != nil {
		return nil, err
	}
	copy(e.Param[EncryptSaltSize:], pubKey)
	s, err := e.Aes128CTRStream(w)
	if err != nil {
		return nil, err
	}
	s.XORKeyStream(e.CipherText, b)
	copy(e.Param[EncryptSaltSize:], w.PublicKey())
	return e, nil
}

