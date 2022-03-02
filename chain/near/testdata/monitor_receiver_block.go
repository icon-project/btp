package testdata

import "github.com/icon-project/btp/chain/near/testdata/mock"

type MonitorReciverBlock struct {
	description string
	testData    []TestData
}

func (t MonitorReciverBlock) Description() string {
	return t.description
}

func (t MonitorReciverBlock) TestDatas() []TestData {
	return t.testData
}

func init() {
	source := "btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"
	destination := "btp://0x1.near/dev-20211206025826-24100687319598"
	blockId := "377825"

	var testData = []TestData{
		{
			Description: "MonitorReciverBlock Succes",
			Input:       []string{source, destination, blockId},
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
				Success: "84toXNMo2p5ttdjkV6RHdJFrgxrnTLRkCTjb7aA8Dh95",
			},
		},
	}

	RegisterTest("MontorReceiverBlock", MonitorReciverBlock{
		description: "Test MonitorReciverBlock",
		testData:    testData,
	})
}
