package testdata

import (
	"github.com/btcsuite/btcutil/base58"
	"github.com/icon-project/btp/chain/near/testdata/mock"
)

type ComputeBlockHashTest struct {
	description string
	testData    []TestData
}

func (t ComputeBlockHashTest) Description() string {
	return t.description
}

func (t ComputeBlockHashTest) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Input:       377825,
			Description: "ComputeBlockHash Pass",
			MockStorage: func() mock.Storage {
				blockHeightMap, blockHashMap := mock.LoadBlockFromFile([]string{"377825_override"})
				return mock.Storage{
					BlockByHashMap:   blockHashMap,
					BlockByHeightMap: blockHeightMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: base58.Decode("DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c"),
			},
		},
	}

	RegisterTest("ComputeBlockHash", ComputeBlockHashTest{
		description: "Test ComputeBlockHash",
		testData:    testData,
	})
}
