package mta

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
)

func TestMTAccumulator_Basic(t *testing.T) {
	mdb := db.NewMapDB()
	bk, _ := mdb.GetBucket("")

	a := &Accumulator{
		KeyForState: []byte("a"),
		Bucket:      bk,
	}
	if a.Len() != 0 {
		t.Errorf("Length is not zero")
	}
	data := []string{
		"dog", "cat", "elephant", "bird", "monkey", "lion", "tiger",
	}
	for i, d := range data {
		bs := []byte(d)
		w := a.AddData(bs)
		t.Logf("Accumulator:%s", a)
		assert.NoError(t, a.Verify(w, crypto.SHA3Sum256(bs)))
		assert.NoError(t, a.Flush())
		for j := int64(0); j <= int64(i); j++ {
			bs := []byte(data[j])
			w, err := a.WitnessFor(j)
			assert.NoError(t, err)
			t.Logf("WitnessFor[%d]:%v", j, w)
			hs := WitnessesToHashes(w)
			w = HashesToWitness(hs, j)
			assert.NoError(t, a.Verify(w, crypto.SHA3Sum256(bs)))
		}
	}
	assert.Equal(t, int64(len(data)), a.Len())

	a = &Accumulator{
		KeyForState: []byte("a"),
		Bucket:      bk,
	}
	a.Recover()

	for i, d := range data {
		w, err := a.WitnessFor(int64(i))
		assert.NoError(t, err)
		hs := WitnessesToHashes(w)
		w = HashesToWitness(hs, int64(i))
		assert.NoError(t, a.Verify(w, crypto.SHA3Sum256([]byte(d))))
	}

	t.Logf("%s", a)
}

func TestMTAccumulator_Extension(t *testing.T) {
	mdb := db.NewMapDB()

	data := []string{
		"dog", "cat", "elephant", "bird", "monkey", "lion", "tiger",
	}

	accs := make([]*Accumulator, 0)
	for i:=0; i<2; i++ {
		name := fmt.Sprintf("acc_%d",i)
		bk, _ := mdb.GetBucket(db.BucketID(name))
		acc := &Accumulator{
			KeyForState: []byte(name),
			Bucket:      bk,
		}
		for _, d := range data {
			acc.AddData([]byte(d))
		}
		assert.NoError(t, acc.Flush())
		accs = append(accs, acc)
	}

	extData := make([]string, 0)
	for i, d := range data {
		ed := fmt.Sprintf("%s_%d", d, i)
		extData = append(extData, ed)
		accs[1].AddData([]byte(ed))
	}

	for i, d := range data {
		idx := int64(i)
		bs := []byte(d)

		ws, err := accs[0].WitnessFor(idx)
		assert.NoError(t, err)
		assert.NoError(t, accs[1].VerifyWithAccLength(ws, crypto.SHA3Sum256(bs), accs[0].length))

		ws, err = accs[1].WitnessFor(idx)
		assert.NoError(t, err)
		assert.NoError(t, accs[0].VerifyWithAccLength(ws, crypto.SHA3Sum256(bs), accs[1].length))
	}

	for i, d := range extData {
		idx := int64(i + len(data))
		bs := []byte(d)

		w, err := accs[0].WitnessFor(idx)
		assert.EqualError(t, err, errors.ErrNotFound.Error())

		w, err = accs[1].WitnessFor(idx)
		assert.NoError(t, err)
		err = accs[0].VerifyWithAccLength(w, crypto.SHA3Sum256(bs), accs[1].length)
		assert.EqualError(t, err, errors.IllegalArgumentError.New("InvalidWitness newer node").Error())
	}
}
