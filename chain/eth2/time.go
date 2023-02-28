package eth2

import (
	"github.com/attestantio/go-eth2-client/spec/phase0"
)

const (
	SecondPerSlot                = 12
	SlotPerEpoch                 = 32
	EpochsPerSyncCommitteePeriod = 256
	SlotsPerHistoricalRoot       = 8192
)

// SlotToEpoch returns the epoch number of the input slot.
func SlotToEpoch(s phase0.Slot) phase0.Epoch {
	return phase0.Epoch(s / SlotPerEpoch)
}

func SyncCommitteePeriod(e phase0.Epoch) uint64 {
	return uint64(e / EpochsPerSyncCommitteePeriod)
}

func SyncCommitteePeriodAtSlot(s phase0.Slot) uint64 {
	return SyncCommitteePeriod(SlotToEpoch(s))
}

func IsSyncCommitteeEdge(s phase0.Slot) bool {
	return (SlotToEpoch(s) % EpochsPerSyncCommitteePeriod) == 0
}

func SlotToHistoricalIndex(s phase0.Slot) int {
	return int(s % SlotsPerHistoricalRoot)
}
