package testdata

type GetNonceTest struct {
	description string
	testData    []TestData
}

func (t GetNonceTest) Description() string {
	return t.description
}

func (t GetNonceTest) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Description: "GetNonce Pass",
			Input:       []string{"B1FqHyNFqZvKXzTU379a83UTVMMuB6VegeQgvA7vsMn7", "94a5a3fc9bc948a7f4b1c6210518b4afe1744ebe33188eb91d17c863dfe200a8"},
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: func() int64 {
					return 51
				}(),
			},
		},
		{
			Input:       []string{"B1FqHyNFqZvKXzTU379a83UTVMMuB6VegeQgvA7vsMn7", "4a5a3fc9bc948a7f4b1c6210518b4afe1744ebe33188eb91d17c863dfe200a8"},
			Description: "GetNonce Fail",
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: nil,
			},
		},
	}

	RegisterTest("GetNonce", GetNonceTest{
		description: "Test GetNonce",
		testData:    testData,
	})
}
