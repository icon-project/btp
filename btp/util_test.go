package btp

import (
	"testing"
)

func TestBTP_PrepareDatabaset(t *testing.T) {
	// mockSender := &chain.MockSender{}

	// btp := &BTP{
	// 	sender: mockSender,
	// 	wp:     workerpool.New(DefaultMaxWorkers),
	// }

	t.Run("should call database.Open() and defer call database.Close()", func(t *testing.T) {
		t.Skip()
	})

	t.Run("should assign BTP.store with ExtAccumulator", func(t *testing.T) {
		t.Skip()
	})
}
