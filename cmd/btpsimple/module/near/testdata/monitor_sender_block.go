package testdata

import "github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"

type MonitorSenderBlock struct {
	description string
	testData    []TestData
}

func (t MonitorSenderBlock) Description() string {
	return t.description
}

func (t MonitorSenderBlock) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Description: "MonitorSenderBlock Success",
			Input:       377825,
			MockStorage: func() mock.Storage {
				blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828"})
				latestBlockHeight := mock.Response{
					Reponse: 377828,
				}

				return mock.Storage{
					BlockByHeightMap:  blockByHeightMap,
					BlockByHashMap:    blockByHashMap,
					LatestBlockHeight: latestBlockHeight,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: "DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c",
				Fail:    nil,
			},
		},
	}

	RegisterTest("MonitorSenderBlock", MonitorSenderBlock{
		description: "Test MonitorSenderBlock",
		testData:    testData,
	})
}
