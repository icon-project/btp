package types

type ReceiptProof struct {
	OutComeProof     ExecutionOutcomeWithIdView `json:"outcome_proof"`
	OutComeRootProof MerklePath                 `json:"outcome_root_proof"`
	BlockProof       MerklePath                 `json:"block_proof"`
}
