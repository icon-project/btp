package icon

import (
	"testing"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/require"
)

func TestSender_newTransactionParam(t *testing.T) {
	prev := "btp://0x3.icon/cx0c59775cb325c1aea9c872546dbe8365af7c9d07"
	rm1 := &RelayMessage{
		BlockUpdates:  [][]byte{{1, 2, 3, 4}},
		BlockProof:    []byte{1, 2, 3, 4},
		ReceiptProofs: [][]byte{{1, 2, 3, 4}},
	}

	rm1b, err := codec.RLP.MarshalToBytes(rm1)
	require.NoError(t, err)

	sender := &sender{l: log.New()}
	p, err := sender.newTransactionParam(prev, rm1b)
	require.Nil(t, err)
	require.NotNil(t, p)
	// assert.Equal(t, prev, p.Data.Params)

	// d, err := base64.URLEncoding.DecodeString(p.Msg)
	// require.Nil(t, err)
	// rm2 := &RelayMessage{}

	// _, err = codec.RLP.UnmarshalFromBytes(d, rm2)
	// require.Nil(t, err)
	// assert.EqualValues(t, rm1, rm2)
}

func TestSender_praSegment(t *testing.T) {

}
