package near

import (
	"crypto/ed25519"
	"encoding/base64"
	"encoding/json"
	"testing"

	"github.com/btcsuite/btcutil/base58"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/base"
	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/chain/near/testdata/mock"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"github.com/stretchr/testify/assert"
)

func Test_Monitor(t *testing.T) {
	t.Run("Test MonitorLoop", func(f *testing.T) {
		mockApi := NewMockApi(func() mock.Storage {
			blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828", "377829", "377830", "377831"})
			receiptProofMap := mock.LoadReceiptsFromFile([]string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G", "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y"})
			nonceMap := mock.LoadNonceFromFile([]string{"69c003c3b80ed12ea02f5c67c9e8167f0ce3b2e8020a0f43b1029c4d787b0d21"})
			return mock.Storage{
				BlockByHeightMap: blockByHeightMap,
				BlockByHashMap:   blockByHashMap,
				LatestBlockHeight: mock.Response{
					Reponse: 377831,
				},
				ReceiptProofMap: receiptProofMap,
				NonceMap:        nonceMap,
			}
		}())
		client := &Client{
			api: &mockApi,
		}
		base.RegisterClients([]string{"near"}, client)

		nearClient, err := base.GetClient("near")
		assert.NoError(f, err)
		logger := log.New()
		endpoint := ""
		source := chain.BtpAddress("btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5")
		destination := chain.BtpAddress("btp://0x1.near/dev-20211206025826-24100687319598")

		privateKeyBytes := base58.Decode("22yx6AjQgG1jGuAmPuEwLnVKFnuq5LU23dbU3JBZodKxrJ8dmmqpDZKtRSfiU4F8UQmv1RiZSrjWhQMQC3ye7M1J")
		privateKey := ed25519.PrivateKey(privateKeyBytes)
		nearWallet, err := wallet.NewNearwalletFromPrivateKey(&privateKey)

		assert.NoError(f, err)
		wallet := base.Wallet(nearWallet)
		sender := base.NewSender(source, destination, wallet, endpoint, nil, logger, nearClient)
		err = sender.MonitorLoop(377825, func(height int64) error {
			if height == 377827 {

				sender.StopMonitorLoop()
			}

			return nil
		}, func() {})
		assert.NoError(f, err)
	})

	t.Run("Test_Sender", func(f *testing.T) {
		mockApi := NewMockApi(func() mock.Storage {
			blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828", "377829", "377830", "377831"})
			receiptProofMap := mock.LoadReceiptsFromFile([]string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G", "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y"})
			nonceMap := mock.LoadNonceFromFile([]string{"69c003c3b80ed12ea02f5c67c9e8167f0ce3b2e8020a0f43b1029c4d787b0d21"})
			transactionMap := mock.LoadTransactionResultFromFile([]string{"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm"})
			return mock.Storage{
				BlockByHeightMap: blockByHeightMap,
				BlockByHashMap:   blockByHashMap,
				LatestBlockHeight: mock.Response{
					Reponse: 377831,
				},
				ReceiptProofMap:      receiptProofMap,
				NonceMap:             nonceMap,
				TransactionResultMap: transactionMap,
			}
		}())
		client := &Client{
			api: &mockApi,
		}

		base.RegisterClients([]string{"near"}, client)

		nearCleint, err := base.GetClient("near")
		assert.NoError(f, err)
		logger := log.New()
		endpoint := ""

		source := chain.BtpAddress("btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5")
		destination := chain.BtpAddress("btp://0x1.near/dev-20211206025826-24100687319598")
		privateKeyBytes := base58.Decode("22yx6AjQgG1jGuAmPuEwLnVKFnuq5LU23dbU3JBZodKxrJ8dmmqpDZKtRSfiU4F8UQmv1RiZSrjWhQMQC3ye7M1J")
		privateKey := ed25519.PrivateKey(privateKeyBytes)

		nearWallet, err := wallet.NewNearwalletFromPrivateKey(&privateKey)
		assert.NoError(f, err)
		wallet := base.Wallet(nearWallet)
		sender := base.NewSender(source, destination, wallet, endpoint, nil, logger, nearCleint)

		bmcStatus, err := sender.GetStatus()
		assert.NoError(f, err)
		relay_message := "-Qee-QQ8uQFS-QFPuNT40gKCQKOHBdCqJx0eQ5UAtrV5G-C172cGOzwQuED7gVFNsv2g1tINkHhbtViQXPruQ-FnkmYs_jA0tYrJKbvq3BYO9Rqgt12iGPeeEGlPxvT_hNLnhr9K5nUqvKiB6A_Yc0bfl76g7Z5kTlmy_2VEb189fXfCeFj6z4rrO5aUcNdJnHn5dXz4APgAgLhG-ESgMpG4jtB7-p0Ls3k2EYmLp9RerPVrvKAFveS23xQl2UT4AKBhUsXohogqHXqYEfjsfRAHCiSbbzQfM_H7NFRDIoxzKLh1-HMA4gGgtVIyurXEb0cIjT3frYFFTMfDQ48iwgntObJ1ht6v-RL4TfhLhwXQqicsVs-4QfNbBjk7e16wLiu54BjoVhrFrH41obLbpuP8tVCxRbVLHYoBQyGbXYOYQduPi9KL_jc9iJ284R8Z5uKMJYJvntwB-AC5AVD5AU240vjQAoJApIcF0KonLFbPlQC2tXkb4LXvZwY7PBC4QPuBUU2y_aB_WlGDMMOOnc25Zw71Asy9qm5P1ckuxut4n7eyRa8sX6BkLQ2BfmajCXpSPKQyPbQfknSBaVE5Jh3D-Wyd1gODu6DtnmROWbL_ZURvXz19d8J4WPrPius7lpRw10mcefl1fPgAoNBv8fj4bmAFfa9a8IyYHOK_pHPZt7lJi8AF0Cgkad7_gKbloDKRuI7Qe_qdC7N5NhGJi6fUXqz1a7ygBb3ktt8UJdlE-AD4ALh1-HMA4gGgqw1QRCuG0tcqzCpKDqhHQO09T3_OQMhOHAAgt-D4O-_4TfhLhwXQqic7qGy4QZPydQXTosV0Bw1Q3ntf7-FCxRDMSM7FOnPhP591OxiYUlsWMdK0qfbp-WrnfBJKObfEwNm1dqfX98D9xKLtCVsA-AC5AZH5AY65ARL5AQ8CgkClhwXQqic7qGyVALa1eRvgte9nBjs8ELhA-4FRTbL9oItWxVWVSnoGHZaeON6tuc1mKVrq1Wm18UkpjaplJALMoBbHRfvDVLJUkLGrwX2BHDYbXjXpRT-EcCLGG68iaSdKoO2eZE5Zsv9lRG9fPX13wnhY-s-K6zuWlHDXSZx5-XV8-AD4ALg8CAAgcEAMEg8IhMDEBAhUDg0OhQEgYIiMEEEWjMIisaAENjsEAUIjEXghCkEZQEbjMQhEChETlEglUKgIuEb4RKC_bo8qyH3l705OLCmJ1kMQd9iEV0aw7gStxMPmCTOAffgAoL2i-AmVd_b_p3gkSP5glYl9pIn40AEUMIvqwpHIvkahuHX4cwDiAaCp1buI8W7RsDhZAp69_T6gJBjO_aR00EmjIj3-397EXfhN-EuHBdCqJ0rbgLhB459yJ9MK7TDZ2xQ23jTHbdbiwOCTEcmb5U7olxiUgaFV28sh8nuVzXrjJXuGfUnNureSKSOSXSwvp-aQTixLNAH4APgA-QNauQNX-QNUALkBVPkBUbkBTvkBS4IgALkBRfkBQgCVAZQ5KOt2a33MGDNmJIyCIn-oz8dWgwdSZIMHUmSFAukO3QC49hAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAgQAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAABAAACAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAACAAAAAAIAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAACAAAAAAAAAAAAAAAAQAAAAAAAAAQAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAPgA-ACgvEWAgjiMdu0pUAxvQcurR0OVNFEmP5ZzhNMkpIVwqPr5Afn5AfYAuQHy-QHvo-IQoMOetu_ZC-YK5vgAe7UsNeBouviaGeP9VQgl1JDY7dRguFP4UaBjTIu0WBCx1UEf8MBqzibUyAkFNsmaQ3gnewIx7LIl96CegRcYwno7-6NZanIbuiC4o_93OVbLE-UByERTjAHFv4CAgICAgICAgICAgICAgLkBc_kBcCC5AWz5AWmVAZwHK-9UWr65aj97ov_fi4YftV3q-FSWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NgL4-7j5-Pe4PmJ0cDovLzB4NThlYjFjLmljb24vY3g5YzA3MmJlZjU0NWFiZWI5NmEzZjdiYTJmZmRmOGI4NjFmYjU1ZGVhuDpidHA6Ly8weDUwMS5wcmEvMHg1Q0MzMDcyNjhhMTM5M0FCOUE3NjRBMjBEQUNFODQ4QUI4Mjc1YzQ2im5hdGl2ZWNvaW4BuG34awC4aPhmqmh4NDUwMmFhZDc5ODZhZDVhODQ4OTU1MTVmYWY3NmU5MGI1YjQ3ODY1NKoweDE1OEEzOTFGMzUwMEMzMjg4QWIyODY1MzcyMmE2NDU5RTc3MjZCMDHPzoNJQ1iJAIlj3YwsXgAA"
		var relaymessage icon.RelayMessage
		// var votes Votes

		base64Data, err := base64.URLEncoding.DecodeString(relay_message)
		assert.NoError(f, err)

		_, err = codec.RLP.UnmarshalFromBytes(base64Data, &relaymessage)
		assert.NoError(f, err)

		var blockUpdates []*chain.BlockUpdate

		for _, blockupdate := range relaymessage.BlockUpdates {

			var newBlockUpdate chain.BlockUpdate
			_, err = codec.RLP.UnmarshalFromBytes(blockupdate, &newBlockUpdate)
			assert.NoError(f, err)

			blockUpdates = append(blockUpdates, &newBlockUpdate)
		}

		var reciptProofs []*chain.ReceiptProof

		for _, receiptProof := range relaymessage.ReceiptProofs {

			var rP icon.ReceiptProof
			_, err = codec.RLP.UnmarshalFromBytes(receiptProof, &rP)
			assert.NoError(f, err)

			receiptProofs := chain.ReceiptProof{
				Index: rP.Index,
				Proof: rP.Proof,
			}

			ev := chain.Event{
				Next:     "",
				Sequence: 1,
				Message:  []byte(``),
			}

			receiptProofs.Events = append(receiptProofs.Events, &ev)

			receiptProofs.EventProofs = append(receiptProofs.EventProofs, rP.EventProofs...)

			reciptProofs = append(reciptProofs, &receiptProofs)

		}

		rm := chain.RelayMessage{
			From:          source,
			ReceiptProofs: reciptProofs,
			BlockUpdates:  blockUpdates,
			BlockProof:    nil,
			Seq:           4,
			HeightOfDst:   bmcStatus.CurrentHeight,
		}

		segments, err := sender.Segment(&rm, 377825)
		assert.NoError(f, err)

		expected := `{"status":{"SuccessValue":"","SuccessReceiptId":null,"Failure":{"ActionError":{"index":0,"kind":{"AccountDoesNotExist":{"account_id":""}}}},"Unknown":""},"transaction":{"signer_id":"alice.node0","public_key":"ed25519:5rsVhAhEqJTYn1Nfzf1hZMBPgX4VBXRT3RuA9kui6M4n","nonce":66835160000040,"receiver_id":"alice.node0","actions":[{"DeployContract":{"code":"hctiB5FxZn0CHrTuOkJXOjw0y3whj0uv/6DCCtUw0EI="}}],"signature":"ed25519:2tw21Gh6zCFgrYs4hBVUHuRxDA4G4kLj3vxWMwCtqxbn5AU7Zq7jwd7C9QAAzr3WbkFfM4YaFzhwUaJwE2gKA592","hash":"689ziDgHzcxCVdfeBvUimay3TGteJy6CbyFbT3i8t4At"},"transaction_outcome":{"proof":[{"hash":"NyvU5SIF/we320JOq/THxALACenHGYCU3j8GhRV2kBY=","direction":"Right"}],"block_hash":"Tu9PlmYOBPGGjv6RZlzTGs5awZU8PV/td6M+bCnB3Pg=","id":"TB+rxdDStvFnlmEnVfglDvDyqgljZv0y4fIrWASKfTE=","outcome":{"logs":[],"receipt_ids":["49itGryaP4qy0Bt/p25G3ht1JBEjN53uP36I9Hoi7aU="],"gas_burnt":3280025539544,"tokens_burnt":"328002553954400000000","executor_id":"alice.node0","status":{"SuccessValue":"","SuccessReceiptId":"49itGryaP4qy0Bt/p25G3ht1JBEjN53uP36I9Hoi7aU=","Failure":{"ActionError":{"index":0,"kind":{"AccountDoesNotExist":{"account_id":""}}}},"Unknown":""}}}}`
		for _, segment := range segments {

			result, err := sender.Relay(segment)
			assert.NoError(f, err)

			tx, err := sender.GetResult(result)
			assert.NoError(f, err)

			data, err := json.Marshal(tx)
			assert.NoError(f, err)
			assert.Equal(f, expected, string(data))
		}

	})

}
