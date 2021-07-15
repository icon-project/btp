package pra

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"testing"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/ethereum/go-ethereum/rpc"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/binding"
	"github.com/icon-project/btp/chain/pra/mocks"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
	mock "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type blockinfo struct {
	BlockNumber            uint64
	Hash                   SubstrateHash
	Header                 SubstrateHeader
	ScaleEncodedHeader     []byte
	MetaData               SubstrateMetaData
	StorageKey             SubstrateStorageKey
	SystemEventsStorageRaw SubstrateStorageDataRaw
	SystemEventsReadProof  ReadProof
}

func readBlockInfoFromAssets(path string, bi *blockinfo) error {
	b, err := ioutil.ReadFile(path)
	if err != nil {
		panic(err)
	}

	return json.Unmarshal(b, &bi)
}

func TestReceiver_ReceiveLoop(t *testing.T) {
	t.Run("should monitor from the given height", func(t *testing.T) {
		subClient := &MockSubstrateClient{}
		r := &Receiver{
			l: log.New(),
			c: &Client{
				subClient:         subClient,
				stopMonitorSignal: make(chan bool),
			},
		}

		bi := &blockinfo{}
		require.NoError(t, readBlockInfoFromAssets("assets/moonbase_blockinfo_243221.json", bi))

		subClient.On("GetFinalizedHead").Return(bi.Hash, nil).Once()
		subClient.On("GetHeader", bi.Hash).Return(&bi.Header, nil).Twice()
		subClient.On("GetBlockHash", uint64(bi.BlockNumber)).Return(bi.Hash, nil).Once()
		subClient.On("GetMetadata", bi.Hash).Return(&bi.MetaData, nil).Once()
		subClient.On("GetStorageRaw", bi.StorageKey, bi.Hash).Return(&bi.SystemEventsStorageRaw, nil).Once()

		err := r.ReceiveLoop(243221, 1, func(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
			assert.EqualValues(t, bu.Height, 243221)
			assert.Equal(t, bi.Hash.Hex(), fmt.Sprintf("0x%x", bu.BlockHash))
			assert.Equal(t,
				fmt.Sprintf("0x%x", bi.ScaleEncodedHeader),
				fmt.Sprintf("0x%x", bu.Header),
			)
			assert.Equal(t, "0xf90140b9013b4b6ca5b74e19d4bc04280edf20a53a4ebe1402cbb2ef7ed9a1611fb8411a33ca56d80e006415ef11020701d83ae5456ccecb3685eb39acc6b52d3e481214d8f1aa6b5465e347562ba9c7c993862047e1d6f020c25ab4139676afbf7170c254e2f36cefe70c046e6d62738060eed538a43e6738f4c560c5d950be96c72ad591f0c16f564c003b5c7b895c0e0466726f6e890101f7b9c5fb3f5b72f937ed511b173e5a39b9fb3ffaa1cc4dd024a4c7c36c7da8610847fec28647d5f0806548f385257170d76cf6e890f7467ef33b36dcc5b9be1b15d0fdb267aa2fce057cc81a0e2397f5a32ffd8637753f7d0c0c0b7289b002dc3f056e6d627301019a1b7069e8aa71015a15925595589999890dba42cb87dae2b5aabfee791bad47d380392c626eef34701ea3a95d7dd4fc891759cb375991aacbc1ee3663742289f800",
				fmt.Sprintf("0x%x", bu.Proof),
			)
			r.StopReceiveLoop()
		}, func() {})

		assert.NoError(t, err)
	})

	t.Run("should call bmc.parseMessage when Parachain emits EVM Log", func(t *testing.T) {
		subClient := &MockSubstrateClient{}
		bmcContract := &mocks.BMCContract{}
		r := &Receiver{
			l: log.New(),
			c: &Client{
				subClient:         subClient,
				stopMonitorSignal: make(chan bool),
				bmc:               bmcContract,
			},
		}

		bi := &blockinfo{}
		require.NoError(t, readBlockInfoFromAssets("assets/moonbase_blockinfo_315553.json", bi))
		subClient.On("GetFinalizedHead").Return(bi.Hash, nil).Once()
		subClient.On("GetHeader", bi.Hash).Return(&bi.Header, nil).Once()
		subClient.On("GetBlockHash", uint64(bi.BlockNumber)).Return(bi.Hash, nil).Once()
		subClient.On("GetHeader", bi.Hash).Return(&bi.Header, nil).Once()
		subClient.On("GetMetadata", bi.Hash).Return(&bi.MetaData, nil).Twice()
		subClient.On("GetStorageRaw", bi.StorageKey, bi.Hash).Return(&bi.SystemEventsStorageRaw, nil).Once()
		// 4 EVM_Logs event
		bmcContract.On("ParseMessage", mock.AnythingOfType("types.Log")).Return(nil, errors.New("abi: could not locate named method or event")).Times(4)

		err := r.ReceiveLoop(315553, 1, func(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
			assert.EqualValues(t, bu.Height, 315553)
			assert.Equal(t, bi.Hash.Hex(), fmt.Sprintf("0x%x", bu.BlockHash))
			assert.Equal(t,
				fmt.Sprintf("0x%x", bi.ScaleEncodedHeader),
				fmt.Sprintf("0x%x", bu.Header),
			)
			assert.Equal(t, "0xf90180b9017bf1e8f0653422859dea6705ec5d86015a3c9f4c7c03eccbcb4bf858682956fb2886421300932c9abc4e9966ecf08dd3a5aa7087b448a88e54d18b3caebdbb2559c3f8744b25385e8e912f3965b85334a4524a6e15b21fb3c1b355d9e64df395b856a635970c046e6d6273802485ca9e9427894cb1864d725977e3c168171daf22bacf64c7ad5e0674c331730466726f6e890201e93015e1d2195ae2d73004a790db2e4bf394e40f14df3e0a2edd9dff0930e8a910ef0e6bfa9d8bb055f94e873f1df551b42df11d2cb053811279a1101e9c03fcf9cfa1b41ba3027ac3bbda9af6685440e8d89f12c7aca5987b334e91dbbaa4aa9356cb1e07577d7374463ec88709ce945f280b072b89f4bc8c0e70abf4b9b267c090d9ec032f7d7121f9bc2b2a8b9a6a06fd7f434cbebb963d6cfcb2e4206d2f43056e6d627301019cb74fccc8c86d67b5766b1c035e16ed4de18ecd091d4dd8724185eb28dbd1612983e904aafb984f05bd27443b6f8a56fbea955e208718d02e52eb28d0e62e89f800",
				fmt.Sprintf("0x%x", bu.Proof),
			)
			r.StopReceiveLoop()
		}, func() {})

		assert.NoError(t, err)
	})

	t.Run("should build StateProof when EVM Log events contains BMC SendMessage Event", func(t *testing.T) {
		subClient := &MockSubstrateClient{}
		ethClient := ethclient.NewClient(&rpc.Client{})

		bmc, err := binding.NewBMC(EvmHexToAddress("0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090"), ethClient)
		require.NoError(t, err)

		r := &Receiver{
			l:   log.New(),
			src: "btp://0x501.pra/0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090",
			c: &Client{
				subClient:         subClient,
				ethClient:         ethClient,
				stopMonitorSignal: make(chan bool),
				bmc:               bmc,
			},
			rxSeq: 10,
		}

		bi := &blockinfo{}
		require.NoError(t, readBlockInfoFromAssets("assets/moonriverlocal_blockinfo_143004.json", bi))
		subClient.On("GetFinalizedHead").Return(bi.Hash, nil).Once()
		subClient.On("GetHeader", bi.Hash).Return(&bi.Header, nil).Once()
		subClient.On("GetBlockHash", uint64(bi.BlockNumber)).Return(bi.Hash, nil).Once()
		subClient.On("GetHeader", bi.Hash).Return(&bi.Header, nil).Once()
		subClient.On("GetMetadata", bi.Hash).Return(&bi.MetaData, nil).Twice()
		subClient.On("GetStorageRaw", bi.StorageKey, bi.Hash).Return(&bi.SystemEventsStorageRaw, nil).Once()
		subClient.On("GetReadProof", bi.StorageKey, bi.Hash).Return(bi.SystemEventsReadProof, nil).Once()

		err = r.ReceiveLoop(143004, 1, func(bu *chain.BlockUpdate, rps []*chain.ReceiptProof) {
			assert.EqualValues(t, bu.Height, 143004)
			assert.Equal(t, bi.Hash.Hex(), fmt.Sprintf("0x%x", bu.BlockHash))
			assert.Equal(t,
				fmt.Sprintf("0x%x", bi.ScaleEncodedHeader),
				fmt.Sprintf("0x%x", bu.Header),
			)
			assert.Equal(t, "0xf8d8b8d4b02aebd1719933b78c76c3f51f6e1b2f760162f3b2f43c6818367dbadb8f7c2072ba0800049ac1e8cddb4d4de2fbe62e180ce05a2b329069f863a9855eb64534a846fa0cd7397f52d2d32573926eee56ca9a35f0c90d1c9a9610e21916c9b8aff1b9ba7608046e6d627380d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d0466726f6e0901010db4dc2444f0ac86a3202819682e13bb5fd9d70ce01006f67720c1628f04745604bfafb68beb571aac735e8be629f3a13eb6e0263b873a9039f1103164da6eefb4f800",
				fmt.Sprintf("0x%x", bu.Proof),
			)
			assert.NotEmpty(t, rps)
			assert.Contains(t, rps, &chain.ReceiptProof{
				Height: 143004,
				Proof:  types.MustHexDecodeString("f90896a026aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7f90872b901b080bfbd80e16f32b67a0579c88994791dc3db528a66e97bcd0460e94168912d7f98999a5a80f507383c1a5b44215ef422525bbeed0208fb631ddc8494ab33fa5144dfe3cb1880848c6f130da0e4ba2831d911e7f94f5df0af1c2d87fcd6ab69eb607833b248068064028242b4b067fb715c603ac8b70963296c8b75c29a1db23f195961c80c6ca480818cdff7b610ec05babcf7c379845b78c31c0f3fb42b3e209c1f24fd3b2ebc6e80d6e50599a85ef275be468f045ad09d66a00e6f69fbfe44482b8499396afe67b5809975559fe90be147370108309674712f62d02e9a2170cd6f63957a07ef6dd9f3806d67c13fdf18e0aed92322b40bd367fabe37c157d965a7a1ec5ee912112010a680454c28d7219597d5d3f13c235ad55bb9cc064760edf04c42de47b8202e2615a580d522a107c8b04c4b84e7dea1a353e90fbaf466e208529439c8b26e512b5619f58020347dd0267217c36a11955d478ba7368ab1b0c09d43d8797b32d3518fd6b31e804b4a89a30f8cd394ef83edb3db4059bc5eecb42d4ff8a6487e8004f4eacf1a8280c657c9ef3150faf8c13c7b8b87aad40e59bb0fd9479a96f46d41ff57e2c3ab1db845804100805b3792e813e83bdec90390bda4c8ffbeb2f2fe379dab660460e54bfa8dbd18f080864c5866c5953a1b760fc88cbefe851c4409850648c85d8dc767742571c4c59bb9050b5ed41e5e16056765bc8461851072c9d7e51320000000000000005095a20900000000020000000100000000000000000000000000020000000200000000000000000000000000020000000300000003026be02d1d3665660d22ff9624b7be0551ee1ac91b171618360f31f94502475e6ccce1da870230baf750c300000000000000000000000000000000030000000a005b5b619e6a040ebcb620155e0aaae89afa45d0900437be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b81070000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000396274703a2f2f3078332e69636f6e2f6378386562323438343961376365623136623866613533376635613862333738633661663461303234370000000000000000000000000000000000000000000000000000000000000000000000000000f8f8f6b83a6274703a2f2f30783530312e7072612f307835623542363139453641303430454243423632303135354530614141653839416641343544303930b8396274703a2f2f3078332e69636f6e2f6378386562323438343961376365623136623866613533376635613862333738633661663461303234378c436f696e5472616e7366657208b86ff86d00b86af868b3307836626530326431643336363536363064323266663936323462376265303535316565316163393162000000000000000000aa687838303632303736616135653638663032313132316431633362346233393739643231613664636165c8c78344455682c15c00000000000000000000030000000a00a7fb3825a1464194b7e4a417b8ce837af12401e80850d22373bb84ed1f9eeb581c913e6d45d918c05f8b1d90f0be168f06a4e6994a0000000000000000000000006be02d1d3665660d22ff9624b7be0551ee1ac91b81060000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000396274703a2f2f3078332e69636f6e2f68783830363230373661613565363866303231313231643163336234623339373964323161366463616500000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000c15c00000000000000000000000000000000000000000000000000000000000001f4000000000000000000000000000000000000000000000000000000000000000344455600000000000000000000000000000000000000000000000000000000000000030000000b006be02d1d3665660d22ff9624b7be0551ee1ac91b0000000000000000000000000000000000000000bfafb68beb571aac735e8be629f3a13eb6e0263b873a9039f1103164da6eefb400010000030000000000b8c0ef9402000000000000b9010a9eaa394eea5630e07c48ae0c9558cef7298f80e8c1531d6476930c47c4bcb835e0ba58be5c24785f48ca4b5d08a9b6949bc5e880fc40af17e2f1a6d8b4e595d125181f0f444b7ac1ae0fd48efec63f1cbb002cfe4c5f0684a022a34dd8bfa2baaf44f172b7100401805d3bfe2ed743dbb5ac92838915d386ed68f1cf5729b750b0e110587ac65bd03080f316b8d2db94c316adfa92c178aeb9b7ca551802afc3089c1b4f442937efecac80a15a30d0238e47107e5d0b7ffd52f1de119ec1a5fbdf104719f6372c271cf2968027b86bc37cf47f15ed6a31455eaf487d00432956170bff410e2c6d33c39ee6f8745f09cce9c888469bb1a0dceaa129672ef82c6d02206d6f6f6e62617365b85b8081048022fd5d3bb4b98a0ffa515fe3c0937d5b1cbb81a8f2bb1a2c62c1ebe3e8a953a1545e8d434d6125b40443fe11fd292d13a41003000000807da6c2d85303f0122b5e3e33633cdf9d6ec96f170af73a7ae3ef19f554dcfdfe"),
			})
			r.StopReceiveLoop()
		}, func() {})

		assert.NoError(t, err)
	})
}
