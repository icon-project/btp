package pra

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/stretchr/testify/assert"
)

func TestDecodeFinalityProof(t *testing.T) {
	dir, err := os.Getwd()
	assert.NoError(t, err)
	// From command
	// curl -X POST -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"grandpa_proveFinality", "params": [8007753], "id":50}' https://kusama-rpc.polkadot.io
	buf, err := ioutil.ReadFile(filepath.Join(dir, "./kusama_encoded_finalityproofs"))
	assert.NoError(t, err)
	hexStr := string(buf)

	/**
	polkadot js type
	[
	  {
	    "Justification": "(ConsensusEngineId, GrandpaJustification)"
	  },
	  {
	    "FinalityProof": {
	      "block": "Hash",
	      "justification": "Justification",
	      "unknown_headers": "Vec<Header>"
	    }
	  }
	]
	**/
	fp := &FinalityProof{}
	err = types.DecodeFromHexString(hexStr, fp)
	assert.NoError(t, err)
	assert.Equal(t, fp.Justification.EncodedJustification.Round, types.NewU64(3714))
	assert.Equal(t, fp.Justification.EncodedJustification.Commit.TargetHash.Hex(), "0x61761bf6a47c5429e24d3562294f5f4ddd8f3bea3162a435671b28ce012035c8")
	assert.Equal(t, fp.Justification.EncodedJustification.Commit.TargetNumber, types.NewU32(8009645))
	assert.Len(t, fp.Justification.EncodedJustification.Commit.Precommits, 607)
}
