package pra

import (
	"testing"

	types "github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
)

// TODO: modify this
func NewTestClient() *Client {
	const uri = "wss://beresheet1.edgewa.re"
	return NewClient("wss://beresheet1.edgewa.re", "0xa3a83D4C5f453C62808f33fc0278d547aeB3F0C0", log.New())
}

func TestGetReadProof(t *testing.T) {
	c := NewTestClient()

	// GIVEN and NewClient
	key, _ := types.HexDecodeString("0x26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7")
	hash, _ := types.NewHashFromHexString("0x2db4d2e7b3a3a9f19470746197eac3c733f59328794aca4c86a05ee83bdff041")

	// WHEN
	rp, err := c.getReadProof(SubstrateStorageKey(key), SubstrateHash(hash))
	if err != nil {
		t.Errorf("GetReadProof(%#x, %#x), expect no error, but got %v", key, hash, err)
	}

	// THEN
	proof := []string{
		"0x5ed41e5e16056765bc8461851072c9d72d0108000000000000005095a20900000000020000012000679c5100b79488128197b10300000000f07c3f006d6f646c70792f7472737279000000000000000000000000000000000000000000",
		"0x9eaa394eea5630e07c48ae0c9558cef7298f8002309720446f18d9f42534c78e5ff5ac1134a83f068c9234040edc10a88f9ee6801dea69e612b114e29ced7ff2d734d13dee2805916b4001a907c36e683ecf621b4c5f0684a022a34dd8bfa2baaf44f172b710040180400d96a5a87522d428e14e18e2d7c1145596560075304cc9ef98f82523f9195780acf929401c9f3900f6e45a2871be66d72e936f806ec0a8bddaaab18b0bef32998019f91c8ac97adf4b38abe9ff586f8dcc7a9cdc36663ef00538482816dea3b5c8803be2bcdbd9327f1257cfc5943ebe1e1cb8d0158d080e0de6126fe9069616ae7b705f09cce9c888469bb1a0dceaa129672ef828b8206564676577617265",
		"0x80499c8017d1d8028c29d5a7e53c75f39627be151d76433d4bbcedcbcf9bd4986aa9a01180c31a6403534bc7c4edd155aa6d5a09a05d57ce80906de1dd072a37df919975d28050afa3b7ffbc5675352d4adfd570ab4133a85bed7cfc1f235596baaf97790a4c80133069e7149bb988cacd53e12e67fa96025aefb387594d3fa2191af093361adf80aa66db766a8b1c584ad7e66fcd4185e644ea9b6c834d376d0ef9eae3d6afaba48097e33c7aa72b7b831a4f73fe0f95f7b4b77c7ff74ce75d58147019be46dc763280a3ae4a2f5ca778d4e343636a7cb1e388602496ef1ec7978311c6bd8ea08acdbf",
		"0x80fefd8050aac1808559ea05dd9749000294fba1e70b0a13f9c3949d04e440ea895af71180c20ec85ab58d3c898f5598e6ed9ffe7db2f33d05db61de74ec3c6de5137ffb1580518a5a9bba95dbf785bafb912c0dcca9c3a7079d407ac0d9f6febf10f93b97ee8053b4066b71808e56c09799aeca7112201c0d7901cf2ded014f6ce229ccb7e78880eb573fa1f800ac931e89e90ad59f20d701702c2a073bc261a5c794431165323880e3bda25f397d6a92ae814be6fb9e271057e8aa6c0f2b3b21fa5bbf2f75c9631b80e28918f2a631507ae9f15db86776ae29e67c1142e49924e944ef57eb2521d1d480a2ff086115772f64684940f8f8815ab5881788e1a5a994dd7d751bc68f259b7280a3e9fa535ab5e4f01519ca4adf555e8b2fda76c30b48d6663d5edc4f5704533280df8bb702967ac0b94c5a50ae75c949dd1a42e35f2a7729a1a46a640e3cedbae6805810e7556924a73fec305f26f34e6967f6b6c76db69cced5d2ae4e1ad561e82f80dc0a86bda2e5d017638fad02a0780324f683a49e190ed0cabe301da8e8f4e1fc803c7dd0467c8008bd426b16601ffd07ac76daf2c5b0d0f3785da9f4413b1b9b3e809debbe06b5755d94db2cb7ba559924d22b9803643f7e273c7faeb47272646e01",
		"0x80810480573b08178f23a4707f5d0808d73b610c904cfbf3cbdde1186a496380be628166545e8d434d6125b40443fe11fd292d13a4100200000180cda5210990517dd74eae026c58a0c948da09547f8c2a662e2aff0b6463221703",
	}
	assert.Equal(t, rp.At, hash, "Should same hash")
	assert.ElementsMatch(t, rp.Proof, proof, "Should same read proofs array elements")
}

func TestGetEvents(t *testing.T) {
	c := NewTestClient()

	hash, err := c.subAPI.RPC.Chain.GetBlockHashLatest()
	if err != nil {
		panic(err)
	}
	events, err := c.getEvents(SubstrateHash(hash))

	if err != nil {
		t.Errorf("GetEvents(%#x), expect no error, but got %v", hash, err)
	}

	assert.NotEmpty(t, events, "GetEvents(%+v), always has events, but got %v", hash, err)
	t.Logf("%+v", events)
}
