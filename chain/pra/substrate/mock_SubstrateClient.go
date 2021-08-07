// Code generated by mockery v0.0.0-dev. DO NOT EDIT.

package substrate

import (
	types "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	mock "github.com/stretchr/testify/mock"
)

// MockSubstrateClient is an autogenerated mock type for the SubstrateClient type
type MockSubstrateClient struct {
	mock.Mock
}

// Call provides a mock function with given fields: result, method, args
func (_m *MockSubstrateClient) Call(result interface{}, method string, args ...interface{}) error {
	var _ca []interface{}
	_ca = append(_ca, result, method)
	_ca = append(_ca, args...)
	ret := _m.Called(_ca...)

	var r0 error
	if rf, ok := ret.Get(0).(func(interface{}, string, ...interface{}) error); ok {
		r0 = rf(result, method, args...)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// CreateStorageKey provides a mock function with given fields: meta, prefix, method, arg, arg2
func (_m *MockSubstrateClient) CreateStorageKey(meta *types.Metadata, prefix string, method string, arg []byte, arg2 []byte) (types.StorageKey, error) {
	ret := _m.Called(meta, prefix, method, arg, arg2)

	var r0 types.StorageKey
	if rf, ok := ret.Get(0).(func(*types.Metadata, string, string, []byte, []byte) types.StorageKey); ok {
		r0 = rf(meta, prefix, method, arg, arg2)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(types.StorageKey)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(*types.Metadata, string, string, []byte, []byte) error); ok {
		r1 = rf(meta, prefix, method, arg, arg2)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetBlockHash provides a mock function with given fields: blockNumber
func (_m *MockSubstrateClient) GetBlockHash(blockNumber uint64) (types.Hash, error) {
	ret := _m.Called(blockNumber)

	var r0 types.Hash
	if rf, ok := ret.Get(0).(func(uint64) types.Hash); ok {
		r0 = rf(blockNumber)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(types.Hash)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(uint64) error); ok {
		r1 = rf(blockNumber)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetBlockHashLatest provides a mock function with given fields:
func (_m *MockSubstrateClient) GetBlockHashLatest() (types.Hash, error) {
	ret := _m.Called()

	var r0 types.Hash
	if rf, ok := ret.Get(0).(func() types.Hash); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(types.Hash)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetBlockHeaderByBlockNumbers provides a mock function with given fields: blockNumbers
func (_m *MockSubstrateClient) GetBlockHeaderByBlockNumbers(blockNumbers []types.BlockNumber) ([]types.Header, error) {
	ret := _m.Called(blockNumbers)

	var r0 []types.Header
	if rf, ok := ret.Get(0).(func([]types.BlockNumber) []types.Header); ok {
		r0 = rf(blockNumbers)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]types.Header)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func([]types.BlockNumber) error); ok {
		r1 = rf(blockNumbers)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetFinalizedHead provides a mock function with given fields:
func (_m *MockSubstrateClient) GetFinalizedHead() (types.Hash, error) {
	ret := _m.Called()

	var r0 types.Hash
	if rf, ok := ret.Get(0).(func() types.Hash); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(types.Hash)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetGrandpaCurrentSetId provides a mock function with given fields: blockHash
func (_m *MockSubstrateClient) GetGrandpaCurrentSetId(blockHash types.Hash) (types.U64, error) {
	ret := _m.Called(blockHash)

	var r0 types.U64
	if rf, ok := ret.Get(0).(func(types.Hash) types.U64); ok {
		r0 = rf(blockHash)
	} else {
		r0 = ret.Get(0).(types.U64)
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.Hash) error); ok {
		r1 = rf(blockHash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetHeader provides a mock function with given fields: hash
func (_m *MockSubstrateClient) GetHeader(hash types.Hash) (*types.Header, error) {
	ret := _m.Called(hash)

	var r0 *types.Header
	if rf, ok := ret.Get(0).(func(types.Hash) *types.Header); ok {
		r0 = rf(hash)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.Header)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.Hash) error); ok {
		r1 = rf(hash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetHeaderByBlockNumber provides a mock function with given fields: blockNumber
func (_m *MockSubstrateClient) GetHeaderByBlockNumber(blockNumber types.BlockNumber) (*types.Header, error) {
	ret := _m.Called(blockNumber)

	var r0 *types.Header
	if rf, ok := ret.Get(0).(func(types.BlockNumber) *types.Header); ok {
		r0 = rf(blockNumber)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.Header)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.BlockNumber) error); ok {
		r1 = rf(blockNumber)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetHeaderLatest provides a mock function with given fields:
func (_m *MockSubstrateClient) GetHeaderLatest() (*types.Header, error) {
	ret := _m.Called()

	var r0 *types.Header
	if rf, ok := ret.Get(0).(func() *types.Header); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.Header)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetJustificationsAndUnknownHeaders provides a mock function with given fields: blockNumber
func (_m *MockSubstrateClient) GetJustificationsAndUnknownHeaders(blockNumber types.BlockNumber) (*GrandpaJustification, []types.Header, error) {
	ret := _m.Called(blockNumber)

	var r0 *GrandpaJustification
	if rf, ok := ret.Get(0).(func(types.BlockNumber) *GrandpaJustification); ok {
		r0 = rf(blockNumber)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*GrandpaJustification)
		}
	}

	var r1 []types.Header
	if rf, ok := ret.Get(1).(func(types.BlockNumber) []types.Header); ok {
		r1 = rf(blockNumber)
	} else {
		if ret.Get(1) != nil {
			r1 = ret.Get(1).([]types.Header)
		}
	}

	var r2 error
	if rf, ok := ret.Get(2).(func(types.BlockNumber) error); ok {
		r2 = rf(blockNumber)
	} else {
		r2 = ret.Error(2)
	}

	return r0, r1, r2
}

// GetMetadata provides a mock function with given fields: blockHash
func (_m *MockSubstrateClient) GetMetadata(blockHash types.Hash) (*types.Metadata, error) {
	ret := _m.Called(blockHash)

	var r0 *types.Metadata
	if rf, ok := ret.Get(0).(func(types.Hash) *types.Metadata); ok {
		r0 = rf(blockHash)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.Metadata)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.Hash) error); ok {
		r1 = rf(blockHash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetMetadataLatest provides a mock function with given fields:
func (_m *MockSubstrateClient) GetMetadataLatest() *types.Metadata {
	ret := _m.Called()

	var r0 *types.Metadata
	if rf, ok := ret.Get(0).(func() *types.Metadata); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.Metadata)
		}
	}

	return r0
}

// GetReadProof provides a mock function with given fields: key, blockHash
func (_m *MockSubstrateClient) GetReadProof(key types.StorageKey, blockHash types.Hash) (SubstrateReadProof, error) {
	ret := _m.Called(key, blockHash)

	var r0 SubstrateReadProof
	if rf, ok := ret.Get(0).(func(types.StorageKey, types.Hash) SubstrateReadProof); ok {
		r0 = rf(key, blockHash)
	} else {
		r0 = ret.Get(0).(SubstrateReadProof)
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.StorageKey, types.Hash) error); ok {
		r1 = rf(key, blockHash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetSpecName provides a mock function with given fields:
func (_m *MockSubstrateClient) GetSpecName() string {
	ret := _m.Called()

	var r0 string
	if rf, ok := ret.Get(0).(func() string); ok {
		r0 = rf()
	} else {
		r0 = ret.Get(0).(string)
	}

	return r0
}

// GetStorageRaw provides a mock function with given fields: key, blockHash
func (_m *MockSubstrateClient) GetStorageRaw(key types.StorageKey, blockHash types.Hash) (*types.StorageDataRaw, error) {
	ret := _m.Called(key, blockHash)

	var r0 *types.StorageDataRaw
	if rf, ok := ret.Get(0).(func(types.StorageKey, types.Hash) *types.StorageDataRaw); ok {
		r0 = rf(key, blockHash)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*types.StorageDataRaw)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.StorageKey, types.Hash) error); ok {
		r1 = rf(key, blockHash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetSystemEventStorageKey provides a mock function with given fields: blockhash
func (_m *MockSubstrateClient) GetSystemEventStorageKey(blockhash types.Hash) (types.StorageKey, error) {
	ret := _m.Called(blockhash)

	var r0 types.StorageKey
	if rf, ok := ret.Get(0).(func(types.Hash) types.StorageKey); ok {
		r0 = rf(blockhash)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(types.StorageKey)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.Hash) error); ok {
		r1 = rf(blockhash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetValidationData provides a mock function with given fields: blockHash
func (_m *MockSubstrateClient) GetValidationData(blockHash types.Hash) (*PersistedValidationData, error) {
	ret := _m.Called(blockHash)

	var r0 *PersistedValidationData
	if rf, ok := ret.Get(0).(func(types.Hash) *PersistedValidationData); ok {
		r0 = rf(blockHash)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*PersistedValidationData)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(types.Hash) error); ok {
		r1 = rf(blockHash)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// SubcribeFinalizedHeadAt provides a mock function with given fields: height, cb
func (_m *MockSubstrateClient) SubcribeFinalizedHeadAt(height uint64, cb func(*types.Hash)) error {
	ret := _m.Called(height, cb)

	var r0 error
	if rf, ok := ret.Get(0).(func(uint64, func(*types.Hash)) error); ok {
		r0 = rf(height, cb)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}
