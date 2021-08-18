package pra

import (
	"path/filepath"

	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/mta"
)

func (r *relayReceiver) prepareDatabase(offset int64, absDir string, relayChainNetworkAddres string) error {
	r.log.Debugln("open database", filepath.Join(absDir, relayChainNetworkAddres))
	database, err := db.Open(absDir, string(db.GoLevelDBBackend), relayChainNetworkAddres)
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

	// A lot of int64 variable should be uint64
	if offset < 0 {
		offset = 0
	}

	r.store = mta.NewExtAccumulator(k, bk, offset)
	if bk.Has(k) {
		if err = r.store.Recover(); err != nil {
			return errors.Wrapf(err, "fail to acc.Recover cause:%v", err)
		}
		// c.log.Debugf("recover Accumulator offset:%d, height:%d", c.store.Offset(), c.store.Height())
		// b.log.Fatal("Stop")
		//TODO sync offset
	}
	return nil
}

func (r *relayReceiver) updateMta(bn uint64, blockHash substrate.SubstrateHash) {
	next := r.store.Height() + 1
	if next < int64(bn) {
		r.log.Fatalf("updateMta: found missing block next:%d bu:%d", next, bn)
		return
	}
	if next == int64(bn) {
		r.log.Debugf("updateMta: syncing Merkle Tree Accumulator at %d", bn)
		r.store.AddHash(blockHash[:])
		if err := r.store.Flush(); err != nil {
			r.log.Fatalf("fail to MTA Flush err:%+v", err)
		}
	}
}
