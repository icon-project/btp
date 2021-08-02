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

func (sme *SignedMessageEnum) Encode(encoder scale.Encoder) error {
	if err := encoder.EncodeOption(sme.IsPrevote, sme.AsPrevote); err != nil {
		return err
	}

	if err := encoder.EncodeOption(sme.IsPrecommit, sme.AsPrecommit); err != nil {
		return err
	}

	if err := encoder.EncodeOption(sme.IsPrimaryPropose, sme.AsPrimaryPropose); err != nil {
		return err
	}

	return nil
}

func NewSubstrateHashFromHexString(s string) SubstrateHash {
	hash, err := types.NewHashFromHexString(s)
	if err != nil {
		panic(err)
	}
	return hash
}

func NewVoteMessage(justification *GrandpaJustification, setId types.U64) VoteMessage {
	return VoteMessage{
		Message: SignedMessageEnum{
			IsPrecommit: true,
			AsPrecommit: SignedMessage{
				TargetHash:   justification.Commit.TargetHash,
				TargetNumber: justification.Commit.TargetNumber,
			},
		},
		Round: justification.Round,
		SetId: setId,
	}
}

func NewEncodedVoteMessage(vm VoteMessage) ([]byte, error) {
	return types.EncodeToBytes(vm)
}

func NewEncodedSubstrateHeader(header SubstrateHeader) ([]byte, error) {
	return types.EncodeToBytes(header)
}
