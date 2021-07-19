package substrate

import (
	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
)

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

func (j *Justification) Decode(decoder scale.Decoder) error {
	err := decoder.Decode(&j.ConsensusEngineId)
	if err != nil {
		return err
	}

	err = decoder.Decode(&j.EncodedJustification)
	return err
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

func (event SubstrateEventRecordsRaw) DecodeEventRecords(meta *SubstrateMetaData, records interface{}) error {
	return types.EventRecordsRaw(event).DecodeEventRecords(meta, records)
}

func NewSubstrateHashFromHexString(s string) SubstrateHash {
	hash, err := types.NewHashFromHexString(s)
	if err != nil {
		panic(err)
	}
	return hash
}
