package types

import (
	"encoding/json"
	"fmt"
)

type ValidatorStakeStructVersion []byte

func (vs *ValidatorStakeStructVersion) UnmarshalJSON(p []byte) error {
	var validatorStakeStructVersion string
	err := json.Unmarshal(p, &validatorStakeStructVersion)
	if err != nil {
		return err
	}

	if validatorStakeStructVersion == "" {
		*vs = nil
		return nil
	}

	switch validatorStakeStructVersion {
	case "V1":
		*vs = []byte{0}
	default:
		return fmt.Errorf("not supported validator struct")
	}
	return nil
}

type BlockProducer struct {
	ValidatorStakeStructVersion ValidatorStakeStructVersion `json:"validator_stake_struct_version"`
	AccountId                   AccountId                   `json:"account_id"`
	PublicKey                   PublicKey                   `json:"public_key"`
	Stake                       BigInt                      `json:"stake"`
}

type NextBlockProducers []BlockProducer

func (nbps *NextBlockProducers) UnmarshalJSON(p []byte) error {
	var response struct {
		BlockProducers []BlockProducer `json:"next_bps"`
	}
	err := json.Unmarshal(p, &response)
	if err != nil {
		return err
	}

	*nbps = NextBlockProducers(response.BlockProducers)
	return nil
}
