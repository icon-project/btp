package testdata

import "github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"

type MonitorBlockHeight struct {
	description string
	testData    []TestData
}

func (t MonitorBlockHeight) Description() string {
	return t.description
}

func (t MonitorBlockHeight) TestDatas() []TestData {
	return t.testData
}

func init() {

	var testData = []TestData{

		{
			Description: "MonitorBlockHeight Success",
			Input:       377825,
			MockStorage: func() mock.Storage {
				blockHeightMap, _ := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828"})

				return mock.Storage{
					BlockByHeightMap: blockHeightMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: []int64{377825, 377826, 377827, 377828},
				Fail:    nil,
			},
		},
		{
			Description: "MonitorBlockHeight Fail",
			Input:       377825,
			MockStorage: func() mock.Storage {
				blockHeightMap, _ := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828"})

				return mock.Storage{
					BlockByHeightMap: blockHeightMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: nil,
				Fail:    []int64{377827, 377829, 377825, 377826},
			},
		},
	}

	RegisterTest("MonitorBlockHeight", MonitorBlockHeight{
		description: "Test_Monitor_Block_Height",
		testData:    testData,
	})
}
