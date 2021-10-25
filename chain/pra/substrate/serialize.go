package substrate

import (
	"encoding/json"
	"reflect"

	"github.com/centrifuge/go-substrate-rpc-client/v3/scale"
	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	scalecodec "github.com/itering/scale.go"
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

func (sme SignedMessageEnum) Encode(encoder scale.Encoder) error {
	var err1, err2 error
	if sme.IsPrevote {
		err1 = encoder.PushByte(0)
		err2 = encoder.Encode(sme.AsPrevote)
	} else if sme.IsPrecommit {
		err1 = encoder.PushByte(1)
		err2 = encoder.Encode(sme.AsPrecommit)
	} else if sme.IsPrimaryPropose {
		err1 = encoder.PushByte(2)
		err2 = encoder.Encode(sme.AsPrimaryPropose)
	}

	if err1 != nil {
		return err1
	}
	if err2 != nil {
		return err2
	}

	return nil
}

func NewEventParaInclusionCandidateIncluded(decodedEvent map[string]interface{}) EventParasInclusionCandidateIncluded {
	eventParamsVal := reflect.ValueOf(decodedEvent["params"])
	event := EventParasInclusionCandidateIncluded{}
	if eventParamsVal.Kind() == reflect.Slice {
		firstEventParam, ok := eventParamsVal.Index(0).Interface().(scalecodec.EventParam)
		if !ok {
			panic("NewEventParaInclusionCandidateIncluded: not an scalecodec processed event")
		}

		b, err := json.Marshal(firstEventParam.Value)
		if err != nil {
			panic("NewEventParaInclusionCandidateIncluded: not an valid json")
		}

		candidateReceipt := CandidateReceipt{}
		json.Unmarshal(b, &candidateReceipt)

		event.CandidateReceipt = CandidateReceiptRaw{
			Descriptor: CandidateDescriptorRaw{
				ParaId:   candidateReceipt.Descriptor.ParaId,
				ParaHead: NewSubstrateHashFromHexString(candidateReceipt.Descriptor.ParaHead),
			},
		}
	}

	return event
}

func NewEventEVMLog(decodedEvent map[string]interface{}) EventEVMLog {
	eventParamsVal := reflect.ValueOf(decodedEvent["params"])
	event := EventEVMLog{}
	if eventParamsVal.Kind() == reflect.Slice {
		firstEventParam, ok := eventParamsVal.Index(0).Interface().(scalecodec.EventParam)
		if !ok {
			panic("NewEventEVMLog: not an scalecodec processed event")
		}

		b, err := json.Marshal(firstEventParam.Value)
		if err != nil {
			panic("NewEventParaInclusionCandidateIncluded: not an valid json")
		}

		evmLog := EthereumLog{}
		json.Unmarshal(b, &evmLog)
		for _, topic := range evmLog.Topics {
			event.Log.Topics = append(event.Log.Topics, types.NewH256(types.MustHexDecodeString(topic)))
		}

		event.Log.Address = types.NewH160(types.MustHexDecodeString(evmLog.Address))
		event.Log.Data = types.MustHexDecodeString(evmLog.Data)
	}

	return event
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

func NewBlockNumber(blocknumber uint64) SubstrateBlockNumber {
	return types.BlockNumber(blocknumber)
}

func NewEncodedVoteMessage(vm VoteMessage) ([]byte, error) {
	return types.EncodeToBytes(vm)
}

func NewEncodedSubstrateHeader(header SubstrateHeader) ([]byte, error) {
	return types.EncodeToBytes(header)
}

func NewStorageKey(hex string) SubstrateStorageKey {
	byte, _ := types.HexDecodeString(hex)
	return types.NewStorageKey(byte)
}
