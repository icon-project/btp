package testdata

import "github.com/icon-project/btp/cmd/btpsimple/module"

type GetBmcLinkStatus struct {
	description string
	testData    []TestData
}

func (t GetBmcLinkStatus) Description() string {
	return t.description
}

func (t GetBmcLinkStatus) TestDatas() []TestData {
	return t.testData
}

func init() {
	source := module.BtpAddress("btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5")
	destination := module.BtpAddress("btp://0x1.near/dev-20211206025826-24100687319598")
	var testData = []TestData{
		{
			Description: "GetBmcStatus Sucess",
			Input:       []module.BtpAddress{destination, source},
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: 73935506,
				Fail:    nil,
			},
		},
	}

	RegisterTest("GetBmcLinkStatus", GetBmcLinkStatus{
		description: "Test_GetBmcStatus",
		testData:    testData,
	})
}
