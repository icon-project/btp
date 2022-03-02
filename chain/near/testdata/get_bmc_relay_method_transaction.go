package testdata

import (
	"crypto/ed25519"

	"github.com/btcsuite/btcutil/base58"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/base"
	"github.com/icon-project/btp/common/wallet"
)

type BmcRelayMethod struct {
	description string
	testData    []TestData
}

func (t BmcRelayMethod) Description() string {
	return t.description
}

func (t BmcRelayMethod) TestDatas() []TestData {
	return t.testData
}

type TransactionParams struct {
	Wallet       base.Wallet
	Source       chain.BtpAddress
	Destination  chain.BtpAddress
	RelayMessage string
}

func init() {
	privateKey := base58.Decode("22yx6AjQgG1jGuAmPuEwLnVKFnuq5LU23dbU3JBZodKxrJ8dmmqpDZKtRSfiU4F8UQmv1RiZSrjWhQMQC3ye7M1J")
	newPrivateKey := ed25519.PrivateKey(privateKey)
	nearWallet, _ := wallet.NewNearwalletFromPrivateKey(&newPrivateKey)

	var testData = []TestData{
		{
			Description: "GetBmcRelayMethodParam Success",
			Input: TransactionParams{
				Wallet:       base.Wallet(nearWallet),
				Source:       chain.BtpAddress("btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"),
				Destination:  chain.BtpAddress("btp://0x1.near/dev-20211206025826-24100687319598"),
				RelayMessage: `{"BlockUpdates":[null],"BlockProof":null,"ReceiptProofs":["+QNXAAC5AVT5AVG5AU75AUuCIAC5AUX5AUIAlQGUOSjrdmt9zBgzZiSMgiJ/qM/HVoMHUmSDB1JkhQLpDt0AuPYQAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAIEAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAQAAAgAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAgAAAAACAAAABCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAgAAAAAAAAAAAAAAAEAAAAAAAAAEAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAD4APgAoLxFgII4jHbtKVAMb0HLq0dDlTRRJj+Wc4TTJKSFcKj6+QH5+QH2ALkB8vkB76PiEKDDnrbv2QvmCub4AHu1LDXgaLr4mhnj/VUIJdSQ2O3UYLhT+FGgY0yLtFgQsdVBH/DAas4m1MgJBTbJmkN4J3sCMeyyJfegnoEXGMJ6O/ujWWpyG7oguKP/dzlWyxPlAchEU4wBxb+AgICAgICAgICAgICAgIC5AXP5AXAguQFs+QFplQGcByvvVFq+uWo/e6L/34uGH7Vd6vhUlk1lc3NhZ2Uoc3RyLGludCxieXRlcym4OmJ0cDovLzB4NTAxLnByYS8weDVDQzMwNzI2OGExMzkzQUI5QTc2NEEyMERBQ0U4NDhBQjgyNzVjNDYC+Pu4+fj3uD5idHA6Ly8weDU4ZWIxYy5pY29uL2N4OWMwNzJiZWY1NDVhYmViOTZhM2Y3YmEyZmZkZjhiODYxZmI1NWRlYbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NopuYXRpdmVjb2luAbht+GsAuGj4ZqpoeDQ1MDJhYWQ3OTg2YWQ1YTg0ODk1NTE1ZmFmNzZlOTBiNWI0Nzg2NTSqMHgxNThBMzkxRjM1MDBDMzI4OEFiMjg2NTM3MjJhNjQ1OUU3NzI2QjAxz86DSUNYiQCJY92MLF4AAPgA"]}`,
			},
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Success: nearWallet.Address(),
			},
		},
		{
			Description: "GetBmcRelayMethodParam Fail",
			Input: TransactionParams{
				Wallet:       base.Wallet(nil),
				Source:       chain.BtpAddress("btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"),
				Destination:  chain.BtpAddress("btp://0x1.near/dev-20211206025826-24100687319598"),
				RelayMessage: `{"BlockUpdates":[null],"BlockProof":null,"ReceiptProofs":["+QNXAAC5AVT5AVG5AU75AUuCIAC5AUX5AUIAlQGUOSjrdmt9zBgzZiSMgiJ/qM/HVoMHUmSDB1JkhQLpDt0AuPYQAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAIEAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAQAAAgAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAgAAAAACAAAABCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAgAAAAAAAAAAAAAAAEAAAAAAAAAEAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAD4APgAoLxFgII4jHbtKVAMb0HLq0dDlTRRJj+Wc4TTJKSFcKj6+QH5+QH2ALkB8vkB76PiEKDDnrbv2QvmCub4AHu1LDXgaLr4mhnj/VUIJdSQ2O3UYLhT+FGgY0yLtFgQsdVBH/DAas4m1MgJBTbJmkN4J3sCMeyyJfegnoEXGMJ6O/ujWWpyG7oguKP/dzlWyxPlAchEU4wBxb+AgICAgICAgICAgICAgIC5AXP5AXAguQFs+QFplQGcByvvVFq+uWo/e6L/34uGH7Vd6vhUlk1lc3NhZ2Uoc3RyLGludCxieXRlcym4OmJ0cDovLzB4NTAxLnByYS8weDVDQzMwNzI2OGExMzkzQUI5QTc2NEEyMERBQ0U4NDhBQjgyNzVjNDYC+Pu4+fj3uD5idHA6Ly8weDU4ZWIxYy5pY29uL2N4OWMwNzJiZWY1NDVhYmViOTZhM2Y3YmEyZmZkZjhiODYxZmI1NWRlYbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NopuYXRpdmVjb2luAbht+GsAuGj4ZqpoeDQ1MDJhYWQ3OTg2YWQ1YTg0ODk1NTE1ZmFmNzZlOTBiNWI0Nzg2NTSqMHgxNThBMzkxRjM1MDBDMzI4OEFiMjg2NTM3MjJhNjQ1OUU3NzI2QjAxz86DSUNYiQCJY92MLF4AAPgA"]}`,
			},
			Expected: struct {
				Success interface{}
				Fail    interface{}
			}{
				Fail: "falied cast the parameters",
			},
		},
	}

	RegisterTest("GetBmcRelayMethod", BmcRelayMethod{
		description: "Test GetBmcRelayMethodParam",
		testData:    testData,
	})
}
