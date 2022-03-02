package testdata

import "github.com/icon-project/btp/chain/near/testdata/mock"

type GetTransactionResultTest struct {
	description string
	testData    []TestData
}

func (t GetTransactionResultTest) Description() string {
	return t.description
}

func (t GetTransactionResultTest) TestDatas() []TestData {
	return t.testData
}

func init() {

	var testData = []TestData{
		{
			Description: "GetTransaction Result Success",
			Input: Input{
				Hash:   "6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm",
				Wallet: Wallet("22yx6AjQgG1jGuAmPuEwLnVKFnuq5LU23dbU3JBZodKxrJ8dmmqpDZKtRSfiU4F8UQmv1RiZSrjWhQMQC3ye7M1J"),
			},
			MockStorage: func() mock.Storage {
				transactionResultMap := mock.LoadTransactionResultFromFile([]string{"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm", "CotTthJuKeUiEPuRG2SuZaEHw28adZGbkCJsSdnN1cMt"})
				return mock.Storage{
					TransactionResultMap: transactionResultMap,
				}
			}(),
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: "6K8SzhwtzZ6wrrTmbKcNCfvqVW5xuEze6yFZBMeWh3a7",
			},
		},
	}
	RegisterTest("GetTransactionResult", GetTransactionResultTest{
		description: "Test GetTransactionResult",
		testData:    testData,
	})
}
