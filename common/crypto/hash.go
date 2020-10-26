package crypto

import (
	"crypto/sha256"
	"io"

	"golang.org/x/crypto/hkdf"
	"golang.org/x/crypto/sha3"
)

// SHA3Sum256 returns the SHA3-256 digest of the data
func SHA3Sum256(m []byte) []byte {
	d := sha3.Sum256(m)
	return d[:]
}

// SHASum256 returns the SHA256 digest of the data
func SHASum256(m []byte) []byte {
	d := sha256.Sum256(m)
	return d[:]
}

func HKDF(secret, salt, info []byte, keyLen int) ([]byte, error){
	r := hkdf.New(sha3.New256, secret, salt, info)
	b := make([]byte, keyLen)
	if _, err := io.ReadFull(r, b[:]); err != nil {
		return nil, err
	}
	return b[:], nil
}