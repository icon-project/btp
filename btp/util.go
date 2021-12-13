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
			b.log.Panicf("fail to acc.Recover cause:%v", err)
		}
		b.log.Debugf("recover Accumulator offset:%d, height:%d", b.store.Offset(), b.store.Height())
		// b.log.Fatal("Stop")
		//TODO sync offset
	}

	return nil
}

func (b *BTP) receiveHeight() int64 {
	mtaHeight := b.store.Height()
	if b.cfg.syncWithBmvHeight {
		if b.store.Height() < b.bmcLinkStatus.Verifier.Height {
			b.log.Panicf("not allow to sycn with BMV height, local mta height %d not catched up", mtaHeight)
		}
		b.log.Warnf("sync with BMV height, might skip next BTP message")
		return b.bmcLinkStatus.Verifier.Height
	}

	if mtaHeight < b.bmcLinkStatus.Verifier.Offset {
		mtaHeight = b.bmcLinkStatus.Verifier.Offset
	}

	mtaHeight += 1
	receiveHeight := b.bmcLinkStatus.Verifier.LastHeight
	if mtaHeight < receiveHeight {
		receiveHeight = mtaHeight
	}

	return receiveHeight
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

	bp := &chain.BlockProof{
		Header: header,
		BlockWitness: &chain.BlockWitness{
			Height:  at,
			Witness: mta.WitnessesToHashes(w),
		},
	}

	b.log.Debugf("newBlockProof height:%d, at:%d, w:%x", height, at, bp.BlockWitness.Witness)
	return bp, nil
}

func (b *BTP) logRelaying(prefix string, rm *chain.RelayMessage, segment *chain.Segment, segmentIdx int) {
	if segment == nil {
		if len(rm.BlockUpdates) > 0 {
			b.log.Debugf("%s rm:%d bu:%d ~ %d rps:%d",
				prefix,
				rm.Seq,
				rm.BlockUpdates[0].Height,
				rm.BlockUpdates[len(rm.BlockUpdates)-1].Height,
				len(rm.ReceiptProofs))
		} else {
			b.log.Debugf("%s rm:%d bp:%d ~ %d rps:%d",
				prefix,
				rm.Seq,
				rm.BlockProof.BlockWitness.Height,
				rm.ReceiptProofs[0],
				len(rm.ReceiptProofs))
		}

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
