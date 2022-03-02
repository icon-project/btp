package pra

import (
	"encoding/hex"
	"fmt"
	"testing"

	"github.com/icon-project/btp/chain/pra/substrate"
	iconcrypto "github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/log"
	"github.com/stretchr/testify/assert"
)

func hashSubstrateInt(number uint64) substrate.SubstrateHash {
	return substrate.NewSubstrateHashFromHexString(hex.EncodeToString(iconcrypto.SHA3Sum256([]byte(fmt.Sprintf("%d", number)))))
}

func TestClientMonitorBlock(t *testing.T) {

	t.Run("monitor from lower finallized header", func(t *testing.T) {
		startBlock := uint64(1)
		finalizedBlock := uint64(5)
		endBlock := uint64(finalizedBlock + 1)

		hashMap := map[uint64]substrate.SubstrateHash{}
		hashInt := map[substrate.SubstrateHash]uint64{}
		subClient := &substrate.MockSubstrateClient{}

		for i := startBlock; i <= finalizedBlock+1; i++ {
			hash := hashSubstrateInt(i)
			hashMap[i] = hash
			hashInt[hash] = i

			subClient.On("GetBlockHash", i).Return(hash, nil).Once()

			header := &substrate.SubstrateHeader{Number: substrate.SubstrateBlockNumber(i)}
			subClient.On("GetHeader", hash).Return(header, nil)
		}
		subClient.On("GetFinalizedHead").Return(hashMap[finalizedBlock], nil).Times(int(finalizedBlock-startBlock) + 1)
		subClient.On("GetFinalizedHead").Return(hashMap[endBlock], nil).Once()

		monitoredBlocks := []int64{}

		client := &Client{log: log.New(), subClient: subClient, stopMonitorSignal: make(chan bool)}
		client.MonitorBlock(startBlock, true, func(v *BlockNotification) error {
			assert.Equal(t, v.Hash, hashMap[v.Height])
			assert.NotNil(t, v.Header)
			assert.EqualValues(t, v.Height, v.Header.Number)

			monitoredBlocks = append(monitoredBlocks, int64(v.Height))
			if v.Height == endBlock {
				client.CloseAllMonitor()
			}
			return nil
		})

		assert.Len(t, monitoredBlocks, int(endBlock-startBlock)+1)
	})

	t.Run("monitor from heigher finallized header", func(t *testing.T) {
		startBlock := uint64(3)
		endBlock := uint64(10)
		finalizedBlock := uint64(1)

		hashMap := map[uint64]substrate.SubstrateHash{}
		hashInt := map[substrate.SubstrateHash]uint64{}
		subClient := &substrate.MockSubstrateClient{}

		for i := finalizedBlock; i <= endBlock; i++ {
			hash := hashSubstrateInt(i)
			hashMap[i] = hash
			hashInt[hash] = i

			subClient.On("GetFinalizedHead").Return(hashMap[i], nil).Once()

			header := &substrate.SubstrateHeader{Number: substrate.SubstrateBlockNumber(i)}
			subClient.On("GetHeader", hashMap[i]).Return(header, nil)

			if i >= startBlock {
				subClient.On("GetBlockHash", i).Return(hash, nil).Once()
			}
		}

		monitoredBlocks := []int64{}

		client := &Client{log: log.New(), subClient: subClient, stopMonitorSignal: make(chan bool)}
		client.MonitorBlock(startBlock, true, func(v *BlockNotification) error {
			assert.Equal(t, v.Hash, hashMap[v.Height])
			assert.NotNil(t, v.Header)
			assert.EqualValues(t, v.Height, v.Header.Number)

			monitoredBlocks = append(monitoredBlocks, int64(v.Height))
			if v.Height == endBlock {
				client.CloseAllMonitor()
			}
			return nil
		})

		assert.Len(t, monitoredBlocks, int(endBlock-startBlock)+1)
	})
}
