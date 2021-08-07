package substrate

import (
	"testing"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestNewNewEncodedVoteMessage(t *testing.T) {
	hash, err := types.NewHashFromHexString("0x6521df611a96c354e6937d8c88cc5d37ab8b0d4254b48e7dade360843d71343c")
	require.NoError(t, err)
	rawByte, err := NewEncodedVoteMessage(VoteMessage{
		Message: SignedMessageEnum{
			IsPrecommit: true,
			AsPrecommit: SignedMessage{
				TargetHash:   hash,
				TargetNumber: types.NewU32(1045791),
			},
		},
		Round: types.NewU64(123),
		SetId: types.NewU64(123),
	})

	assert.NoError(t, err)
	rawHex := types.HexEncodeToString(rawByte)
	assert.Equal(t, rawHex, "0x016521df611a96c354e6937d8c88cc5d37ab8b0d4254b48e7dade360843d71343c1ff50f007b000000000000007b00000000000000")
}
