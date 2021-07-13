package icon

// import (
// 	"encoding/base64"
// 	"testing"

// 	"github.com/icon-project/btp/common/codec"
// 	"github.com/icon-project/btp/common/log"
// 	"github.com/stretchr/testify/assert"
// 	"github.com/stretchr/testify/require"
// )

// func TestSender_NewTransactionParam(t *testing.T) {
// 	prev := "string"
// 	rm1 := &RelayMessage{
// 		BlockUpdates:  [][]byte{{1, 2, 3, 4}},
// 		BlockProof:    []byte{1, 2, 3, 4},
// 		ReceiptProofs: [][]byte{{1, 2, 3, 4}},
// 	}

// 	sender := &sender{l: log.New()}
// 	p, err := sender.newTransactionParam(prev, rm1)
// 	require.Nil(t, err)
// 	require.NotNil(t, p)
// 	assert.Equal(t, prev, p.Prev)

// 	d, err := base64.URLEncoding.DecodeString(p.Msg)
// 	require.Nil(t, err)
// 	rm2 := &RelayMessage{}

// 	_, err = codec.RLP.UnmarshalFromBytes(d, rm2)
// 	require.Nil(t, err)
// 	assert.EqualValues(t, rm1, rm2)
// }
