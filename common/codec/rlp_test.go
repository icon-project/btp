package codec_test

// import (
// 	"testing"

// 	"github.com/icon-project/btp/common/codec"
// 	"github.com/stretchr/testify/require"
// )

// type TailAsBool struct {
// 	head []byte
// 	tail bool
// }

// func TestTailAsBool(t *testing.T) {
// 	b, err := codec.RLP.MarshalToBytes(TailAsBool{
// 		head: []byte{0},
// 		tail: true,
// 	})

// 	require.NoError(err)
// 	t.Logf("%x", b)
// }
