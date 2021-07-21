package btp

import (
	"errors"
	"testing"

	"github.com/gammazero/workerpool"
	"github.com/icon-project/btp/chain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func TestBTP_updateResult(t *testing.T) {
	mockSender := &chain.MockSender{}

	btp := &BTP{
		sender: mockSender,
		wp:     workerpool.New(DefaultMaxWorkers),
	}

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
}
