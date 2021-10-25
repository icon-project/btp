package substrate

import (
	"strings"

	"github.com/itering/scale.go/types"
	"github.com/itering/substrate-api-rpc/hasher"
	"github.com/itering/substrate-api-rpc/util"
)

type StorageKey struct {
	EncodeKey string
	ScaleType string
}

type Storage struct {
	Prefix string
	Method string
	Type   types.StorageType
}

func EncodeStorageKey(m *types.MetadataStruct, section, method string, args ...string) (storageKey StorageKey) {
	if m == nil {
		return
	}

	method = upperCamel(method)
	prefix, storageType := moduleStorageMapType(m, section, method)
	if storageType == nil {
		return
	}

	mapType := checkoutHasherAndType(storageType)
	if mapType == nil {
		return
	}

	storageKey.ScaleType = mapType.Value

	var hash []byte
	sectionHash := hasher.HashByCryptoName([]byte(upperCamel(prefix)), "Twox128")
	methodHash := hasher.HashByCryptoName([]byte(method), "Twox128")

	hash = append(sectionHash, methodHash[:]...)

	if len(args) > 0 {
		var param []byte
		param = append(param, hasher.HashByCryptoName(util.HexToBytes(args[0]), mapType.Hasher)...)
		if len(args) == 2 {
			param = append(param, hasher.HashByCryptoName(util.HexToBytes(args[1]), mapType.Hasher2)...)
		}
		hash = append(hash, param[:]...)
	}
	storageKey.EncodeKey = util.BytesToHex(hash)
	return
}

type storageOption struct {
	Value    string `json:"value"`
	Hasher   string `json:"hasher"`
	Hasher2  string `json:"hasher_2"`
	IsLinked bool   `json:"is_linked"`
}

func checkoutHasherAndType(t *types.StorageType) *storageOption {
	option := storageOption{}
	switch t.Origin {
	case "MapType":
		option.Value = t.MapType.Value
		option.Hasher = t.MapType.Hasher
	case "DoubleMapType":
		option.Value = t.DoubleMapType.Value
		option.Hasher = t.DoubleMapType.Hasher
		option.Hasher2 = t.DoubleMapType.Key2Hasher
		option.IsLinked = t.DoubleMapType.IsLinked
	case "Map":
		option.Value = t.NMapType.Value
		if len(t.NMapType.Hashers) == 1 {
			option.Hasher = t.NMapType.Hashers[0]
		}
		if len(t.NMapType.Hashers) == 2 {
			option.Hasher2 = t.NMapType.Hashers[1]
		}
	default:
		option.Value = *t.PlainType
		option.Hasher = "Twox64Concat"
	}
	return &option
}

func upperCamel(s string) string {
	if len(s) == 0 {
		return ""
	}
	s = strings.ToUpper(string(s[0])) + string(s[1:])
	return s
}

func moduleStorageMapType(m *types.MetadataStruct, section, method string) (string, *types.StorageType) {
	modules := m.Metadata.Modules
	for _, value := range modules {
		if strings.EqualFold(strings.ToLower(value.Name), strings.ToLower(section)) {
			for _, storage := range value.Storage {
				if strings.EqualFold(strings.ToLower(storage.Name), strings.ToLower(method)) {
					return value.Prefix, &storage.Type
				}
			}
		}
	}
	return "", nil
}
