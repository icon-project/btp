package mta

import (
	"fmt"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
)

type ExtAccumulator struct {
	Accumulator
	offset int64
	//TODO config
	limitRoots int
	//TODO cache
	//sizeCache  int
	serialized []byte
}

type serializedExtAccumulator struct {
	Height     int64
	Roots      [][]byte
	Offset     int64
	LimitRoots int
	//SizeCache  int
	//Cache      [][]byte
	//IsAllowNewerWitness bool
}

func (a *ExtAccumulator) Height() int64 {
	return a.length + a.offset
}

func (a *ExtAccumulator) Offset() int64 {
	return a.offset
}

func (a *ExtAccumulator) Serialized() []byte {
	return a.serialized
}

func (a *ExtAccumulator) Flush() error {
	rhs := make([][]byte, len(a.roots))
	for i, rn := range a.roots {
		if rn != nil {
			if err := rn.Flush(); err != nil {
				return err
			}
			rhs[i] = rn.Hash()
		}
	}

	s := serializedExtAccumulator{
		Height:     a.Height(),
		Roots:      rhs,
		Offset:     a.offset,
		LimitRoots: a.limitRoots,
		//SizeCache:  a.sizeCache,
	}
	b, err := codec.RLP.MarshalToBytes(&s)
	if err != nil {
		return err
	}
	err = a.Bucket.Set(a.KeyForState, b)
	if err != nil {
		return err
	}
	a.serialized = b
	return nil
}

func (a *ExtAccumulator) Recover() error {
	b, err := a.Bucket.Get(a.KeyForState)
	if err != nil {
		return err
	}
	if len(b) == 0 {
		a.roots = nil
		a.length = 0
		return nil
	}
	var s serializedExtAccumulator
	if _, err = codec.RLP.UnmarshalFromBytes(b, &s); err != nil {
		return err
	}
	a.roots = make([]Node, len(s.Roots))
	for i, hv := range s.Roots {
		if len(hv) == 0 {
			a.roots[i] = nil
		} else if len(hv) == HashSize {
			a.roots[i] = &hashNode{
				bucket:    a.Bucket,
				hashValue: hv,
			}
		}
	}
	a.length = s.Height - s.Offset
	a.serialized = b
	if a.offset < s.Offset || a.offset > s.Offset {
		a.length = 0
		a.Flush()
		log.Debugf("resync with new offset:%d, height:%d", a.Offset(), a.Height())
	}
	return nil
}

func (a *ExtAccumulator) addNode(h int, n Node, w []Witness) []Witness {
	//limitRoots, offset calculate
	var rootSize = a.RootSize()
	if a.limitRoots > 0 && rootSize == a.limitRoots {
		log.Debugf("limitRoots, offset calculate:%d, rootSize:%d", a.Offset(), rootSize)
		a.roots[rootSize-1].Delete()
		a.roots[rootSize-1] = nil
		var change = int64(1) << (a.limitRoots - 1)
		a.offset += change
		a.length -= change
		log.Debugf("new offset:%d", a.Offset())
	}
	w = a.Accumulator.addNode(h, n, w)
	return w
}

func (a *ExtAccumulator) AddNode(n Node) []Witness {
	w := make([]Witness, 0, len(a.roots))
	return a.addNode(0, n, w)
}

func (a *ExtAccumulator) AddHash(h []byte) []Witness {
	n := &hashNode{
		bucket:    a.Bucket,
		hashValue: h,
	}
	return a.AddNode(n)
}

func (a *ExtAccumulator) AddData(d []byte) []Witness {
	l := &dataNode{
		state:     stateDirty,
		bucket:    a.Bucket,
		hashValue: nil,
		data:      d,
	}
	return a.AddNode(l)
}

func (a *ExtAccumulator) WitnessForAt(height, at, offset int64) (int64, []Witness, error) {
	if a.offset > offset {
		return -1, nil, errors.New("not support lower offset")
	} else if a.offset < offset {
		//TODO support higher offset
		return -1, nil, errors.New("not support higher offset yet")
	}

	idx := height - 1 - a.offset
	accLength := at - a.offset
	if accLength > a.length {
		accLength = a.length
		at = a.length + a.offset
	}

	w, err := a.Accumulator.WitnessForWithAccLength(idx, accLength)
	fmt.Printf("at:%d, idx:%d, accLength:%d\n", at, idx, accLength)
	return at, w, err
}

func (a *ExtAccumulator) VerifyAt(w []Witness, h []byte, at, offset int64) error {
	if a.offset > offset {
		return errors.New("not support lower offset")
	} else if a.offset < offset {
		//TODO support higher offset
		return errors.New("not support higher offset yet")
	}
	accLength := at - a.offset
	if accLength > a.length {
		accLength = a.length
		at = a.length + a.offset
	}
	return a.VerifyWithAccLength(w, h, accLength)
}

func (a *ExtAccumulator) GetNode(height int64) (Node, error) {
	return a.Accumulator.GetNode(height - 1 - a.offset)
}

func NewExtAccumulator(keyForState []byte, bk db.Bucket, offset int64, limitRoots int) *ExtAccumulator {
	return &ExtAccumulator{
		offset:     offset,
		limitRoots: limitRoots,
		Accumulator: Accumulator{
			KeyForState: keyForState,
			Bucket:      bk,
		},
	}
}
