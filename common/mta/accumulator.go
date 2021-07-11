package mta

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
)

const (
	HashSize = 32
)

type State int

const (
	stateDirty State = iota
	stateHashed
	stateFlushed
)

type Direction int

const (
	Left Direction = iota
	Right
)

func (d Direction) String() string {
	switch d {
	case Left:
		return "LEFT"
	case Right:
		return "RIGHT"
	default:
		return fmt.Sprint(int(d))
	}
}

type Witness struct {
	Direction Direction
	HashValue []byte
}

func (w Witness) String() string {
	return fmt.Sprintf("{%s,%#x}", w.Direction, w.HashValue)
}

type Node interface {
	Hash() []byte
	Flush() error
	WitnessFor(depth int, idx int64, w []Witness) (Node, []Witness, error)
}

type hashNode struct {
	bucket    db.Bucket
	hashValue []byte
}

func (n *hashNode) Hash() []byte {
	if n == nil {
		return nil
	}
	return n.hashValue
}

func (n *hashNode) Flush() error {
	return nil
}

func (n *hashNode) String() string {
	return fmt.Sprintf("HashNode{hash=%x}", n.hashValue)
}

func (n *hashNode) resolve() (Node, error) {
	bs, err := n.bucket.Get(n.hashValue)
	if err != nil {
		return n, errors.Wrapf(err, "ResolveFailure(hash=%x)", n.hashValue)
	}

	if len(bs) != 2*HashSize {
		return n, errors.New("InvalidData")
	}
	return &branchNode{
		state:      stateFlushed,
		bucket:     n.bucket,
		hashValue:  n.hashValue,
		serialized: bs,
		left: &hashNode{
			bucket:    n.bucket,
			hashValue: bs[0:HashSize],
		},
		right: &hashNode{
			bucket:    n.bucket,
			hashValue: bs[HashSize:],
		},
	}, nil
}

func (n *hashNode) WitnessFor(depth int, idx int64, w []Witness) (Node, []Witness, error) {
	if depth < 1 {
		return n, w, nil
	}
	if node, err := n.resolve(); err != nil {
		return n, nil, err
	} else {
		return node.WitnessFor(depth, idx, w)
	}
}

type branchNode struct {
	state       State
	bucket      db.Bucket
	hashValue   []byte
	serialized  []byte
	left, right Node
}

func (n *branchNode) String() string {
	return fmt.Sprintf("BranchNode{hash=%x,left=%s,right=%s}",
		n.Hash(), n.left, n.right)
}

func (n *branchNode) Hash() []byte {
	if n == nil {
		return nil
	}
	if n.state < stateHashed {
		bs := make([]byte, 2*HashSize)
		copy(bs, n.left.Hash())
		copy(bs[HashSize:], n.right.Hash())
		n.serialized = bs
		n.hashValue = crypto.SHA3Sum256(bs)
		n.state = stateHashed
	}
	return n.hashValue
}

func (n *branchNode) Flush() error {
	if n == nil {
		return nil
	}
	if n.state == stateFlushed {
		return nil
	}
	if err := n.left.Flush(); err != nil {
		return err
	}
	if err := n.right.Flush(); err != nil {
		return err
	}
	hv := n.Hash()
	if err := n.bucket.Set(hv, n.serialized); err != nil {
		return err
	}
	n.state = stateFlushed
	return nil
}

func (n *branchNode) WitnessFor(depth int, idx int64, w []Witness) (Node, []Witness, error) {
	if depth < 1 {
		return n, nil, errors.New("InvalidDepth")
	}
	var err error
	bound := int64(1) << uint(depth-1)
	if idx < bound {
		n.left, w, err = n.left.WitnessFor(depth-1, idx, w)
		if err == nil {
			w = append(w, Witness{Right, n.right.Hash()})
		}
	} else {
		n.right, w, err = n.right.WitnessFor(depth-1, idx-bound, w)
		if err == nil {
			w = append(w, Witness{Left, n.left.Hash()})
		}
	}
	return n, w, err
}

type dataNode struct {
	state     State
	bucket    db.Bucket
	hashValue []byte
	data      []byte
}

func (n *dataNode) WitnessFor(depth int, idx int64, w []Witness) (Node, []Witness, error) {
	if depth > 0 {
		return n, nil, errors.New("InvalidDepth")
	}
	return n, w, nil
}

func (n *dataNode) String() string {
	return fmt.Sprintf("DataNode{hash=%x}", n.Hash())
}

func (n *dataNode) Hash() []byte {
	if n == nil {
		return nil
	}
	if n.state < stateHashed {
		n.hashValue = crypto.SHA3Sum256(n.data)
		n.state = stateHashed
	}
	return n.hashValue
}

func (n *dataNode) Flush() error {
	if n.state < stateFlushed {
		err := n.bucket.Set(n.Hash(), n.data)
		if err != nil {
			return err
		}
		n.state = stateFlushed
	}
	return nil
}

type Accumulator struct {
	KeyForState []byte    // key to recover state
	Bucket      db.Bucket // bucket to store all state data
	roots       []Node
	length      int64
}

func (a *Accumulator) String() string {
	return fmt.Sprintf("Accumulator{roots=%+v, length=%d}",
		a.roots, a.length)
}

func (a *Accumulator) Len() int64 {
	return a.length
}

func (a *Accumulator) addNode(h int, n Node, w []Witness) []Witness {
	if h >= len(a.roots) {
		a.roots = append(a.roots, n)
		a.length += 1
		return w
	}
	root := a.roots[h]
	var rh []byte
	if root != nil {
		rh = root.Hash()
	}
	if len(rh) == 0 {
		a.roots[h] = n
		a.length += 1
		return w
	} else {
		w = append(w, Witness{Left, rh})
		a.roots[h] = nil
		b := &branchNode{
			state:      stateDirty,
			bucket:     a.Bucket,
			hashValue:  nil,
			serialized: nil,
			left:       root,
			right:      n,
		}
		return a.addNode(h+1, b, w)
	}
}

type serializedMTAccumulator struct {
	Roots  [][]byte        `json:"roots"`
	Length common.HexInt64 `json:"length"`
}

func (a *Accumulator) Flush() error {
	roots := make([][]byte, len(a.roots))
	for i, r := range a.roots {
		if r != nil {
			if err := r.Flush(); err != nil {
				return err
			}
			roots[i] = r.Hash()
		}
	}

	var s serializedMTAccumulator
	s.Roots = roots
	s.Length.Value = a.length

	if bs, err := json.Marshal(&s); err != nil {
		return err
	} else {
		return a.Bucket.Set(a.KeyForState, bs)
	}
}

func (a *Accumulator) Recover() error {
	bs, err := a.Bucket.Get(a.KeyForState)
	if err != nil {
		return err
	}
	if len(bs) == 0 {
		a.roots = nil
		a.length = 0
		return nil
	}
	var s serializedMTAccumulator
	if err := json.Unmarshal(bs, &s); err != nil {
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
	a.length = s.Length.Value
	return nil
}

func (a *Accumulator) AddNode(n Node) []Witness {
	w := make([]Witness, 0, len(a.roots))
	return a.addNode(0, n, w)
}

func (a *Accumulator) AddHash(h []byte) []Witness {
	n := &hashNode{
		bucket:    a.Bucket,
		hashValue: h,
	}
	return a.AddNode(n)
}

func (a *Accumulator) AddData(d []byte) []Witness {
	l := &dataNode{
		state:     stateDirty,
		bucket:    a.Bucket,
		hashValue: nil,
		data:      d,
	}
	return a.AddNode(l)
}

func (a *Accumulator) WitnessFor(idx int64) (w []Witness, err error) {
	if idx >= a.length {
		return nil, errors.ErrNotFound
	}
	offset := len(a.roots)

	// FIXME fix this case MTA root := a.roots[0]
	// if offset == 0 {
	// }

	for offset > 0 {
		offset -= 1
		if a.roots[offset] == nil {
			continue
		}
		inbound := int64(1) << uint(offset)
		if idx < inbound {
			witness := make([]Witness, 0, offset)
			root := a.roots[offset]
			root, w, err = root.WitnessFor(offset, idx, witness)
			a.roots[offset] = root
			return w, err
		}
		idx -= inbound

	}
	return nil, errors.ErrNotFound
}

func (a *Accumulator) WitnessForWithAccLength(idx, accLength int64) (w []Witness, err error) {
	if w, err = a.WitnessFor(idx); err != nil {
		return nil, err
	}
	depth := GetDepthByHeightAndAccLength(idx, accLength)
	return w[:depth], nil
}

func (a *Accumulator) Verify(ws []Witness, h []byte) error {
	buf := make([]byte, HashSize*2)
	height := 0
	for _, w := range ws {
		if w.Direction == Left {
			copy(buf, w.HashValue)
			copy(buf[HashSize:], h)
		} else {
			copy(buf, h)
			copy(buf[HashSize:], w.HashValue)
		}
		h = crypto.SHA3Sum256(buf)
		height += 1
	}
	if height >= len(a.roots) {
		return errors.Wrap(errors.ErrIllegalArgument, "InvalidWitness newer witness")
	}
	root := a.roots[height]
	if root == nil {
		return errors.IllegalArgumentError.New("InvalidWitness not exists root")
	}
	if !bytes.Equal(root.Hash(), h) {
		return errors.IllegalArgumentError.New("InvalidWitness not matched root")
	}
	return nil
}

func (a *Accumulator) VerifyWithAccLength(ws []Witness, h []byte, accLength int64) error {
	if a.length == accLength {
		return a.Verify(ws, h)
	}

	height := GetHeightFromWitness(ws, accLength)
	if height >= a.length {
		return errors.IllegalArgumentError.New("InvalidWitness newer node")
	}
	if a.length > accLength {
		// acc: new, wit: old
		nws, err := a.WitnessFor(height)
		if err != nil {
			return err
		}
		for i, w := range ws {
			nws[i] = w
		}
		return a.Verify(nws, h)
	} else {
		// acc: old, wit: new
		depth := 0
		for i := len(a.roots) - 1; i > 0; i-- {
			bitFlag := int64(1) << uint(i)
			if height < bitFlag {
				depth = i
				break
			}
			height -= bitFlag
		}
		return a.Verify(ws[:depth], h)
	}
}

func (a *Accumulator) GetNode(idx int64) (Node, error) {
	if idx >= a.length {
		return nil, errors.ErrNotFound
	}
	offset := len(a.roots)
	for offset > 0 {
		offset -= 1
		if a.roots[offset] == nil {
			continue
		}
		inbound := int64(1) << uint(offset)
		if idx < inbound {
			return GetNode(a.roots[offset], offset, idx)
		}
		idx -= inbound
	}
	return nil, errors.ErrNotFound
}

func WitnessesToHashes(w []Witness) [][]byte {
	hs := make([][]byte, len(w))
	for i, wt := range w {
		hs[i] = wt.HashValue
	}
	return hs
}

func HashesToWitness(hvs [][]byte, idx int64) []Witness {
	w := make([]Witness, len(hvs))
	for i, hv := range hvs {
		if idx%2 == 0 {
			w[i].Direction = Right
		} else {
			w[i].Direction = Left
		}
		w[i].HashValue = hv
		idx /= 2
	}
	return w
}

func GetHeightFromWitness(ws []Witness, accLength int64) int64 {
	idx := 0
	offset := 0
	depth := len(ws)
	//calculate index (latest=0)
	for i := 0; i < depth; i++ {
		bitFlag := 1 << uint(i)
		if ws[i].Direction == Right {
			//merkle tree at root_idx=len(ws)
			//same as binary tree
			idx |= bitFlag
		}
		if (accLength & int64(bitFlag)) != 0 {
			//merkle tree is not empty at root_idx=i
			//add merkle tree size at root_idx=i
			offset += bitFlag
		}
	}
	idx += offset
	//reverse index (first=0)
	return accLength - 1 - int64(idx)
}

func GetDepthByHeightAndAccLength(height, accLength int64) int {
	depth := 0
	v := accLength
	//calculate max depth
	for ; 0 < v; v >>= 1 {
		depth++
	}
	for depth > 0 {
		depth--
		bitFlag := int64(1) << uint(depth)
		if accLength&bitFlag == bitFlag {
			if height < bitFlag {
				return depth
			}
			height -= bitFlag
		}
	}
	return depth
}

func GetNode(p Node, depth int, idx int64) (Node, error) {
	switch n := p.(type) {
	case *branchNode:
		if depth < 1 {
			return n, errors.New("InvalidDepth")
		}
		bound := int64(1) << uint(depth-1)
		if idx < bound {
			return GetNode(n.left, depth-1, idx)
		} else {
			return GetNode(n.right, depth-1, idx-bound)
		}
	case *hashNode:
		if depth < 1 {
			return n, nil
		}
		if r, err := n.resolve(); err != nil {
			return n, err
		} else {
			return GetNode(r, depth, idx)
		}
	case *dataNode:
		if depth > 0 {
			return n, errors.New("InvalidDepth")
		}
		return n, nil
	default:
		panic("invalid type")
	}
}
