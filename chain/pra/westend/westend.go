package westend

import (
	"github.com/icon-project/btp/chain/pra/kusama"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
)

func NewWestendEventRecord(sdr *substrate.SubstrateStorageDataRaw, meta *substrate.SubstrateMetaData) *WestendEventRecord {
	records := &WestendEventRecord{}
	if err := substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		log.Debugf("NewWestendEventRecord decode fails: %v", err)
		return nil
	}

	return records
}

type WestendEventRecord struct {
	kusama.KusamaEventRecord
}
