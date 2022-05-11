package testdata

import "github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"

type GetBlockTest struct {
	description string
	testData    []TestData
}

func (t GetBlockTest) Description() string {
	return t.description
}

func (t GetBlockTest) TestDatas() []TestData {
	return t.testData
}

func init() {

	var testData = []TestData{
		{
			Description: "GetBlock Success",
			Input:       377825,
			MockStorage: func() mock.Storage {
				blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826"})

				return mock.Storage{
					BlockByHeightMap: blockByHeightMap,
					BlockByHashMap:   blockByHashMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: "DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c",
			},
		},
	}

	RegisterTest("GetBlock", GetBlockTest{
		description: "Test_GetBlock",
		testData:    testData,
	})
}
