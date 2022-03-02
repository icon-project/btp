package near

import (
	"encoding/hex"
	"fmt"
	"testing"

	"github.com/btcsuite/btcutil/base58"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/base"
	"github.com/icon-project/btp/chain/near/testdata/mock"

	com "github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
)

func TestReceiver_ReceiveLoop(t *testing.T) {

	t.Run("should monitor from the given height", func(t *testing.T) {
		mockApi := NewMockApi(func() mock.Storage {
			blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828", "377829", "377830", "377831"})
			receiptProofMap := mock.LoadReceiptsFromFile([]string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G", "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y"})
			lightClientMap := mock.LoadLightClientBlocksFromFile([]string{"DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c", "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w", "9HhNTVZBu7sBFFsn4dCFnJehVXvMUJ8TDLXDffRJa1hz", "4CSFBudwkUAgoHHQNh6UnVx78EP9ubeUhbc9ZHoC5w4u", "3m8kMqwyMqVSkaFn4LrGim4qy6RdNVVxx3ebp11btHFd", "HR437ytW77eMcYdNw9tLghraEts1bkjCWHe1AtgHR7Zm"})
			return mock.Storage{
				BlockByHeightMap: blockByHeightMap,
				BlockByHashMap:   blockByHashMap,
				LatestBlockHeight: mock.Response{
					Reponse: 377831,
				},
				ReceiptProofMap:     receiptProofMap,
				LightClientBlockMap: lightClientMap,
			}
		}())
		client := &Client{
			api: &mockApi,
		}
		base.RegisterClients([]string{"near"}, client)

		nearClient, err := base.GetClient("near")
		if err != nil {
			panic(err)
		}

		var logger com.Logger
		endpoint := ""
		source := chain.BtpAddress("btp://0x1.near/53b3d03550bdd3367a686f4f19333e98b61ae6ebb259d906696617305c7bcf7a")
		destination := chain.BtpAddress("btp://0x58eb1c.icon/cx9c072bef545abeb96a3f7ba2ffdf8b861fb55dea")

		receiver := base.NewReceiver(source, destination, endpoint, nil, logger, nearClient)
		err = receiver.ReceiveLoop(377826, 0, func(blockUpdate *chain.BlockUpdate, receiptProof []*chain.ReceiptProof) {

			if blockUpdate.Height == 377826 {

				assert.Equal(t, base58.Encode(blockUpdate.BlockHash), "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w")

				assert.Equal(t, hex.EncodeToString(blockUpdate.Header), "f9032ba0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c6987f8d38305c3e2a0dd4bfddd18ff0547be029ee48eb24fc57e535611f9f6c5dac1d19697354bc96da069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a094712793fc2575fd39fbedd43fe0a7319040d15c747fdff10ee40371eb3edd13a066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f29258816bcde70f77ecdb7a001213e665fbb68d07df6fbb5267633572e263dff5e24ff10c75dcc61697f27f7a015c8be8d2f6acd34f24b6d08049bd868113dbd20bbd298b6759f71773ee1f171f901efa07a4fde36a426deaf5e1252f5718a06e1787f8d12cb7da5a18057fa9ffb66ce0da099ed696060a422df2eda0c2db8c1151f308381f24f9c6f97b50e3fa7d6c7810ca066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925a00000000000000000000000000000000000000000000000000000000000000000a084e391895fdb2976acfc814a8f105b350bac500fa69dd372143d087dbde132c7c0c1018a31303030303030303030a235303033363733373934313438383837343734343037373030303032323832303332c0a069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c69878305c3d08305c3e1f800f8c9b841007bba48efa26e408cf80a2f55ff3961581866a8827b3a59b9022a0c1a24ea9288c8adc649d498123dae2a028ef8072bf61d00faee9300ac63293736ba7b314203b84100cae2e0262d7847a536d5ea00043d7423be38523fea104434cdf9fee47e134e362dfc4df09d2b186f0bf9a5c404fcfd3cf1d4fae3e82df71cc5d38696673c8a00b84100486035608e5cef6820b6cf020faa6f573ff5344bab146c572872c4142168ca81d4f5986daf59394b03c7035725e744e4877f16accedd2319722ed399521b380f31b841008edc3a524ba0b31fe08452f55b83ad3cb69d7fa3182add3adbeec561abe3d48fed621ccbc6c8cb140ee42a30023a1a8ca1051531fc71e4b37720e02cd55ae809")

				receiver.StopReceiveLoop()
			}
		}, func() { fmt.Println("Connect ReceiveLoop") })
		assert.NoError(t, err)
	})

	t.Run("should build receipt proof for the events", func(t *testing.T) {
		mockApi := NewMockApi(func() mock.Storage {
			blockByHeightMap, blockByHashMap := mock.LoadBlockFromFile([]string{"377825", "377826", "377827", "377828", "377829", "377830", "377831"})
			receiptProofMap := mock.LoadReceiptsFromFile([]string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G", "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y"})
			lightClientMap := mock.LoadLightClientBlocksFromFile([]string{"DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c", "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w", "9HhNTVZBu7sBFFsn4dCFnJehVXvMUJ8TDLXDffRJa1hz", "4CSFBudwkUAgoHHQNh6UnVx78EP9ubeUhbc9ZHoC5w4u", "3m8kMqwyMqVSkaFn4LrGim4qy6RdNVVxx3ebp11btHFd", "HR437ytW77eMcYdNw9tLghraEts1bkjCWHe1AtgHR7Zm"})
			return mock.Storage{
				BlockByHeightMap: blockByHeightMap,
				BlockByHashMap:   blockByHashMap,
				LatestBlockHeight: mock.Response{
					Reponse: 377831,
				},
				ReceiptProofMap:     receiptProofMap,
				LightClientBlockMap: lightClientMap,
			}
		}())
		client := &Client{
			api: &mockApi,
		}
		base.RegisterClients([]string{"near"}, client)

		nearClient, err := base.GetClient("near")
		if err != nil {
			panic(err)
		}
		LatestBlockHeight = 377840

		var logger com.Logger
		endpoint := ""

		source := chain.BtpAddress("btp://0x1.near/53b3d03550bdd3367a686f4f19333e98b61ae6ebb259d906696617305c7bcf7a")
		destination := chain.BtpAddress("btp://0x58eb1c.icon/cx9c072bef545abeb96a3f7ba2ffdf8b861fb55dea")

		receiver := base.NewReceiver(source, destination, endpoint, nil, logger, nearClient)

		err = receiver.ReceiveLoop(377826, 0, func(blockUpdate *chain.BlockUpdate, receiptProof []*chain.ReceiptProof) {

			if blockUpdate.Height == 377826 {

				assert.Equal(t, base58.Encode(blockUpdate.BlockHash), "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w")

				assert.Equal(t, hex.EncodeToString(blockUpdate.Header), "f9032ba0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c6987f8d38305c3e2a0dd4bfddd18ff0547be029ee48eb24fc57e535611f9f6c5dac1d19697354bc96da069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a094712793fc2575fd39fbedd43fe0a7319040d15c747fdff10ee40371eb3edd13a066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f29258816bcde70f77ecdb7a001213e665fbb68d07df6fbb5267633572e263dff5e24ff10c75dcc61697f27f7a015c8be8d2f6acd34f24b6d08049bd868113dbd20bbd298b6759f71773ee1f171f901efa07a4fde36a426deaf5e1252f5718a06e1787f8d12cb7da5a18057fa9ffb66ce0da099ed696060a422df2eda0c2db8c1151f308381f24f9c6f97b50e3fa7d6c7810ca066687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925a00000000000000000000000000000000000000000000000000000000000000000a084e391895fdb2976acfc814a8f105b350bac500fa69dd372143d087dbde132c7c0c1018a31303030303030303030a235303033363733373934313438383837343734343037373030303032323832303332c0a069011faf3f21710b1e1cadccbaf8f155a40ff96043203a0b2920dacb46028aa4a0b5867c7d6fc0a6d5f21016d1af16d0d136842335ea38b969754ee4a22d7c69878305c3d08305c3e1f800f8c9b841007bba48efa26e408cf80a2f55ff3961581866a8827b3a59b9022a0c1a24ea9288c8adc649d498123dae2a028ef8072bf61d00faee9300ac63293736ba7b314203b84100cae2e0262d7847a536d5ea00043d7423be38523fea104434cdf9fee47e134e362dfc4df09d2b186f0bf9a5c404fcfd3cf1d4fae3e82df71cc5d38696673c8a00b84100486035608e5cef6820b6cf020faa6f573ff5344bab146c572872c4142168ca81d4f5986daf59394b03c7035725e744e4877f16accedd2319722ed399521b380f31b841008edc3a524ba0b31fe08452f55b83ad3cb69d7fa3182add3adbeec561abe3d48fed621ccbc6c8cb140ee42a30023a1a8ca1051531fc71e4b37720e02cd55ae809")

				assert.EqualValues(t, receiptProof[0].Height, 377826)
				assert.Equal(t, string(receiptProof[0].Events[0].Message), "+MmaYnRwOi8vMHgxLm5lYXIvYWxpY2Uubm9kZTG4OWJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YoNibWOBgLhr+GkVuGZCTUNSZXZlcnRVbnJlYWNoYWJsZSBhdCBidHA6Ly8weDUucHJhLzg4YmQwNTQ0MjY4NmJlMGE1ZGY3ZGEzM2I2ZjEwODllYmZlYTM3NjliMTlkYmIyNDc3ZmUwY2Q2ZTBmMTI2ZTQ=")
				receiver.StopReceiveLoop()
			}
		}, func() { fmt.Println("Connect ReceiveLoop") })

		assert.NoError(t, err)
	})

}
