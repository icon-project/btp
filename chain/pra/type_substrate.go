package pra

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

type SubstrateHash = types.Hash
type SubstrateMetaData = types.Metadata
type SubstrateStorageKey = types.StorageKey
type SubstrateStorageDataRaw = types.StorageDataRaw

func CreateStorageKey(meta *types.Metadata, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error) {
	key, err := types.CreateStorageKey(meta, prefix, method, arg, arg2)
	if err != nil {
		return nil, err
	}

	return SubstrateStorageKey(key), nil
}

type SubstrateEventRecordsRaw types.EventRecordsRaw

func (event SubstrateEventRecordsRaw) DecodeEventRecords(meta *SubstrateMetaData, records interface{}) error {
	return types.EventRecordsRaw(event).DecodeEventRecords(meta, records)
}

type GrandpaPrecommit struct {
	TargetHash   types.Hash
	TargetNumber GrandpaBlockNumber
}

type GrandpaSignedPrecommit struct {
	Precommit GrandpaPrecommit
	Signature types.Signature
	Id        types.AccountID
}

func (gsp *GrandpaSignedPrecommit) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&gsp.Precommit)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gsp.Signature)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gsp.Id)
	return err
}

type GrandpaCommit struct {
	TargetHash   types.Hash
	TargetNumber GrandpaBlockNumber
	Precommits   []GrandpaSignedPrecommit
}

func (gc *GrandpaCommit) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&gc.TargetHash)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gc.TargetNumber)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gc.Precommits)
	return err
}

// This type does contains Decodable interface like types.BlockNumber
type GrandpaBlockNumber = types.U32
type Round = types.U64

type GrandpaJustification struct {
	Round           Round
	Commit          GrandpaCommit
	VotesAncestries []types.Header
}

func (gj *GrandpaJustification) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&gj.Round)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gj.Commit)
	if err != nil {
		return err
	}

	err = decoder.Decode(&gj.VotesAncestries)
	return err
}

type Justification struct {
	ConsensusEngineId    [4]types.U8
	EncodedJustification GrandpaJustification
}

func (j *Justification) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&j.ConsensusEngineId)
	if err != nil {
		return err
	}

	err = decoder.Decode(&j.EncodedJustification)
	return err
}

type FinalityProof struct {
	Block          types.Hash
	Justification  Justification
	UnknownHeaders []types.Header
}

func (fp *FinalityProof) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&fp.Block)
	if err != nil {
		return err
	}

	err = decoder.Decode(&fp.Justification)
	if err != nil {
		return err
	}

	err = decoder.Decode(&fp.UnknownHeaders)
	return err
}

type SubstrateHeader = types.Header
type HeaderSubscription interface {
	Chan() <-chan types.Header
	Err() <-chan error
	Unsubscribe()
}
