package near

import (
	"encoding/json"
	"strconv"
	"testing"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/base"
	"github.com/icon-project/btp/chain/near/testdata"
	"github.com/icon-project/btp/chain/near/types"
	"github.com/reactivex/rxgo/v2"
	"github.com/stretchr/testify/assert"
)

func TestNearClient(t *testing.T) {
	if test, err := testdata.GetTest("ComputeBlockHash", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)

					block, err := client.api.getBlockByHeight(int64(input))
					assert.NoError(f, err)
					actual, err := block.ComputeHash(block.Header.PreviousBlockHash, block.InnerLite(), block.InnerRest())

					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).([]byte)
						assert.True(f, Ok)
						assert.Equal(f, expected, actual)
					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("GetBlockHeader", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)

					actual, err := client.GetBlockHeaderByHeight(int64(input))
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).([]byte)
						assert.True(f, Ok)
						assert.Equal(f, expected, actual.Serilaized)
					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("GetNonce", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {

					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).([]string)
					assert.True(f, Ok)

					actual, err := client.GetNonce(input[0], input[1])
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(int64)
						assert.True(f, Ok)
						assert.Equal(f, expected, actual)
					} else {
						assert.Error(f, err)
					}
				})

			}
		})
	}

	if test, err := testdata.GetTest("MonitorBlockHeight", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)

					observable := client.MonitorBlockHeight(int64(input)).Take(4).Observe()
					actualdata := make([]int64, 0)
					for result := range observable {
						actualdata = append(actualdata, result.V.(int64))
					}
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).([]int64)
						assert.True(f, Ok)
						for idx, data := range actualdata {
							assert.Equal(f, expected[idx], data)
						}
					} else {
						expected, Ok := (testData.Expected.Fail).([]int64)
						assert.True(f, Ok)
						for idx, data := range actualdata {
							assert.NotEqual(f, expected[idx], data)
						}
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("GetBmcLinkStatus", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}
					input, Ok := (testData.Input).([]chain.BtpAddress)
					assert.True(f, Ok)

					actual, err := client.GetBMCLinkStatus(input[0], input[1])
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(int)
						assert.True(f, Ok)
						assert.Equal(f, int64(expected), actual.RotateHeight)
					} else {
						assert.Error(f, err)
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("GetEvents", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}
					input, Ok := (testData.Input).([]string)
					assert.True(f, Ok)
					accountId := input[0]
					blockId, err := strconv.Atoi(input[1])
					assert.NoError(f, err)

					actualEvents, actualRecieptHash, err := client.getEvents(accountId, int64(blockId))
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(testdata.Eventsreposnse)
						assert.True(f, Ok)
						for index, event := range actualEvents {
							eventBytes, err := json.Marshal(event)
							assert.NoError(f, err)

							assert.Equal(f, expected.Events[index], string(eventBytes))
						}
						for index, receiptHash := range actualRecieptHash {

							receipthash := receiptHash.Base58Encode()

							assert.Equal(f, expected.RecieptHash[index], receipthash)
						}
					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("GetBlock", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)

					observable := client.GetBlock(int64(input)).Take(1).Observe()
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)

						for result := range observable {
							err := result.E
							assert.NoError(f, err)
							actualdata := result.V.(*MonitorBlockData)
							assert.Equal(f, expected, actualdata.BlockHash)
						}

					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("GetBlockProof", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int64)
					assert.True(f, Ok)
					block, err := client.api.getBlockByHeight(input)
					assert.NoError(f, err)

					var blockNotifcation base.BlockNotification = &block
					actual, err := client.GetBlockProof(&blockNotifcation)

					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).([]byte)
						assert.True(f, Ok)
						assert.Equal(f, expected, actual)
					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("MontorReceiverBlock", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).([]string)
					assert.True(f, Ok)
					source := chain.BtpAddress(input[0])
					destination := chain.BtpAddress(input[1])
					blockId, err := strconv.Atoi(input[2])
					assert.NoError(f, err)
					blockRequest := client.GetEventRequest(source, destination, int64(blockId))
					err = client.MonitorReceiverBlock(blockRequest,
						func(observable rxgo.Observable) error {
							result := observable.Take(1).Observe()
							var err error

							for item := range result {
								data := item.V.(*types.Block)

								if testData.Expected.Success != nil {

									expected, Ok := (testData.Expected.Success).(string)
									assert.True(f, Ok)

									assert.Equal(f, expected, data.Header.PreviousBlockHash.Base58Encode())
								} else {
									assert.Error(f, err)
								}

							}
							return nil
						},
						func() {
							client.CloseAllMonitor()
						})
					assert.NoError(f, err)

				})
			}
		})
	}

	if test, err := testdata.GetTest("MonitorSenderBlock", t); err == nil {
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)
					blockRequest := client.GetBlockRequest(int64(input))
					err := client.MonitorSenderBlock(blockRequest,
						func(observable rxgo.Observable) error {
							result := observable.Take(1).Observe()
							for item := range result {
								err := item.E
								assert.NoError(f, err)
								data := item.V.(*types.Block)

								if testData.Expected.Success != nil {
									expected, Ok := (testData.Expected.Success).(string)
									assert.True(f, Ok)
									assert.Equal(f, expected, data.Header.Hash.Base58Encode())
								}
							}
							return nil
						},
						func() {
							client.CloseAllMonitor()
						})
					assert.NoError(f, err)

				})
			}
		})
	}
	if test, err := testdata.GetTest("GetBmcRelayMethod", t); err == nil {
		var relayMessage base.RelayMessageClient
		t.Run(test.Description(), func(f *testing.T) {
			for _, testData := range test.TestDatas() {
				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(testdata.TransactionParams)
					assert.True(f, Ok)
					source := input.Source
					destination := input.Destination

					err := json.Unmarshal([]byte(input.RelayMessage), &relayMessage)
					assert.NoError(f, err)

					transactionParam, err := client.BMCRelayMethodTransactionParam(input.Wallet, destination, source.String(), &relayMessage, 0)
					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)
						newTransactionParam := transactionParam.(*types.TransactionParam)
						assert.Equal(f, expected, newTransactionParam.From)
					} else {

						assert.Error(f, err)
					}
					relayMessage, previous, err := client.GetRelayMethodParams(&transactionParam)
					if testData.Expected.Success != nil {
						newTransactionParam := transactionParam.(*types.TransactionParam)
						assert.Equal(f, newTransactionParam.RelayMessage.Message, relayMessage)
						assert.Equal(f, newTransactionParam.RelayMessage.Previous, previous)

					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

	if test, err := testdata.GetTest("CreateTransaction", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {

				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(testdata.Input)
					assert.True(f, Ok)
					transactionParam := chain.TransactionParam(&input.TransactionParameters)
					err := client.CreateTransaction(input.Wallet, &transactionParam)
					if testData.Expected.Success != nil {

						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)
						acutal, Ok := (transactionParam).(*types.TransactionParam)
						assert.True(f, Ok)
						assert.Equal(f, expected, acutal.Base64encodedData)
					} else {
						assert.Error(f, err)
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("GetReceiptProof", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {

				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(int)
					assert.True(f, Ok)

					blockRequest := &BlockRequest{
						Height: int64(input),
					}
					newBlockRequest := base.BlockRequest(blockRequest)
					receiptProofs, err := client.GetReceiptProofs(&newBlockRequest)

					if testData.Expected.Success != nil {

						expected, Ok := (testData.Expected.Success).([]string)
						assert.True(f, Ok)
						for index, receiptsProof := range receiptProofs {

							byetsdata, err := json.Marshal(receiptsProof)
							assert.NoError(f, err)
							assert.Equal(f, expected[index], string(byetsdata))
						}
					} else {
						assert.Error(f, err)
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("SendTransaction", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {

				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(chain.TransactionParam)
					assert.True(f, Ok)
					transactionHash, err := client.SendTransaction(&input)
					if testData.Expected.Success != nil {

						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)
						assert.Equal(f, expected, string(transactionHash))
					} else {

						assert.Error(f, err)
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("SendTransactionWait", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {

				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}

					input, Ok := (testData.Input).(chain.TransactionParam)
					assert.True(f, Ok)
					transactionHash, err := client.SendTransactionAndWait(&input)
					if testData.Expected.Success != nil {

						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)
						assert.Equal(f, expected, string(transactionHash))
					} else {

						assert.Error(f, err)
					}

				})
			}
		})
	}

	if test, err := testdata.GetTest("GetTransactionResult", t); err == nil {

		t.Run(test.Description(), func(f *testing.T) {

			for _, testData := range test.TestDatas() {

				f.Run(testData.Description, func(f *testing.T) {
					mockApi := NewMockApi(testData.MockStorage)
					client := &Client{
						api: &mockApi,
					}
					input, Ok := (testData.Input).(testdata.Input)
					assert.True(f, Ok)
					var blockHashParam base.TransactionHashParam
					err := client.AssignHash(input.Wallet, &blockHashParam, []byte(input.Hash))

					assert.NoError(f, err)

					getResultParam := chain.GetResultParam(&blockHashParam)

					transactionResult, err := client.GetTransactionResult(&getResultParam)

					if testData.Expected.Success != nil {
						expected, Ok := (testData.Expected.Success).(string)
						assert.True(f, Ok)

						actualData, Ok := (transactionResult).(types.TransactionResult)
						assert.True(f, Ok)

						assert.Equal(f, expected, actualData.TransactionOutcome.BlockHash.Base58Encode())

					} else {
						assert.Error(f, err)
					}
				})
			}
		})
	}

}
