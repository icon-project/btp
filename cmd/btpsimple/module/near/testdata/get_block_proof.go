package testdata

import (
	"encoding/hex"

	"github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"
)

type GetBlockProofTest struct {
	description string
	testData    []TestData
}

func (t GetBlockProofTest) Description() string {
	return t.description
}

func (t GetBlockProofTest) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Description: "GetBlockProof Success",
			Input:       int64(377826),
			MockStorage: func() mock.Storage {
				blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828"})
				lightClientBlockMap := mock.LoadLightClientBlocksFromFile([]string{"DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c", "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w", "9HhNTVZBu7sBFFsn4dCFnJehVXvMUJ8TDLXDffRJa1hz", "4CSFBudwkUAgoHHQNh6UnVx78EP9ubeUhbc9ZHoC5w4u"})

				return mock.Storage{
					BlockByHeightMap:    blockByHeightMap,
					BlockByHashMap:      blockByHashMap,
					LightClientBlockMap: lightClientBlockMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: func() []byte {
					decoded, _ := hex.DecodeString("f904b5f9032ba0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c6987f8d38305c3e2a0dd4bfddd18ff0547be029ee48eb24fc57e535611f9f6c5dac1d19697354bc96da069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a094712793fc2575fd39fbedd43fe0a7319040d15c747fdff10ee40371eb3edd13a066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f29258816bcde70f77ecdb7a001213e665fbb68d07df6fbb5267633572e263dff5e24ff10c75dcc61697f27f7a015c8be8d2f6acd34f24b6d08049bd868113dbd20bbd298b6759f71773ee1f171f901efa07a4fde36a426deaf5e1252f5718a06e1787f8d12cb7da5a18057fa9ffb66ce0da099ed696060a422df2eda0c2db8c1151f308381f24f9c6f97b50e3fa7d6c7810ca066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925a00000000000000000000000000000000000000000000000000000000000000000a084e391895fdb2976acfc814a8f105b350bac500fa69dd372143d087dbde132c7c0c1018a31303030303030303030a235303033363733373934313438383837343734343037373030303032323832303332c0a069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c69878305c3d08305c3e1f800f8c9b841007bba48efa26e408cf80a2f55ff3961581866a8827b3a59b9022a0c1a24ea9288c8adc649d498123dae2a028ef8072bf61d00faee9300ac63293736ba7b314203b84100cae2e0262d7847a536d5ea00043d7423be38523fea104434cdf9fee47e134e362dfc4df09d2b186f0bf9a5c404fcfd3cf1d4fae3e82df71cc5d38696673c8a00b84100486035608e5cef6820b6cf020faa6f573ff5344bab146c572872c4142168ca81d4f5986daf59394b03c7035725e744e4877f16accedd2319722ed399521b380f31b841008edc3a524ba0b31fe08452f55b83ad3cb69d7fa3182add3adbeec561abe3d48fed621ccbc6c8cb140ee42a30023a1a8ca1051531fc71e4b37720e02cd55ae809a0d77acc1ec437d51770447cecb8cb6a12162bf8c8e22f1199cace77485f0b45d9f8c9b841004699db96aaee29cd51db1af04f25e1f860709510454df6796f34e9a1be107a54b6986d10598f48f6e0851b6d11225d5647a1fbfd140d5f6632eec794fa3b4205b8410077890a91da83841829a08f873507ec62c23b3999de43fe4bceca474686556c9245a52da59e25a4c531ee6359aba8b094706054d03c70d0d9031deb7ae275de04b84100fc48f0c577ed81f13341330b4291f1c27fe6050b7040da38a5e4052a21a18a650bfecc0a15f0c7df02ced415a492cb02e0a9c5a536b2ae71cfa728f243b3f608e400a0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c69870000e700a07b246f4ee2f28a4dc4f69407bfa3136c03f39a80e87bd6bfe65a98fa9b416e01008305c3e4f84cf84a00856e6f646532a100e9f1e1f48f966f3cb086e238c8f2d9d6a54063a8e757cf5587bf429a5d2292c2a03530393032333836373536323633333238303330323339373139303839313132")
					return decoded
				}(),
			},
		},
	}
	RegisterTest("GetBlockProof", GetBlockProofTest{
		description: "Test GetBlockProof",
		testData:    testData,
	})
}
