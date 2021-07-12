package icon

import (
	"testing"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/log"
)

func TestSender_Segment(t *testing.T) {
	s := NewSender(
		"btp://0x501.pra/0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090",
		"btp://0x3.icon/cx8eb24849a7ceb16b8fa537f5a8b378c6af4a0247",
		nil, // wallet
		"http://goloop.linhnc.info/api/v3/icondao",
		nil,
		log.New(),
	)

	t.Run("should append a new Segment with BaseMessage", func(t *testing.T) {
		s.Segment(&chain.RelayMessage{}, 10000)
	})
}
