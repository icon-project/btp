package pra

import "github.com/centrifuge/go-substrate-rpc-client/v3/types"

type SubstrateHash types.Hash

func (hash SubstrateHash) Hash() types.Hash {
	return types.Hash(hash)
}

func (hash SubstrateHash) Bytes() []byte {
	return hash[:]
}

func (key SubstrateHash) Hex() string {
	return key.Hash().Hex()
}

type SubstrateMetaData struct {
	*types.Metadata
}

type SubstrateStorageKey types.StorageKey

func CreateStorageKey(meta *types.Metadata, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error) {
	key, err := types.CreateStorageKey(meta, prefix, method, arg, arg2)
	if err != nil {
		return nil, err
	}

	return SubstrateStorageKey(key), nil
}

func (key SubstrateStorageKey) StorageKey() types.StorageKey {
	return types.StorageKey(key)
}

func (key SubstrateStorageKey) Hex() string {
	return key.StorageKey().Hex()
}

type SubstrateEventRecordsRaw types.EventRecordsRaw

func (event SubstrateEventRecordsRaw) DecodeEventRecords(meta *SubstrateMetaData, records interface{}) error {
	return types.EventRecordsRaw(event).DecodeEventRecords(meta.Metadata, records)
}
