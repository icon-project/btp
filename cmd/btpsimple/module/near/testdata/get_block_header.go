package testdata

import (
	"encoding/hex"

	"github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"
)

type GetBlockHeaderTest struct {
	description string
	testData    []TestData
}

func (t GetBlockHeaderTest) Description() string {
	return t.description
}

func (t GetBlockHeaderTest) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Input:       377825,
			Description: "GetBlockHeader Pass",
			MockStorage: func() mock.Storage {
				blockHeightMap, blockHashMap := mock.LoadBlockFromFile([]string{"377825"})
				return mock.Storage{
					BlockByHashMap:   blockHashMap,
					BlockByHeightMap: blockHeightMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: func() []byte {
					decoded, _ := hex.DecodeString("f9034aa069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4f8d38305c3e1a0dd4bfddd18ff0547be029ee48eb24fc57e535611f9f6c5dac1d19697354bc96da069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a0b08318984fd7d7d5941b59054cb6714644c04382b9f42a0a543d3e629f2f78bca066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f29258816bcde70d4266bb8a001213e665fbb68d07df6fbb5267633572e263dff5e24ff10c75dcc61697f27f7a09661baf8410ee1cbf18c7088ce8bf336b5424b3ec65492fac3c3caa2b79fdfbdf9020ea07a4fde36a426deaf5e1252f5718a06e1787f8d12cb7da5a18057fa9ffb66ce0da0e176325dca56b62ae276b02d75ee33852dc02994ed89d5680ccb3a7d6104c185a066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925a00000000000000000000000000000000000000000000000000000000000000000a0364a9fd4f9c1b47eff8c396fd4d8b11aee33316db63af69f66a6c426676b7580c0c1018a31303030303030303030a235303033363733373934313438383837343734343037373030303032323832303332c0a09e96fdaabe64d3bfa8dd6baa10b748c62fc345e64dcbe53a80ee64058de99168a069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa48305c3cf8305c3e0a07d5b2dc3acbed7a1d0f8acf22b3f33279e8bc5992fbbfd26c3267631d22813cff8c9b841008aef52c0d4aab730c4cd445aeab8aa8c6c1a59c4aab9456d1f37b79f97669cfb0d096587a5ba3b48b627598fddc32abdcd410774151d1558cfb8ca5faabf5504b841009d780665262c57f51bc5ccd04ede8ab919412f59c530226463ac4ddd58bf52110d5be05ecfed6a81d5134ad556a0e7f66242d02c125d223717e66df3c8a9c10ab84100940a26097e356678adb83e4123ad15c45243814774190723292973be81965c7a97b9aaa86038b94e6a164f357423b9c4f850c48670caefe8a4277d8d78410f0631b84100ce360279a56b18c5ec6d7783bf209526370e60d57278f649bb2e6d0c6e54fa6d7ec9123ec198d862d537e1a6107a953c178ccdbee42a465356d9792dec3d4608")
					return decoded
				}(),
			},
		},
		{
			Input:       77768563,
			Description: "GetBlockHeader Fail",
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: nil,
			},
		},
	}

	RegisterTest("GetBlockHeader", GetBlockHeaderTest{
		description: "Test GetBlockHeader",
		testData:    testData,
	})
}
