package icon

import "errors"

var (
	ErrBlockNotReady               = errors.New("required result to be 32 bytes, but got 0")
	ErrInvalidBlockUpdateProofSize = errors.New("invalid BlockUpdate.Proof size")
	ErrInvalidBlockProofSize       = errors.New("invalid BlockProof size")
	ErrInvalidStateProofSize       = errors.New("invalid StateProof.Proof size")
	ErrInvalidReceiptProofSize     = errors.New("invalid ReceiptProof.Proof size")
	ErrInvalidEventProofProofSize  = errors.New("invalid EventProof.Proof size")
	ErrNotSupportedSrcChain        = errors.New("not supported source chain")
)
