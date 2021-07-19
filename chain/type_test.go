package chain

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRelayMessageRemoveSegment(t *testing.T) {

	t.Run("should removed 1 segment ok", func(t *testing.T) {
		rm := RelayMessage{
			Segments: []*Segment{{}},
		}

		rm.RemoveSegment(1)
		assert.Nil(t, rm.Segments)
	})

	t.Run("should removed 0 segment ok", func(t *testing.T) {
		rm := RelayMessage{
			Segments: []*Segment{{}},
		}

		rm.RemoveSegment(1)
		assert.Nil(t, rm.Segments)
	})

	t.Run("should removed a segment from 3 segments ok", func(t *testing.T) {
		rm1 := RelayMessage{
			Segments: []*Segment{{Height: 1}, {Height: 2}, {Height: 3}},
		}
		sg1 := rm1.Segments[0]
		rm1.RemoveSegment(0)
		assert.Len(t, rm1.Segments, 2)
		assert.NotContains(t, rm1.Segments, sg1)

		rm2 := RelayMessage{
			Segments: []*Segment{{Height: 1}, {Height: 2}, {Height: 3}},
		}
		sg2 := rm2.Segments[1]
		rm2.RemoveSegment(1)
		assert.Len(t, rm2.Segments, 2)
		assert.NotContains(t, rm2.Segments, sg2)

		rm3 := RelayMessage{
			Segments: []*Segment{{Height: 1}, {Height: 2}, {Height: 3}},
		}
		sg3 := rm3.Segments[2]
		rm3.RemoveSegment(2)
		assert.Len(t, rm3.Segments, 2)
		assert.NotContains(t, rm3.Segments, sg3)

		rm4 := RelayMessage{
			Segments: []*Segment{{Height: 1}, {Height: 2}, {Height: 3}},
		}
		assert.Panics(t, func() {
			rm4.RemoveSegment(4)
		})
	})
}
