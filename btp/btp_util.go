package btp

import (
	"path/filepath"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/mta"
)

func (b *BTP) prepareDatabase(offset int64) error {
	b.log.Debugln("open database", filepath.Join(b.cfg.AbsBaseDir(), b.bmcDstBtpAddress.NetworkAddress()))
	database, err := db.Open(b.cfg.AbsBaseDir(), string(DefaultDBType), b.bmcDstBtpAddress.NetworkAddress())
	if err != nil {
		return errors.Wrap(err, "fail to open database")
	}
	defer func() {
		if err != nil {
			database.Close()
		}
	}()

	var bk db.Bucket
	if bk, err = database.GetBucket("Accumulator"); err != nil {
		return err
	}
	k := []byte("Accumulator")
	if offset < 0 {
		offset = 0
	}

	b.store = mta.NewExtAccumulator(k, bk, offset)
	if bk.Has(k) {
		if err = b.store.Recover(); err != nil {
			return errors.Wrapf(err, "fail to acc.Recover cause:%v", err)
		}
		b.log.Debugf("recover Accumulator offset:%d, height:%d", b.store.Offset(), b.store.Height())
		//TODO sync offset
	}
	return nil
}

func (b *BTP) receiveHeight() int64 {

	max := b.store.Height()
	if max < b.bmcLinkStatus.Verifier.Offset {
		max = b.bmcLinkStatus.Verifier.Offset
	}

	max += 1
	min := b.bmcLinkStatus.Verifier.LastHeight
	if max < min {
		min = max
	}
	return min
}

// newRelayMessage initializes an empty RelayMessage into the bufferred rms
func (b *BTP) newRelayMessage() *chain.RelayMessage {
	rm := &chain.RelayMessage{
		From:         b.bmcSrcBtpAddress,
		BlockUpdates: make([]*chain.BlockUpdate, 0),
		Seq:          b.rmSeq,
	}
	b.rms = append(b.rms, rm)
	b.rmSeq++

	return rm
}

// newBlockProof creates a new BlockProof
func (b *BTP) newBlockProof(height int64, header []byte) (*chain.BlockProof, error) {
	at, w, err := b.store.WitnessForAt(height, b.bmcLinkStatus.Verifier.Height, b.bmcLinkStatus.Verifier.Offset)
	if err != nil {
		return nil, err
	}

	b.log.Debugf("newBlockProof height:%d, at:%d, w:%d", height, at, len(w))
	bp := &chain.BlockProof{
		Header: header,
		BlockWitness: &chain.BlockWitness{
			Height:  at,
			Witness: mta.WitnessesToHashes(w),
		},
	}
	return bp, nil
}

func (b *BTP) logRelaying(prefix string, rm *chain.RelayMessage, segment *chain.Segment, segmentIdx int) {
	if segment == nil {
		b.log.Debugf("%s rm:%d bu:%d ~ %d rps:%d",
			prefix,
			rm.Seq,
			rm.BlockUpdates[0].Height,
			rm.BlockUpdates[len(rm.BlockUpdates)-1].Height,
			len(rm.ReceiptProofs))
	} else {
		b.log.Debugf("%s rm:%d [i:%d,h:%d,bu:%d,seq:%d,evt:%d,txh:%v]",
			prefix,
			rm.Seq,
			segmentIdx,
			segment.Height,
			segment.NumberOfBlockUpdate,
			segment.EventSequence,
			segment.NumberOfEvent,
			segment.GetResultParam)
	}
}
