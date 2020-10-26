package mta

import (
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/db"
)

func TestExtAccumulator_Basic(t *testing.T) {
	mdb := db.NewMapDB()
	bk, _ := mdb.GetBucket("")

	a := NewExtAccumulator([]byte("a"), bk, 0)
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
			hs := WitnessesToHashes(w)
			w = HashesToWitness(hs, j)
			assert.NoError(t, a.Verify(w, crypto.SHA3Sum256(bs)))
		}
	}
	assert.Equal(t, int64(len(data)), a.Len())

	a2 := NewExtAccumulator([]byte("a"), bk, 0)
	assert.NoError(t, a2.Recover())
	t.Logf("Recovered Accumulator:%s", a2)

	for i, d := range data {
		w, err := a2.WitnessFor(int64(i))
		assert.NoError(t, err)
		hs := WitnessesToHashes(w)
		w = HashesToWitness(hs, int64(i))
		assert.NoError(t, a2.Verify(w, crypto.SHA3Sum256([]byte(d))))
	}

	h := int64(5)
	at := int64(5)
	tat, w, err := a2.WitnessForAt(h, at, a2.offset)
	assert.NoError(t, err)
	assert.Equal(t, at, tat, "")
	assert.NoError(t,a2.VerifyAt(w, crypto.SHA3Sum256([]byte(data[h-1])), at, a2.offset))

	t.Logf("%s", a)
}

func TestMTAccumulator_Dump(t *testing.T) {
	mdb := db.NewMapDB()
	bk, _ := mdb.GetBucket("")

	acc := &Accumulator{
		KeyForState: []byte("acc"),
		Bucket:      bk,
	}
	data := []string{
		"dog", "cat", "elephant", "bird", "monkey", "lion", "tiger", "last",
	}
	for i, d := range data {
		acc.AddData([]byte(d))
		assert.NoError(t, acc.Flush())
		fmt.Println(strings.Repeat("=",10),"Accumulator", acc.length, strings.Repeat("=",10), strings.Join(data[:i+1],","))
		fmt.Println(dumpAccumulator(acc))
		for j := int64(0); j <= int64(i); j++ {
			fmt.Println("Witness", j, data[j])

			bs := []byte(data[j])
			fmt.Println(dumpData(j, bs))

			ws, _ := acc.WitnessFor(j)
			fmt.Println(dumpWitness(acc.length, ws))
		}
	}
}

func base64Encode(b []byte) string {
	return base64.URLEncoding.EncodeToString(b)
}

func dumpAccumulator(acc *Accumulator) string {
	st := struct{
		Height int64
		Roots [][]byte
	}{
		Height: acc.length,
		Roots: make([][]byte, len(acc.roots)),
	}
	fmt.Println("height:",st.Height)
	for i, r := range acc.roots {
		if r != nil {
			st.Roots[i] = r.Hash()
			fmt.Println("root[",i,"]:",hex.EncodeToString(st.Roots[i]))
		}else{
			fmt.Println("root[",i,"]:nil")
		}
	}
	b, _ := codec.RLP.MarshalToBytes(&st)
	return base64Encode(b)
}

func dumpData(height int64, data []byte) string {
	h := crypto.SHA3Sum256(data)
	st := struct{
		Height int64
		Hash []byte
	}{
		Height: height,
		Hash: h,
	}
	b, _ := codec.RLP.MarshalToBytes(&st)
	fmt.Println("dumpData.height:",st.Height,",hash:",hex.EncodeToString(h))
	return base64Encode(b)
}

func dumpWitness(height int64, ws []Witness) string {
	st := struct{
		Height int64
		Witness [][]byte
	}{
		Height: height,
		Witness: WitnessesToHashes(ws),
	}
	b, _ := codec.RLP.MarshalToBytes(&st)
	fmt.Print("dumpWitness.height:",st.Height,",witness:[")
	for _, w := range st.Witness {
		fmt.Print(hex.EncodeToString(w),",")
	}
	fmt.Println("]")
	return base64Encode(b)
}

func dumpNodes(rn Node) {
	switch n := rn.(type) {
	case *branchNode:
		fmt.Printf("branch:%s left:%s right:%s\n",
			hex.EncodeToString(n.Hash()), hex.EncodeToString(n.left.Hash()), hex.EncodeToString(n.right.Hash()))
		dumpNodes(n.left)
		dumpNodes(n.right)
	case *hashNode:
		if hn, err := n.resolve(); err != nil {
			fmt.Printf("hash:%s err:%v \n", hex.EncodeToString(n.Hash()), err)
		} else {
			dumpNodes(hn)
		}
	case *dataNode:
		fmt.Printf("data:%s\n", hex.EncodeToString(n.Hash()))
	}
}

func dumpAt(a *ExtAccumulator, at int64) ([]byte, error) {
	rhs := make([][]byte, 0)
	idx := at - a.offset - 1
rootLoop:
	for i := len(a.roots) - 1; i >= 0; i-- {
		inbound := int64(1) << uint(i)
		rn := a.roots[i]

		if rn == nil {
			fmt.Printf("a.roots[%d]:%s, inbound:%d, idx:%d\n", i, "nil", inbound, idx)
			rhs = append(rhs, nil)
		} else {
			fmt.Printf("a.roots[%d]:%s, inbound:%d, idx:%d\n", i, hex.EncodeToString(rn.Hash()), inbound, idx)
			if (idx + 1) == inbound {
				fmt.Printf("found root:%s\n", hex.EncodeToString(rn.Hash()))
				rhs = append(rhs, rn.Hash())
				dumpNodes(rn)
				for ; inbound > 1; inbound >>= 1 {
					rhs = append(rhs, nil)
				}
				break rootLoop
			} else if idx < inbound {
				for inbound > 1 {
					inbound >>= 1
					fmt.Printf("inbound:%d, idx:%d\n", inbound, idx)
					if hn, ok := rn.(*hashNode); ok {
						if n, err := hn.resolve(); err != nil {
							panic(err)
						} else {
							rn = n
						}
					}
					bn := rn.(*branchNode)
					if (idx + 1) == inbound {
						rn = bn.left
						fmt.Printf("found root:%s\n", hex.EncodeToString(rn.Hash()))
						rhs = append(rhs, rn.Hash())
						dumpNodes(rn)
						for ; inbound > 1; inbound >>= 1 {
							rhs = append(rhs, nil)
						}
						break rootLoop
					} else {
						if idx < inbound {
							rn = bn.left
							if len(rhs) > 0 {
								rhs = append(rhs, nil)
								fmt.Printf("select left add nil\n")
							} else {
								fmt.Printf("select left\n")
							}
						} else {
							fmt.Printf("select right add left:%s\n", hex.EncodeToString(bn.left.Hash()))
							rhs = append(rhs, bn.left.Hash())
							rn = bn.right
							idx -= inbound
						}
					}
				}
				fmt.Printf("add last:%s\n", hex.EncodeToString(rn.Hash()))
				rhs = append(rhs, rn.Hash())
				break rootLoop
			} else {
				rhs = append(rhs, rn.Hash())
			}
			idx -= inbound
		}
	}

	s := serializedExtAccumulator{
		Height:     at,
		Roots:      make([][]byte, len(rhs)),
		Offset:     a.offset,
		LimitRoots: a.limitRoots,
		//SizeCache:  a.sizeCache,
	}
	j := len(rhs)
	for i, rh := range rhs {
		s.Roots[j-i-1] = rh
	}
	return codec.RLP.MarshalToBytes(&s)
}
