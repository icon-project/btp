package btp

import (
	"math"
	"sync/atomic"

	"github.com/icon-project/btp/chain"
)

// bmrIndex returns the index of the current BRM in BMCStatusLink
func (b *BTP) bmrIndex() int {
	for i, bmr := range b.bmcLinkStatus.BMRs {
		if bmr.Address == b.wallet.Address() {
			return i
		}
	}

	b.log.Warn("BMR not found")
	return -1
}

// HeightOfDst return turn the current heigh of the BMC destination
func (b *BTP) HeightOfDst() int64 {
	return atomic.LoadInt64(&b.heightOfDst)
}

func (b *BTP) relayableIndex(rotate int) int {
	bs := b.bmcLinkStatus
	relaybleIndex := bs.BMRIndex

	if rotate > 0 {
		relaybleIndex += rotate
		if relaybleIndex >= len(bs.BMRs) {
			relaybleIndex = relaybleIndex % len(bs.BMRs)
		}
	}

	return relaybleIndex
}

func (b *BTP) overMaxAggregation(rm *chain.RelayMessage) bool {
	return len(rm.BlockUpdates) >= b.bmcLinkStatus.MaxAggregation
}

func (b *BTP) relayble(rm *chain.RelayMessage) bool {
	bs := b.bmcLinkStatus
	if bs.RotateTerm <= 0 {
		b.log.Debugf("bs.RotateTerm:%v", bs.RotateTerm)
		return false
	}

	rotate := int(math.Ceil(float64(b.HeightOfDst()+1-bs.RotateHeight) / float64(bs.RotateTerm)))
	relayableIndex := b.relayableIndex(rotate)
	relaybleHeightEnd := bs.RotateHeight
	if rotate > 0 {
		relaybleHeightEnd += int64(bs.RotateTerm * rotate)
	}

	b.log.Tracef("relayableIndex:%v b.bmrIndex:%v b.overMaxAggregation(rm):%v", relayableIndex, b.bmrIndex(), b.overMaxAggregation(rm))

	prevFinalizeHeight := relaybleHeightEnd - int64(bs.RotateTerm) + int64(b.sender.FinalizeLatency())
	return (relayableIndex == b.bmrIndex()) &&
		(prevFinalizeHeight <= b.HeightOfDst()) &&
		(relaybleHeightEnd == b.HeightOfDst()+1 || b.overMaxAggregation(rm))
}

func (b *BTP) skippable(rm *chain.RelayMessage) bool {
	if len(rm.ReceiptProofs) < 0 {
		return false
	}

	bs := b.bmcLinkStatus
	if bs.RotateTerm <= 0 {
		return true
	}

	rotate := 0
	relaybleHeightStart := bs.RotateHeight - int64(bs.RotateTerm+1)
	if rm.HeightOfDst > bs.RotateHeight {
		rotate = int(math.Ceil(float64(rm.HeightOfDst-bs.RotateHeight) / float64(bs.RotateTerm)))
		if rotate > 0 {
			relaybleHeightStart += int64(bs.RotateTerm * (rotate - 1))
		}
	}

	skip := int(math.Ceil(float64(b.HeightOfDst()+1-rm.HeightOfDst)/float64(bs.DelayLimit))) - 1
	if skip > 0 {
		rotate += skip
		relaybleHeightStart = rm.HeightOfDst + int64(bs.DelayLimit*skip)
	}

	relaybleIndex := b.relayableIndex(rotate)
	prevFinalizeHeight := relaybleHeightStart + int64(b.sender.FinalizeLatency())
	return (relaybleIndex == b.bmrIndex()) && (prevFinalizeHeight <= b.HeightOfDst())
}
