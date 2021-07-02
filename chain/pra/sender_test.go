package pra

import (
	"context"
	"encoding/base64"
	"math/big"
	"math/rand"
	"testing"
	"time"

	"github.com/ethereum/go-ethereum/core/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/mocks"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	TestnetURL                     = "https://sejong.net.solidwallet.io/api/v3/icon_dex"
	TestBlockNumberHasReceiptProof = 273042
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

func genFakeBytes(n int) []byte {
	b := []byte{}
	for i := 0; i < n; i++ {
		b = append(b, 1)
	}
	return b
}

func TestNewTransactionParam(t *testing.T) {

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

func TestSegment(t *testing.T) {
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

func TestParseTransactionError(t *testing.T) {
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
