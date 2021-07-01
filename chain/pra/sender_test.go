package pra

import (
	"encoding/base64"
	"math/rand"
	"testing"
	"time"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
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
