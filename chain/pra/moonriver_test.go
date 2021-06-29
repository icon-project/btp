package pra

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"

	types "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
)

func TestMoonriverEventRecord1(t *testing.T) {
	dir, err := os.Getwd()
	assert.NoError(t, err)
	// curl -X POST -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"chain_getFinalizedHead","id":50}' https://rpc.testnet.moonbeam.network | jq -r '.result' | xargs -I {} curl -X POST -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"state_getMetadata","params": ["{}"],"id":50}' https://rpc.testnet.moonbeam.network | jq -rj '.result' > chain/pra/assets/moonbeam_metadata
	buf, err := ioutil.ReadFile(filepath.Join(dir, "./assets/moonbeam_metadata"))
	assert.NoError(t, err)
	hexStr := string(buf)

	meta := &types.Metadata{}
	err = types.DecodeFromHexString(hexStr, meta)
	assert.NoError(t, err)

	// curl -X POST -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"state_getStorage","params":["0x26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7", "0xfb611b0c747f35e1e2248dff111c2f151aefabd91099205b6efafa0590e59d10"],"id":50}' https://rpc.testnet.moonbeam.network | jq -rj '.result' > chain/pra/assets/moonbeam_system_event_1
	buf1, err := ioutil.ReadFile(filepath.Join(dir, "./assets/moonbeam_system_event_1"))
	assert.NoError(t, err)
	hexStr1 := string(buf1)
	b, err := types.HexDecodeString(hexStr1)
	assert.NoError(t, err)

	// Decode the event records
	events := MoonriverEventRecord{}
	err = types.EventRecordsRaw(b).DecodeEventRecords(meta, &events)
	assert.NoErrorf(t, err, "Key: %s\t\tRaw: %x\t\tBlock: %s", "0x26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7", b, "0xfb611b0c747f35e1e2248dff111c2f151aefabd91099205b6efafa0590e59d10")
}
