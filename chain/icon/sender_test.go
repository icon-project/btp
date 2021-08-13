package icon

import (
	"encoding/base64"
	"testing"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func fakeWallet() wallet.Wallet {
	priKey, _ := crypto.GenerateKeyPair()

	w, err := wallet.NewIcxWalletFromPrivateKey(priKey)
	if err != nil {
		panic(err)
	}
	return w
}

func TestSender_newTransactionParam(t *testing.T) {
	prev := "btp://0x3.icon/cx0c59775cb325c1aea9c872546dbe8365af7c9d07"
	rm := &RelayMessage{
		BlockUpdates:  [][]byte{{1, 2, 3, 4}},
		BlockProof:    []byte{1, 2, 3, 4},
		ReceiptProofs: [][]byte{{1, 2, 3, 4}},
	}

	sender := &sender{l: log.New(), w: fakeWallet(), dst: "btp://0x42.icon/cx1f304314d232db2ea504e84607e8926ed6fa8d91"}
	p, err := sender.newTransactionParam(prev, rm)
	require.NoError(t, err)
	require.NotNil(t, p)

	callData, ok := p.Data.(CallData)
	require.True(t, ok)
	params, ok := callData.Params.(BMCRelayMethodParams)
	require.True(t, ok)
	assert.Equal(t, prev, params.Prev)

	rlpEncoded, err := base64.URLEncoding.DecodeString(params.Messages)
	require.NoError(t, err)

	expected := &RelayMessage{}
	_, err = codec.RLP.UnmarshalFromBytes(rlpEncoded, expected)
	require.NoError(t, err)
	assert.Equal(t, expected, rm)
}

func TestSender_praSegment(t *testing.T) {
	t.Skip()
}
