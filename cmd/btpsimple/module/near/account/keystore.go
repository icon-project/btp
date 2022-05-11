package account

import (
	"bytes"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/btcsuite/btcutil/base58"
)

const ed25519Prefix = "ed25519:"

type Ed25519KeyPair struct {
	AccountID      string             `json:"account_id"`
	PublicKey      string             `json:"public_key"`
	PrivateKey     string             `json:"private_key,omitempty"`
	SecretKey      string             `json:"secret_key,omitempty"`
	Ed25519PubKey  ed25519.PublicKey  `json:"-"`
	Ed25519PrivKey ed25519.PrivateKey `json:"-"`
}

// GenerateEd25519KeyPair generates a new Ed25519 key pair for accountID.
func GenerateEd25519KeyPair(accountID string) (*Ed25519KeyPair, error) {
	var (
		keyPair Ed25519KeyPair
		err     error
	)
	keyPair.Ed25519PubKey, keyPair.Ed25519PrivKey, err = ed25519.GenerateKey(rand.Reader)

	if err != nil {
		return nil, err
	}

	keyPair.AccountID = accountID
	keyPair.PublicKey = ed25519Prefix + base58.Encode(keyPair.Ed25519PubKey)
	keyPair.PrivateKey = ed25519Prefix + base58.Encode(keyPair.Ed25519PrivKey)
	return &keyPair, nil
}

func (kp *Ed25519KeyPair) write(filename string) error {
	data, err := json.Marshal(kp)
	if err != nil {
		return err
	}
	return os.WriteFile(filename, data, 0600)
}

// Write the Ed25519 key pair to the unencrypted file system key store with
// networkID and return the filename of the written file.
func (kp *Ed25519KeyPair) Write(networkID string) (string, error) {
	homeDirectory, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}

	filename := filepath.Join(homeDirectory, ".near-credentials", networkID, kp.AccountID+".json")
	return filename, kp.write(filename)
}

// LoadKeyPair reads the Ed25519 key pair for the given ccountID from path
// returns it.
func LoadKeyPairFromPath(path, accountID string) (*Ed25519KeyPair, error) {
	buffer, err := os.ReadFile(path)

	if err != nil {
		return nil, err
	}

	var keyPair Ed25519KeyPair
	err = json.Unmarshal(buffer, &keyPair)
	if err != nil {
		return nil, err
	}

	// account ID
	if keyPair.AccountID != accountID {
		return nil, fmt.Errorf("keystore: parsed account_id '%s' does not match with accountID '%s'",
			keyPair.AccountID, accountID)
	}

	// public key
	if !strings.HasPrefix(keyPair.PublicKey, ed25519Prefix) {
		return nil, fmt.Errorf("keystore: parsed public_key '%s' is not an Ed25519 key",
			keyPair.PublicKey)
	}

	publicKey := base58.Decode(strings.TrimPrefix(keyPair.PublicKey, ed25519Prefix))
	keyPair.Ed25519PubKey = ed25519.PublicKey(publicKey)

	// private key
	var privateKey []byte
	if len(keyPair.PrivateKey) > 0 && len(keyPair.SecretKey) > 0 {
		return nil, fmt.Errorf("keystore: private_key and secret_key are defined at the same time: %s", path)
	} else if len(keyPair.PrivateKey) > 0 {
		if !strings.HasPrefix(keyPair.PrivateKey, ed25519Prefix) {
			return nil, fmt.Errorf("keystore: parsed private_key '%s' is not an Ed25519 key",
				keyPair.PrivateKey)
		}
		privateKey = base58.Decode(strings.TrimPrefix(keyPair.PrivateKey, ed25519Prefix))
	} else { // secret_key
		if !strings.HasPrefix(keyPair.SecretKey, ed25519Prefix) {
			return nil, fmt.Errorf("keystore: parsed secret_key '%s' is not an Ed25519 key",
				keyPair.SecretKey)
		}
		privateKey = base58.Decode(strings.TrimPrefix(keyPair.SecretKey, ed25519Prefix))
	}
	keyPair.Ed25519PrivKey = ed25519.PrivateKey(privateKey)

	// make sure keys match
	if !bytes.Equal(publicKey, keyPair.Ed25519PrivKey.Public().(ed25519.PublicKey)) {
		return nil, fmt.Errorf("keystore: public_key does not match private_key: %s", path)
	}
	return &keyPair, nil
}

// LoadKeyPair reads the Ed25519 key pair for the given networkID and
// accountID from the unencrypted file system key store and returns it.
func LoadKeyPair(networkID, accountID string) (*Ed25519KeyPair, error) {
	homeDirectory, err := os.UserHomeDir()
	if err != nil {
		return nil, err
	}
	filename := filepath.Join(homeDirectory, ".near-credentials", networkID, accountID+".json")
	return LoadKeyPairFromPath(filename, accountID)
}
