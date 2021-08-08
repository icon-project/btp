package moonbase

import (
	"github.com/icon-project/btp/chain/pra/moonriver"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/log"
)

func NewMoonbaseEventRecord(sdr *substrate.SubstrateStorageDataRaw, meta *substrate.SubstrateMetaData) *MoonbaseEventRecord {
	records := &MoonbaseEventRecord{}
	if err := substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		log.Debugf("NewWestendEventRecord decode fails: %v", err)
		return nil
	}

	return records
}

type MoonbaseEventRecord struct {
	moonriver.MoonriverEventRecord
}
