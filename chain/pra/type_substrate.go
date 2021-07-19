package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain/substrate"
)

func CreateStorageKey(meta *types.Metadata, prefix, method string, arg []byte, arg2 []byte) (substrate.SubstrateStorageKey, error) {
	key, err := types.CreateStorageKey(meta, prefix, method, arg, arg2)
	if err != nil {
		return nil, err
	}

	return substrate.SubstrateStorageKey(key), nil
}

type HeaderSubscription interface {
	Chan() <-chan types.Header
	Err() <-chan error
	Unsubscribe()
}

func NewSubstrateHashFromHexString(s string) substrate.SubstrateHash {
	hash, err := types.NewHashFromHexString(s)
	if err != nil {
		panic(err)
	}
	return hash
}
