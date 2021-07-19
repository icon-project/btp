package substrate

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
	// curl -X POST -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"grandpa_proveFinality", "params": [8007753], "id":50}' https://kusama-rpc.polkadot.io | jq -rj '.result' > chain/pra/assets/kusama_encoded_finalityproofs
	buf, err := ioutil.ReadFile(filepath.Join(dir, "./assets/kusama_encoded_finalityproofs"))
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
	assert.Equal(t, fp.Justification.EncodedJustification.Round, types.NewU64(5152))
	assert.Equal(t, fp.Justification.EncodedJustification.Commit.TargetHash.Hex(), "0x7412c2d47c83266de081fbc0efc5c103a06ac6afbd93ec932df0aee80f0df856")
	assert.Equal(t, fp.Justification.EncodedJustification.Commit.TargetNumber, types.NewU32(8010648))
}
