package eth2

import (
	"testing"

	"github.com/attestantio/go-eth2-client/spec/altair"
	"github.com/attestantio/go-eth2-client/spec/phase0"
	ssz "github.com/ferranbt/fastssz"
	"github.com/prysmaticlabs/go-bitfield"
	"github.com/stretchr/testify/assert"

	"github.com/icon-project/btp/common/codec"
)

var lightClientHeader = &altair.LightClientHeader{
	Beacon: &phase0.BeaconBlockHeader{
		Slot:          phase0.Slot(4943744),
		ProposerIndex: phase0.ValidatorIndex(222870),
		ParentRoot: phase0.Root{
			0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
			0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
		},
		StateRoot: phase0.Root([32]byte{
			0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f,
		}),
		BodyRoot: phase0.Root([32]byte{
			0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f,
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x5c, 0x5d, 0x5e, 0x5f,
		}),
	},
}

var sszProof = &ssz.Proof{
	Index: 321,
	Leaf:  []byte("leaf"),
	Hashes: [][]byte{
		[]byte("hash1"),
		[]byte("hash2"),
	},
}

var blsPubKey = phase0.BLSPubKey([48]byte{
	0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
	0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
	0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f,
})

func TestMessage_BlockUpdateData(t *testing.T) {
	bu := &blockUpdateData{
		AttestedHeader:  lightClientHeader,
		FinalizedHeader: lightClientHeader,
		FinalizedHeaderBranch: [][]byte{
			[]byte("branch0"),
			[]byte("branch1"),
		},
		SyncAggregate: &altair.SyncAggregate{
			SyncCommitteeBits: bitfield.Bitvector512{
				0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			},
			SyncCommitteeSignature: phase0.BLSSignature(
				[96]byte{
					0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				},
			),
		},
		SignatureSlot: phase0.Slot(3),
		NextSyncCommittee: &altair.SyncCommittee{
			Pubkeys: []phase0.BLSPubKey{
				blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey,
				blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey,
				blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey,
				blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey, blsPubKey,
			},
			AggregatePubkey: blsPubKey,
		},
		NextSyncCommitteeBranch: [][]byte{
			[]byte("branch2"),
			[]byte("branch3"),
		},
	}

	bs := codec.RLP.MustMarshalToBytes(bu)

	bu2 := new(blockUpdateData)
	_, err := codec.RLP.UnmarshalFromBytes(bs, bu2)
	assert.NoError(t, err)
	assert.Equal(t, bu, bu2)
}

func TestMessage_BlockProofData(t *testing.T) {
	bp := &blockProofData{
		Header: lightClientHeader,
		Proof:  sszProof,
	}

	bs := codec.RLP.MustMarshalToBytes(bp)

	bp2 := new(blockProofData)
	_, err := codec.RLP.UnmarshalFromBytes(bs, bp2)
	assert.NoError(t, err)
	assert.Equal(t, bp, bp2)
}

func TestMessage_MessageProofData(t *testing.T) {
	mp := &messageProofData{
		Slot:              phase0.Slot(10),
		ReceiptsRootProof: sszProof,
		ReceiptProofs: []*receiptProof{
			{
				Key:   []byte("key1"),
				Proof: []byte("proof1"),
			},
			{
				Key:   []byte("key2"),
				Proof: []byte("proof3"),
			},
		},
	}

	bs := codec.RLP.MustMarshalToBytes(mp)

	mp2 := new(messageProofData)
	_, err := codec.RLP.UnmarshalFromBytes(bs, mp2)
	assert.NoError(t, err)
	assert.Equal(t, mp, mp2)
}

func TestMessage_ReceiptProof(t *testing.T) {
	rp := &receiptProof{
		Key:   []byte("key"),
		Proof: []byte("proof"),
	}

	bs := codec.RLP.MustMarshalToBytes(rp)

	rp2 := new(receiptProof)
	_, err := codec.RLP.UnmarshalFromBytes(bs, &rp2)
	assert.NoError(t, err)
	assert.Equal(t, rp, rp2)
}
