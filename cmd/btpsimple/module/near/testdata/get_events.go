package testdata

type GetEvents struct {
	description string
	testData    []TestData
}

func (t GetEvents) Description() string {
	return t.description
}

func (t GetEvents) TestDatas() []TestData {
	return t.testData
}

type Eventsreposnse struct {
	Events      []string
	RecieptHash []string
}

func init() {
	var testData = []TestData{
		{
			Description: "Get Events Success",
			Input:       []string{"alice.node0", "377825"},
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: Eventsreposnse{
					Events: []string{
						`{"Next":"btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b","Sequence":0,"Message":"K01tYVluUndPaTh2TUhneExtNWxZWEl2WVd4cFkyVXVibTlrWlRHNE9XSjBjRG92THpCNE1TNXBZMjl1TDJONE9EZGxaRGt3TkRoaU5UazBZamsxTVRrNVpqTXlObVpqTnpabE56WmhPV1F6TTJSa05qWTFZb05pYldPQmdMaHIrR2tWdUdaQ1RVTlNaWFpsY25SVmJuSmxZV05vWVdKc1pTQmhkQ0JpZEhBNkx5OHdlRFV1Y0hKaEx6ZzRZbVF3TlRRME1qWTRObUpsTUdFMVpHWTNaR0V6TTJJMlpqRXdPRGxsWW1abFlUTTNOamxpTVRsa1ltSXlORGMzWm1Vd1kyUTJaVEJtTVRJMlpUUT0="}`,
						`{"Next":"btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b","Sequence":0,"Message":"K01tYVluUndPaTh2TUhneExtNWxZWEl2WVd4cFkyVXVibTlrWlRHNE9XSjBjRG92THpCNE1TNXBZMjl1TDJONE9EZGxaRGt3TkRoaU5UazBZamsxTVRrNVpqTXlObVpqTnpabE56WmhPV1F6TTJSa05qWTFZb05pYldPQmdMaHIrR2tWdUdaQ1RVTlNaWFpsY25SVmJuSmxZV05vWVdKc1pTQmhkQ0JpZEhBNkx5OHdlRFV1Y0hKaEx6ZzRZbVF3TlRRME1qWTRObUpsTUdFMVpHWTNaR0V6TTJJMlpqRXdPRGxsWW1abFlUTTNOamxpTVRsa1ltSXlORGMzWm1Vd1kyUTJaVEJtTVRJMlpUUT0="}`,
					},
					RecieptHash: []string{
						"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
						"3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
					},
				},
				Fail: nil,
			},
		},
	}

	RegisterTest("GetEvents", GetEvents{
		description: "Test_GetEvents",
		testData:    testData,
	})
}
