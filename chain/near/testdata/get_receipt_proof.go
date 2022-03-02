package testdata

import "github.com/icon-project/btp/chain/near/testdata/mock"

type GetReceiptProofsTest struct {
	description string
	testData    []TestData
}

func (t GetReceiptProofsTest) Description() string {
	return t.description
}

func (t GetReceiptProofsTest) TestDatas() []TestData {
	return t.testData
}

func init() {
	var testData = []TestData{
		{
			Description: "GetReceiptProof Success",
			Input:       377825,
			MockStorage: func() mock.Storage {
				blockHeightMap, blockHashMap := mock.LoadBlockFromFile([]string{"377825", "377826"})
				receiptProofMap := mock.LoadReceiptsFromFile([]string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G", "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y"})

				return mock.Storage{
					BlockByHashMap:   blockHashMap,
					BlockByHeightMap: blockHeightMap,
					ReceiptProofMap:  receiptProofMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: []string{
					`{"Height":377825,"Index":0,"Proof":null,"EventProofs":[{"Index":0,"Proof":"+QFC+O34UOegtjBNfUgE6JqmVVwDUjNuFjGyRrdc/YV5k4Ot8grcyWWFUmlnaHTnoNMqHJym7V3VjmhvttOyAuagW8Kk4G3ch/MkLB2oJzv4hVJpZ2h0oL6mAt4FNk42I9iacgJ30SM7tdW1gZA1TgoplsjadETOoBYpT1e3u+vGfWiA8jTwoypjPli90Zs32ppIpkDkHF+T+FfA4aDfsE0/yC2q71NLc33jR3oq2yJCGfDproqaqtUC8oX25YYF5ePQJreVNjQ4NDkyNzcxOTA5NTAwMDAwMDAwi2FsaWNlLm5vZGUxyoD4AMXEAMLBgID4UOegZmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSWFUmlnaHTnoC7rdKYXf1iNgMDHUrmVVpAt35aC0LkG9aoq26+EZqTphVJpZ2h0wA=="},{"Index":0,"Proof":"+QFB+Oz4T+agDWDwK1Fjjc00pUFtUC3A5On5WR0xpwGY32feq8KjhGmETGVmdOeg0yocnKbtXdWOaG+207IC5qBbwqTgbdyH8yQsHagnO/iFUmlnaHSgvqYC3gU2TjYj2JpyAnfRIzu11bWBkDVOCimWyNp0RM6gI7Z4htAHgtCPZarmSiruA9ZzNFmCNDvi4/tyOyKM69b4V8DhoN0bmH6VJyfIuZCTJLO5DRkcqAkqsh3AFew//igrh337hgRJqftVj5U0NzE0NDMwOTQ0NjU1MDAwMDAwMDCLYWxpY2Uubm9kZTHKgPgAxcQAwsGAgPhQ56BmaHqt+GK9d2yPwYuOn44gCJcUhW7iM7OQKlkdDV8pJYVSaWdodOegLut0phd/WI2AwMdSuZVWkC3floLQuQb1qirbr4RmpOmFUmlnaHTA"}],"Events":[{"Next":"btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b","Sequence":0,"Message":"K01tYVluUndPaTh2TUhneExtNWxZWEl2WVd4cFkyVXVibTlrWlRHNE9XSjBjRG92THpCNE1TNXBZMjl1TDJONE9EZGxaRGt3TkRoaU5UazBZamsxTVRrNVpqTXlObVpqTnpabE56WmhPV1F6TTJSa05qWTFZb05pYldPQmdMaHIrR2tWdUdaQ1RVTlNaWFpsY25SVmJuSmxZV05vWVdKc1pTQmhkQ0JpZEhBNkx5OHdlRFV1Y0hKaEx6ZzRZbVF3TlRRME1qWTRObUpsTUdFMVpHWTNaR0V6TTJJMlpqRXdPRGxsWW1abFlUTTNOamxpTVRsa1ltSXlORGMzWm1Vd1kyUTJaVEJtTVRJMlpUUT0="},{"Next":"btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b","Sequence":0,"Message":"K01tYVluUndPaTh2TUhneExtNWxZWEl2WVd4cFkyVXVibTlrWlRHNE9XSjBjRG92THpCNE1TNXBZMjl1TDJONE9EZGxaRGt3TkRoaU5UazBZamsxTVRrNVpqTXlObVpqTnpabE56WmhPV1F6TTJSa05qWTFZb05pYldPQmdMaHIrR2tWdUdaQ1RVTlNaWFpsY25SVmJuSmxZV05vWVdKc1pTQmhkQ0JpZEhBNkx5OHdlRFV1Y0hKaEx6ZzRZbVF3TlRRME1qWTRObUpsTUdFMVpHWTNaR0V6TTJJMlpqRXdPRGxsWW1abFlUTTNOamxpTVRsa1ltSXlORGMzWm1Vd1kyUTJaVEJtTVRJMlpUUT0="}]}`,
				},
			},
		},
	}

	RegisterTest("GetReceiptProof", GetBlockProofTest{
		description: "Test GetReceiptProof",
		testData:    testData,
	})
}
