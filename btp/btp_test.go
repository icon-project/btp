package btp

import (
	"errors"
	"testing"

	"github.com/gammazero/workerpool"
	"github.com/icon-project/btp/chain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func TestBTP_UpdateResult(t *testing.T) {
	mockSender := &chain.MockSender{}

	btp := &BTP{
		sender: mockSender,
		wp:     workerpool.New(DefaultMaxWorkers),
	}

	t.Run("should panic if error in unknown", func(t *testing.T) {
		t.Skip()
	})

	t.Run("should panic if error is not decodable", func(t *testing.T) {
		t.Skip()
	})

	t.Run("should remove segment when receive BMVRevertInvalidSequence, BMVRevertInvalidBlockUpdateLower", func(t *testing.T) {
		t.Skip()
		segment := &chain.Segment{
			GetResultParam: mock.AnythingOfType("chain.GetResultParam"),
		}
		rm := &chain.RelayMessage{
			Segments: make([]*chain.Segment, 3),
		}
		rm.Segments = append(rm.Segments, segment)

		mockSender.On(
			"GetResult",
			segment.GetResultParam,
		).Return(
			mock.AnythingOfType("chain.TransactionResult"),
			errors.New(chain.BMVRevertCodeNames[chain.BMVRevertInvalidSequence]),
		).Once()

		btp.updateResult(rm, segment)
		assert.Len(t, rm.Segments, 3)
	})

	t.Run("should call senderUpdateSegment when receive BMVRevertInvalidBlockWitnessOld", func(t *testing.T) {
		t.Skip()
	})

	t.Run("should call only set segment.GetResultParam = nil when receive BMVRevertInvalidSequenceHigher or BMVRevertInvalidBlockUpdateHigher, BMVRevertInvalidBlockProofHigher", func(t *testing.T) {
		t.Skip()
	})

	t.Run("should call only set segment.GetResultParam = nil when receiver BMCRevertUnauthorized", func(t *testing.T) {
		t.Skip()
	})
}
