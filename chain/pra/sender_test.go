package pra

import (
	"context"
	"encoding/base64"
	"math/big"
	"math/rand"
	"testing"
	"time"

	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/binding"
	"github.com/icon-project/btp/chain/pra/mocks"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	TestnetGooloopURL              = "https://sejong.net.solidwallet.io/api/v3/icon_dex"
	TestnetMoonbaseURL             = "https://rpc.testnet.moonbeam.network"
	TestBlockNumberHasReceiptProof = 273042
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

func genFakeBytes(n int) []byte {
	b := []byte{}
	for i := 0; i < n; i++ {
		b = append(b, uint8(n%127))
	}
	return b
}

func testWallet() wallet.Wallet {
	privateKey, err := crypto.HexToECDSA("8075991ce870b93a8870eca0c0f91913d12f47948ca0fd25b49c6fa7cdbeee8b")
	if err != nil {
		panic(err)
	}

	w, err := wallet.NewEvmWalletFromPrivateKey(privateKey)
	if err != nil {
		panic(err)
	}
	return w
}

func TestSenderNewTransactionParam(t *testing.T) {

	prev := "string"
	rm1 := &RelayMessage{
		BlockUpdates:  [][]byte{{1, 2, 3, 4}},
		BlockProof:    []byte{1, 2, 3, 4},
		ReceiptProofs: [][]byte{{1, 2, 3, 4}},
	}

	sender := &Sender{log: log.New()}
	p, err := sender.newTransactionParam(prev, rm1)
	require.Nil(t, err)
	require.NotNil(t, p)
	assert.Equal(t, prev, p.Prev)

	d, err := base64.URLEncoding.DecodeString(p.Msg)
	require.Nil(t, err)
	rm2 := &RelayMessage{}

	_, err = codec.RLP.UnmarshalFromBytes(d, rm2)
	require.Nil(t, err)
	assert.EqualValues(t, rm1, rm2)
}

func TestSenderSegment(t *testing.T) {
	f := txSizeLimit
	txSizeLimit := int(f)
	sender := &Sender{log: log.New()}

	t.Run("should get error ErrInvalidBlockUpdateProofSize", func(t *testing.T) {
		segments, err := sender.Segment(&chain.RelayMessage{
			BlockUpdates: []*chain.BlockUpdate{
				{
					Height: 1,
					Proof:  genFakeBytes(txSizeLimit + 1),
				},
			},
		}, 0)

		require.Nil(t, segments)
		assert.Equal(t, ErrInvalidBlockUpdateProofSize, err)
	})

	t.Run("should be segmented by over BlockProof size", func(t *testing.T) {
		blocks := MaxBlockUpdatesPerSegment - 1

		rm := &chain.RelayMessage{}
		for i := 1; i <= blocks; i++ {
			rm.BlockUpdates = append(rm.BlockUpdates, &chain.BlockUpdate{
				Height: int64(i),
				Proof:  genFakeBytes(txSizeLimit),
			})
		}

		segments, err := sender.Segment(rm, 0)
		require.Nil(t, err)
		assert.Len(t, segments, 2)
		for _, segment := range segments {
			assert.NotNil(t, segment.TransactionParam)
			assert.Nil(t, segment.TransactionResult)
		}
	})

	t.Run("should be segmented by over BlockUpdates", func(t *testing.T) {
		blocks := MaxBlockUpdatesPerSegment + 1
		limit := txSizeLimit / (blocks + 1)

		rm := &chain.RelayMessage{}
		for i := 1; i <= blocks; i++ {
			rm.BlockUpdates = append(rm.BlockUpdates, &chain.BlockUpdate{
				Height: int64(i),
				Proof:  genFakeBytes(limit),
			})
		}

		segments, err := sender.Segment(rm, 0)
		require.Nil(t, err)
		assert.Len(t, segments, 2)
		for _, segment := range segments {
			assert.NotNil(t, segment.TransactionParam)
			assert.Nil(t, segment.TransactionResult)
		}
	})

	t.Run("should get error ErrInvalidReceiptProofSize", func(t *testing.T) {
		rm := &chain.RelayMessage{
			BlockUpdates: []*chain.BlockUpdate{
				{
					Height: 1,
					Proof:  genFakeBytes(txSizeLimit),
				},
			},
			ReceiptProofs: []*chain.ReceiptProof{
				{
					Proof: genFakeBytes(txSizeLimit + 1),
				},
			},
		}

		segments, err := sender.Segment(rm, 0)
		require.NotNil(t, err)
		require.Nil(t, segments)
		assert.Equal(t, ErrInvalidReceiptProofSize, err)
	})

	t.Run("should get error ErrInvalidEventProofProofSize", func(t *testing.T) {
		rm := &chain.RelayMessage{
			BlockUpdates: []*chain.BlockUpdate{
				{
					Height: 1,
				},
			},
			ReceiptProofs: []*chain.ReceiptProof{
				{
					Proof: genFakeBytes(txSizeLimit),
					EventProofs: []*chain.EventProof{
						{
							Proof: genFakeBytes(txSizeLimit + 1),
						},
					},
				},
			},
		}

		segments, err := sender.Segment(rm, 0)
		require.NotNil(t, err)
		require.Nil(t, segments)
		assert.Equal(t, ErrInvalidEventProofProofSize, err)
	})

	t.Run("should be segmented by over EventProofs size", func(t *testing.T) {
		for i := 0; i < 10; i++ {
			numRps := i
			numEvents := i
			sizeLimit := txSizeLimit / 3 //to avoid this error BlockProof + ReceiptProof + EventProof
			numSegments := 0
			size := 0

			bu := &chain.BlockUpdate{
				Proof:  genFakeBytes(sizeLimit),
				Height: 1,
			}
			rm := &chain.RelayMessage{
				From:         "string",
				Seq:          1,
				BlockUpdates: []*chain.BlockUpdate{bu},
			}

			size += sizeLimit

			for i := 0; i < numRps; i++ {
				rp := &chain.ReceiptProof{
					Index: i,
					Proof: genFakeBytes(sizeLimit),
				}
				size += sizeLimit

				for j := 0; j < numEvents; j++ {
					rp.Events = append(rp.Events, &chain.Event{
						Sequence: int64(j),
					})

					ep := &chain.EventProof{
						Index: j,
						Proof: genFakeBytes(sizeLimit),
					}
					size += sizeLimit
					if sender.isOverSizeLimit(size) {
						numSegments++
						size = sizeLimit * 3
					}

					rp.EventProofs = append(rp.EventProofs, ep)
				}
				rm.ReceiptProofs = append(rm.ReceiptProofs, rp)
			}
			numSegments += 1

			segments, err := sender.Segment(rm, 0)
			require.Nil(t, err)
			assert.Len(t, segments, numSegments)
			for i, segment := range segments {
				assert.NotNil(t, segment.TransactionParam)
				assert.Nil(t, segment.TransactionResult)

				p, ok := segment.TransactionParam.(*RelayMessageParam)
				require.True(t, ok)
				require.NotNil(t, p)

				d, err := base64.URLEncoding.DecodeString(p.Msg)
				require.Nil(t, err)
				rm2 := &RelayMessage{}
				_, err = codec.RLP.UnmarshalFromBytes(d, rm2)
				require.Nil(t, err)

				if i > 0 {
					assert.EqualValues(t, rm2.BlockProof, bu.Proof)
					assert.NotEmpty(t, rm2.ReceiptProofs)
				} else {
					assert.EqualValues(t, rm2.BlockUpdates[0], bu.Proof)
				}
			}
		}
	})
}

func TestSenderParseTransactionError(t *testing.T) {
	var encodedErrors = map[string]int{
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001b424d435265766572743a2052616e646f6d537472696e67486572650000000000":                                                                 10,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000027424d43526576657274556e617574686f72697a65643a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000000000": 11,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000024424d43526576657274496e76616c6964534e3a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000000000000000": 12,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002b424d43526576657274416c7265616479457869737473424d563a2052616e646f6d537472696e6748657265000000000000000000000000000000000000000000": 13,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000027424d435265766572744e6f74457869737473424d563a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000000000": 14,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002b424d43526576657274416c72656164794578697374734253483a2052616e646f6d537472696e6748657265000000000000000000000000000000000000000000": 15,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000027424d435265766572744e6f744578697374734253483a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000000000": 16,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002c424d43526576657274416c72656164794578697374734c696e6b3a2052616e646f6d537472696e67486572650000000000000000000000000000000000000000": 17,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000028424d435265766572744e6f744578697374734c696e6b3a2052616e646f6d537472696e6748657265000000000000000000000000000000000000000000000000": 18,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000026424d43526576657274556e726561636861626c653a2052616e646f6d537472696e67486572650000000000000000000000000000000000000000000000000000": 19,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002e424d435265766572744e6f744578697374735065726d697373696f6e3a2052616e646f6d537472696e6748657265000000000000000000000000000000000000": 20,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001b424d565265766572743a2052616e646f6d537472696e67486572650000000000":                                                                 25,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000025424d56526576657274496e76616c69644d50543a2052616e646f6d537472696e6748657265000000000000000000000000000000000000000000000000000000": 26,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000027424d56526576657274496e76616c6964566f7465733a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000000000": 27,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002a424d56526576657274496e76616c696453657175656e63653a2052616e646f6d537472696e674865726500000000000000000000000000000000000000000000": 28,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002d424d56526576657274496e76616c6964426c6f636b5570646174653a2052616e646f6d537472696e674865726500000000000000000000000000000000000000": 29,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002c424d56526576657274496e76616c6964426c6f636b50726f6f663a2052616e646f6d537472696e67486572650000000000000000000000000000000000000000": 30,
		"08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002e424d56526576657274496e76616c6964426c6f636b5769746e6573733a2052616e646f6d537472696e6748657265000000000000000000000000000000000000": 31,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000030424d56526576657274496e76616c696453657175656e63654869676865723a2052616e646f6d537472696e674865726500000000000000000000000000000000": 32,
		"08c379a000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000033424d56526576657274496e76616c6964426c6f636b5570646174654869676865723a2052616e646f6d537472696e674865726500000000000000000000000000": 33,
	}

	ethClient := &mocks.EthClient{}
	sender := &Sender{c: &Client{ethClient: ethClient}}

	address := EvmHexToAddress("0x0000000000000000000000000000000000000000")
	tx := types.NewTransaction(0, address, big.NewInt(0), 0, big.NewInt(0), []byte(""))
	callmsg := EvmCallMsg{From: address, To: tx.To(), Gas: tx.Gas(), GasPrice: tx.GasPrice(), Value: tx.Value(), Data: tx.Data()}
	ctx := context.Background()

	for key, value := range encodedErrors {
		mockErr := &mocks.DataError{}
		mockErr.On("ErrorData").Return(key)

		ethClient.On("CallContract", ctx, callmsg, mock.AnythingOfType("*big.Int")).Return(nil, mockErr).Once()
		err := sender.parseTransactionError(address, tx, nil)
		ec, ok := errors.CoderOf(err)
		require.True(t, ok)
		assert.EqualValues(t, value, ec.ErrorCode())
		ethClient.AssertCalled(t, "CallContract", ctx, callmsg, mock.AnythingOfType("*big.Int"))
	}

	ethClient.On("CallContract", ctx, callmsg, mock.AnythingOfType("*big.Int")).Return([]byte(""), nil).Once()
	assert.EqualError(t, sender.parseTransactionError(address, tx, nil), "parseTransactionError: empty")
}

func TestSenderUpdateSegment(t *testing.T) {
	sender := &Sender{log: log.New()}
	t.Run("should crash if TransactionParam not a *RelayMessageParam", func(t *testing.T) {
		segment := &chain.Segment{}
		require.Panics(t, func() { sender.UpdateSegment(&chain.BlockProof{}, segment) })
	})

	t.Run("should return error msg is not a encoded base64", func(t *testing.T) {
		p := &RelayMessageParam{
			Msg: "rawstring",
		}
		segment := &chain.Segment{TransactionParam: p}
		err := sender.UpdateSegment(&chain.BlockProof{}, segment)
		require.NotNil(t, err)
		assert.Contains(t, err.Error(), "base64")
		assert.Equal(t, p, segment.TransactionParam)
	})

	t.Run("should return error msg is not a encoded rlp", func(t *testing.T) {
		err := sender.UpdateSegment(&chain.BlockProof{}, &chain.Segment{
			TransactionParam: &RelayMessageParam{
				Msg: base64.URLEncoding.EncodeToString([]byte("rawstring")),
			},
		})
		require.NotNil(t, err)
		assert.Error(t, err, "InvalidFormat(RLPBytes)")
	})

	t.Run("should return error msg is not RelayMessage", func(t *testing.T) {
		b := codec.RLP.MustMarshalToBytes("randomgstring")
		base64encoded := base64.URLEncoding.EncodeToString(b)

		err := sender.UpdateSegment(&chain.BlockProof{}, &chain.Segment{
			TransactionParam: &RelayMessageParam{
				Msg: base64encoded,
			},
		})
		require.NotNil(t, err)
		assert.Error(t, err, "InvalidFormat(RLPBytes)")
	})

	t.Run("should updated UpdateSegment ok", func(t *testing.T) {
		rm := &RelayMessage{
			BlockProof:    genFakeBytes(10),
			BlockUpdates:  [][]byte{genFakeBytes(11)},
			ReceiptProofs: [][]byte{genFakeBytes(12)},
		}
		b := codec.RLP.MustMarshalToBytes(rm)
		base64encoded := base64.URLEncoding.EncodeToString(b)
		p := &RelayMessageParam{Msg: base64encoded}
		segment := &chain.Segment{TransactionParam: p}
		bp := &chain.BlockProof{Header: genFakeBytes(13)}

		err := sender.UpdateSegment(bp, segment)
		require.Nil(t, err)
		assert.NotNil(t, segment.TransactionParam)
		p1 := segment.TransactionParam.(*RelayMessageParam)
		require.NotNil(t, p1)
		assert.NotEqualValues(t, p, p1)

		rm1 := &RelayMessage{}
		bp1 := &chain.BlockProof{Header: genFakeBytes(13)}

		b1, _ := base64.URLEncoding.DecodeString(p1.Msg)
		require.NotNil(t, b1)
		_, err = codec.RLP.UnmarshalFromBytes(b1, rm1)
		require.Nil(t, err)
		_, err = codec.RLP.UnmarshalFromBytes(rm1.BlockProof, bp1)
		require.Nil(t, err)

		require.Nil(t, err)
		assert.Equal(t, rm.ReceiptProofs, rm1.ReceiptProofs)
		assert.Equal(t, rm.BlockUpdates, rm1.BlockUpdates)
		assert.Equal(t, bp, bp1)
	})
}

func TestSenderRelay(t *testing.T) {
	DefaultRetryContractCall = 0 // reduce test time
	bmcMock := &mocks.BMCContract{}
	sender := &Sender{log: log.New(), c: &Client{bmc: bmcMock}, w: testWallet()}

	t.Run("should return error if TransactionParam not a *RelayMessageParam", func(t *testing.T) {
		_, err := sender.Relay(&chain.Segment{})
		require.NotNil(t, err)
		assert.Error(t, err, "casting failure")
	})

	t.Run("should return error if got error on bmc.HandleRelayMessage", func(t *testing.T) {
		bmcMock.On("HandleRelayMessage", mock.AnythingOfType("*bind.TransactOpts"), mock.Anything, mock.Anything).Return(nil, errors.New("HandleRelayMessageError")).Once()
		_, err := sender.Relay(&chain.Segment{
			TransactionParam: &RelayMessageParam{},
		})
		require.NotNil(t, err)
		assert.Error(t, err, "HandleRelayMessageError")
	})

	t.Run("should return TransactionHashParam", func(t *testing.T) {
		p := &RelayMessageParam{
			Prev: "string",
			Msg:  "string",
		}
		tx := &EvmTransaction{}

		bmcMock.On("HandleRelayMessage", mock.AnythingOfType("*bind.TransactOpts"), mock.Anything, mock.Anything).Return(tx, nil).Once()
		r, err := sender.Relay(&chain.Segment{
			TransactionParam: p,
		})
		require.Nil(t, err)
		thp, ok := r.(*TransactionHashParam)
		require.True(t, ok)
		assert.Equal(t, tx, thp.Tx)
		assert.Equal(t, sender.w.Address(), thp.From.Hex())
		assert.Equal(t, p, thp.Param)
	})
}

func TestSenderGetResult(t *testing.T) {
	ethClient := &mocks.EthClient{}
	sender := &Sender{log: log.New(), c: &Client{ethClient: ethClient}}
	ctx := context.Background()
	fromAddress := EvmHexToAddress("0x0000000000000000000000000000000000000000")

	t.Run("should return error if GetResultParam not a *TransactionHashParam", func(t *testing.T) {
		_, err := sender.GetResult(nil)
		require.NotNil(t, err)
		assert.Contains(t, err.Error(), "fail to casting TransactionHashParam")
	})

	t.Run("should return error if Status = 0", func(t *testing.T) {
		p := &TransactionHashParam{
			From:  fromAddress,
			Tx:    types.NewTransaction(0, fromAddress, big.NewInt(0), 0, big.NewInt(0), nil),
			Param: &RelayMessageParam{},
		}
		receipt := &EvmReceipt{
			BlockNumber: big.NewInt(1),
			Status:      0,
		}

		ethClient.On("TransactionReceipt", mock.AnythingOfType("*context.emptyCtx"), p.Tx.Hash()).Return(receipt, nil).Once()
		ethClient.On("CallContract", ctx, mock.AnythingOfType("ethereum.CallMsg"), mock.AnythingOfType("*big.Int")).Return([]byte(""), nil)
		_, err := sender.GetResult(p)
		require.NotNil(t, err)
	})

	t.Run("should return string as TransactionResult if Status = 1", func(t *testing.T) {
		p := &TransactionHashParam{
			From:  fromAddress,
			Tx:    types.NewTransaction(0, fromAddress, big.NewInt(0), 0, big.NewInt(0), nil),
			Param: &RelayMessageParam{},
		}
		receipt := &EvmReceipt{
			BlockNumber: big.NewInt(1),
			Status:      1,
			TxHash:      EvmHexToHash("0x89807d988e91650d41c6cf1127c3106a0d281b0c0523e6af61df5859e1bcd935"),
		}

		ethClient.On("TransactionReceipt", mock.AnythingOfType("*context.emptyCtx"), p.Tx.Hash()).Return(receipt, nil).Once()
		tr, err := sender.GetResult(p)
		require.Nil(t, err)
		require.IsType(t, "string", tr)
		assert.Equal(t, receipt.TxHash.Hex(), tr)
	})
}

func TestSenderGetStatus(t *testing.T) {
	DefaultRetryContractCall = 0 // reduce test time
	bmcMock := &mocks.BMCContract{}
	sender := &Sender{
		log: log.New(),
		src: chain.BtpAddress("btp://0x2c295d.icon/cx8e270cb0610d67daeb0de5fbaebbbd812de28e4d"),
		c:   &Client{bmc: bmcMock}, w: testWallet()}

	t.Run("should return error if got error on bmc.GetStatus", func(t *testing.T) {
		getStatusErr := errors.New("GetStatusError")
		bmcMock.On("GetStatus", mock.AnythingOfType("*bind.CallOpts"), sender.src.String()).Return(binding.TypesLinkStats{}, getStatusErr).Once()

		_, err := sender.GetStatus()
		require.NotNil(t, err)
		assert.Contains(t, err.Error(), getStatusErr.Error())
	})

	t.Run("should return chain.BMCLinkStatus", func(t *testing.T) {
		input := binding.TypesLinkStats{
			BlockIntervalDst: big.NewInt(1),
			BlockIntervalSrc: big.NewInt(2),
			TxSeq:            big.NewInt(3),
			RxSeq:            big.NewInt(4),
			Verifier: binding.TypesVerifierStats{
				HeightMTA:  big.NewInt(4),
				OffsetMTA:  big.NewInt(5),
				LastHeight: big.NewInt(6),
			},
			RotateHeight:   big.NewInt(7),
			RotateTerm:     big.NewInt(8),
			DelayLimit:     big.NewInt(9),
			MaxAggregation: big.NewInt(10),
			RxHeight:       big.NewInt(11),
			RxHeightSrc:    big.NewInt(12),
			CurrentHeight:  big.NewInt(13),
			RelayIdx:       big.NewInt(14),
			Relays: []binding.TypesRelayStats{
				{
					Addr:       EvmHexToAddress("0x3Cd0A705a2DC65e5b1E1205896BaA2be8A07c6e0"),
					BlockCount: big.NewInt(15),
					MsgCount:   big.NewInt(16),
				},
			},
		}

		bmcMock.On("GetStatus", mock.AnythingOfType("*bind.CallOpts"), sender.src.String()).Return(input, nil)

		output, err := sender.GetStatus()
		require.Nil(t, err)
		assert.EqualValues(t, input.BlockIntervalSrc.Int64(), output.BlockIntervalSrc)
		assert.EqualValues(t, input.BlockIntervalDst.Int64(), output.BlockIntervalDst)
		assert.EqualValues(t, input.TxSeq.Int64(), output.TxSeq)
		assert.EqualValues(t, input.RxSeq.Int64(), output.RxSeq)
		assert.EqualValues(t, input.Verifier.HeightMTA.Int64(), output.Verifier.Height)
		assert.EqualValues(t, input.Verifier.OffsetMTA.Int64(), output.Verifier.Offset)
		assert.EqualValues(t, input.Verifier.LastHeight.Int64(), output.Verifier.LastHeight)
		assert.EqualValues(t, input.RotateHeight.Int64(), output.RotateHeight)
		assert.EqualValues(t, input.RotateTerm.Int64(), output.RotateTerm)
		assert.EqualValues(t, input.DelayLimit.Int64(), output.DelayLimit)
		assert.EqualValues(t, input.MaxAggregation.Int64(), output.MaxAggregation)
		assert.EqualValues(t, input.RxHeight.Int64(), output.RxHeight)
		assert.EqualValues(t, input.RxHeightSrc.Int64(), output.RxHeightSrc)
		assert.EqualValues(t, input.CurrentHeight.Int64(), output.CurrentHeight)
		assert.EqualValues(t, input.RelayIdx.Int64(), output.BMRIndex)
		assert.Len(t, output.BMRs, 1)
		assert.Equal(t, input.Relays[0].Addr.Hex(), output.BMRs[0].Address)
		assert.EqualValues(t, input.Relays[0].BlockCount.Int64(), output.BMRs[0].BlockCount)
		assert.EqualValues(t, input.Relays[0].MsgCount.Int64(), output.BMRs[0].MessageCount)
	})
}

func TestSenderMonitorLoop(t *testing.T) {
	log := log.New()

	t.Run("monitor from current finallized header", func(t *testing.T) {
		subClient := &mocks.SubstrateClient{}
		sender := &Sender{
			log: log,
			c:   &Client{log: log, subClient: subClient, stopMonitorSignal: make(chan bool)},
		}

		monitoredBlocks := []uint64{}
		pulledBlocks := []uint64{}

		cb := func(height int64) error {
			monitoredBlocks = append(monitoredBlocks, uint64(height))
			return nil
		}

		blockNumber := uint64(1)
		stopAt := uint64(10)
		blockHeader := &SubstrateHeader{}
		hash := NewSubstrateHashFromHexString("0xe11336d6e16cce664b5e9c83ecfaecb9c2f5d5866cf605493d215ca79d88b3e9")

		subClient.On("GetFinalizedHead").Return(hash, nil)
		subClient.On("GetHeader", hash).Return(blockHeader, nil).Run(func(args mock.Arguments) {
			blockHeader.Number = SubstrateBlockNumber(blockNumber)
			blockNumber++
		})
		subClient.On("GetBlockHash", mock.AnythingOfType("uint64")).Return(hash, nil).Run(func(args mock.Arguments) {
			pullBlock := args[0].(uint64)
			pulledBlocks = append(pulledBlocks, pullBlock)
			if pullBlock == stopAt {
				sender.StopMonitorLoop()
			}
		})

		err := sender.MonitorLoop(int64(blockNumber), cb, func() {})
		assert.Nil(t, err)
		assert.Len(t, monitoredBlocks, int(stopAt))
		assert.EqualValues(t, pulledBlocks, monitoredBlocks)
	})

	t.Run("monitor from a heigher finallized header", func(t *testing.T) {
		subClient := &mocks.SubstrateClient{}
		sender := &Sender{
			log: log,
			c:   &Client{log: log, subClient: subClient, stopMonitorSignal: make(chan bool)},
		}

		monitoredBlocks := []uint64{}
		pulledBlocks := []uint64{}

		cb := func(height int64) error {
			monitoredBlocks = append(monitoredBlocks, uint64(height))
			return nil
		}

		blockNumber := uint64(1)
		stopAt := uint64(10)
		from := blockNumber + 2
		blockHeader := &SubstrateHeader{}
		hash := NewSubstrateHashFromHexString("0xe11336d6e16cce664b5e9c83ecfaecb9c2f5d5866cf605493d215ca79d88b3e9")

		subClient.On("GetFinalizedHead").Return(hash, nil)
		subClient.On("GetHeader", hash).Return(blockHeader, nil).Run(func(args mock.Arguments) {
			blockHeader.Number = SubstrateBlockNumber(blockNumber)
			blockNumber++
		})
		subClient.On("GetBlockHash", mock.AnythingOfType("uint64")).Return(hash, nil).Run(func(args mock.Arguments) {
			pullBlock := args[0].(uint64)
			pulledBlocks = append(pulledBlocks, pullBlock)
			if pullBlock == stopAt {
				sender.StopMonitorLoop()
			}
		})

		err := sender.MonitorLoop(int64(from), cb, func() {})
		assert.Nil(t, err)
		assert.Len(t, monitoredBlocks, int(stopAt-from+1))
		assert.EqualValues(t, pulledBlocks, monitoredBlocks)
	})
}
