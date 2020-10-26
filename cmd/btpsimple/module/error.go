package module

import (
	"fmt"

	"github.com/icon-project/btp/common/errors"
)

var (
	ErrAlreadyExists = fmt.Errorf("already exists")
)

var (
	//TODO ErrorCodes
	ErrConnectFail            = fmt.Errorf("fail to connect")
	ErrSendFailByExpired      = fmt.Errorf("reject by expired")
	ErrSendFailByFuture       = fmt.Errorf("reject by future")
	ErrSendFailByOverflow     = fmt.Errorf("reject by overflow")
	ErrGetResultFailByPending = fmt.Errorf("fail to getresult by pending")
	ErrDelegatorNotAvailable  = fmt.Errorf("fail to delegate relay")
)

const (
	CodeBTP      errors.Code = 0
	CodeBMC      errors.Code = 10
	CodeBMV      errors.Code = 25
	CodeBSH      errors.Code = 40
	CodeReserved errors.Code = 55
)

const (
	BMCRevert = CodeBMC + iota
	BMCRevertUnauthorized
	BMCRevertInvalidSN
	BMCRevertAlreadyExistsBMV
	BMCRevertNotExistsBMV
	BMCRevertAlreadyExistsBSH
	BMCRevertNotExistsBSH
	BMCRevertAlreadyExistsLink
	BMCRevertNotExistsLink
	BMCRevertUnreachable
	BMCRevertNotExistsPermission
)

var (
	BMCRevertCodeNames = map[errors.Code]string{
		BMCRevert:                    "BMCRevert",
		BMCRevertUnauthorized:        "BMCRevertUnauthorized",
		BMCRevertInvalidSN:           "BMCRevertInvalidSN",
		BMCRevertAlreadyExistsBMV:    "BMCRevertAlreadyExistsBMV",
		BMCRevertNotExistsBMV:        "BMCRevertNotExistsBMV",
		BMCRevertAlreadyExistsBSH:    "BMCRevertAlreadyExistsBSH",
		BMCRevertNotExistsBSH:        "BMCRevertNotExistsBSH",
		BMCRevertAlreadyExistsLink:   "BMCRevertAlreadyExistsLink",
		BMCRevertNotExistsLink:       "BMCRevertNotExistsLink",
		BMCRevertUnreachable:         "BMCRevertUnreachable",
		BMCRevertNotExistsPermission: "BMCRevertNotExistsPermission",
	}
)

const (
	BMVRevert = CodeBMV + iota
	BMVRevertInvalidMPT
	BMVRevertInvalidVotes
	BMVRevertInvalidSequence
	BMVRevertInvalidBlockUpdate
	BMVRevertInvalidBlockProof
	BMVRevertInvalidBlockWitness
	BMVRevertInvalidSequenceHigher
	BMVRevertInvalidBlockUpdateHigher
	BMVRevertInvalidBlockUpdateLower
	BMVRevertInvalidBlockProofHigher
	BMVRevertInvalidBlockWitnessOld
)

var (
	BMVRevertCodeNames = map[errors.Code]string{
		BMVRevert:                         "BMVRevert",
		BMVRevertInvalidMPT:               "BMVRevertInvalidMPT",
		BMVRevertInvalidVotes:             "BMVRevertInvalidVotes",
		BMVRevertInvalidSequence:          "BMVRevertInvalidSequence",
		BMVRevertInvalidBlockUpdate:       "BMVRevertInvalidBlockUpdate",
		BMVRevertInvalidBlockProof:        "BMVRevertInvalidBlockProof",
		BMVRevertInvalidBlockWitness:      "BMVRevertInvalidBlockWitness",
		BMVRevertInvalidSequenceHigher:    "BMVRevertInvalidSequenceHigher",
		BMVRevertInvalidBlockUpdateHigher: "BMVRevertInvalidBlockUpdateHigher",
		BMVRevertInvalidBlockUpdateLower:  "BMVRevertInvalidBlockUpdateLower",
		BMVRevertInvalidBlockProofHigher:  "BMVRevertInvalidBlockProofHigher",
		BMVRevertInvalidBlockWitnessOld:   "BMVRevertInvalidBlockWitnessOld",
	}
)

func NewRevertError(code int) error {
	c := errors.Code(code)
	if c >= CodeBTP {
		var msg string
		var ok bool
		if c < CodeBMC {
			msg = fmt.Sprintf("BTPRevert[%d]", c)
		} else if c < CodeBMV {
			if msg, ok = BMCRevertCodeNames[c]; !ok {
				msg = fmt.Sprintf("BMCRevert[%d]", c)
			}
		} else if c < CodeBSH {
			if msg, ok = BMVRevertCodeNames[c]; !ok {
				msg = fmt.Sprintf("BMVRevert[%d]", c)
			}
		} else if c < CodeReserved {
			msg = fmt.Sprintf("BSHRevert[%d]", c)
		} else {
			msg = fmt.Sprintf("ReservedRevert[%d]", c)
		}
		return errors.NewBase(c, msg)
	}
	return nil
}
