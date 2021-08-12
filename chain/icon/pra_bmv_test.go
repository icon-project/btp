package icon

import (
	"testing"
	"time"

	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
)

func TestGetPraBmvStatus(t *testing.T) {
	// t.Skip("Manual test only")
	c := NewPraBmvClient("https://btp.net.solidwallet.io/api/v3/icon_dex", "cx88fe82e2427432bb0b2ba6d75b3ef4e25eb9d085", log.New())

	start1 := time.Now()
	c.GetRelayMtaHeight()
	c.GetRelayMtaOffset()
	c.GetSetId()
	c.GetParaMtaHeight()
	elapsed1 := time.Since(start1)

	start2 := time.Now()
	c.GetPraBmvStatus()
	elapsed2 := time.Since(start2)

	t.Logf("%f %f", elapsed1.Seconds(), elapsed2.Seconds())
	assert.Greater(t, elapsed1, elapsed2)
}
