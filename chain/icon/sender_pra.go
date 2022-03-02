package icon

import (
	"errors"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
)

var (
	paraChainMaxBlockUpdatePerSegment       = 10
	ErrInvalidParaChainFinalityProofsLength = errors.New("FinalityProofs of ParaChainBlockUpdates must greater than 0")
)

func (s *sender) praSegment(rm *chain.RelayMessage, height int64) ([]*chain.Segment, error) {
	var err error
	segments := make([]*chain.Segment, 0)

	msg := &RelayMessage{
		BlockUpdates:  make([][]byte, 0),
		ReceiptProofs: make([][]byte, 0),
	}

	for i, bu := range rm.BlockUpdates {
		if bu.Height <= height {
			continue
		}

		// There're cases that BMR requires to submit BlockUpdate contains empty ScaleEncodedBlockHeader and
		// with FinaliyProofs with RelayBlockUpdates and Justifications
		// Then, it can submit ParaChainBlockUpdate with RelayBlockProof and RelayReceiptProofs
		var paraBuExtra chain.ParaChainBlockUpdateExtra
		if _, err := codec.RLP.UnmarshalFromBytes(bu.Proof, &paraBuExtra); err != nil {
			return nil, err
		}

		// There must be FinalityProofs because Parachain can be self-finalized
		if len(paraBuExtra.FinalityProofs) == 0 {
			return nil, ErrInvalidParaChainFinalityProofsLength
		}

		// The ParaChainBlockUpdate with RelayBlockProof and RelayReceiptProofs always in the last element of the array
		encodedParaBu, err := codec.RLP.MarshalToBytes(chain.ParaChainBlockUpdate{
			ScaleEncodedBlockHeader: paraBuExtra.ScaleEncodedBlockHeader,
			FinalityProof:           paraBuExtra.FinalityProofs[len(paraBuExtra.FinalityProofs)-1],
		})

		if err != nil {
			return nil, err
		}

		// We can't submit ParachainBlockUpdates contains non-empty ScaleEncodedBlockHeader,
		// and ParaBlockUpdates contains empty ScaleEncodedBlockHeader, in a SAME RelayMessage

		// Check a Single ParachainBlockUpdate requires submit multiple RelayMessages
		if len(paraBuExtra.FinalityProofs) > 1 {
			// The next ParachainBlockUpdate requires submit in a separate RelayMessage
			// Then, all the previous ParachainBlockUpdates in msg, is included in a RelayMessage
			if len(msg.BlockUpdates) > 0 {
				s.l.Debug("Segment: send non-empty ScaleEncodedBlockHeader")
				segment := &chain.Segment{
					Height:              msg.height,
					NumberOfBlockUpdate: msg.numberOfBlockUpdate,
				}

				if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
					return nil, err
				}

				segments = append(segments, segment)
				// Reset message
				msg = &RelayMessage{
					BlockUpdates:  make([][]byte, 0),
					ReceiptProofs: make([][]byte, 0),
				}
			}

			// 2 cases of RelayChainData
			// - Justifications Block greater than ParaInclusionCandidateIncluded Block -> 1 RelayMessage for RelayChainData
			// - Justifications Block greater than GrandpaNewAuthorities Block
			// And GrandpaNewAuthorities Block greater than ParaInclusionCandidateIncluded Block
			// -> 2 RelayMessages for Relaychain Data
			for j := 0; j < len(paraBuExtra.FinalityProofs)-1; j++ {
				s.l.Debugf("Segment: send RelayChainData")
				encodedRelayChainDataBu, err := codec.RLP.MarshalToBytes(chain.ParaChainBlockUpdate{
					ScaleEncodedBlockHeader: nil,
					FinalityProof:           paraBuExtra.FinalityProofs[j],
				})

				if err != nil {
					return nil, err
				}

				relayChainDataMsg := &RelayMessage{
					BlockUpdates:  [][]byte{encodedRelayChainDataBu},
					ReceiptProofs: make([][]byte, 0),
				}

				var transParams *TransactionParam
				if transParams, err = s.newTransactionParam(rm.From.String(), relayChainDataMsg); err != nil {
					return nil, err
				}

				segments = append(segments, &chain.Segment{
					// Height at the same height with the non-empty ScaleEncodedBlockHeader
					// This is to prove that submit the non-empty ScaleEncodedBlockHeader requires RelayChain Blocks prior to
					Height:           bu.Height,
					TransactionParam: transParams,
					// No Parachain BlockUpdate
					NumberOfBlockUpdate: 0,
				})
			}
		}

		// If the remaining BlockUpdates in msg over limit
		if msg.numberOfBlockUpdate > paraChainMaxBlockUpdatePerSegment {
			s.l.Debugf("Segment: split by maximum number of blockupdate per Segment")
			segment := &chain.Segment{
				Height:              msg.height,
				NumberOfBlockUpdate: msg.numberOfBlockUpdate,
			}
			if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
				return nil, err
			}
			segments = append(segments, segment)
			// Reset message
			msg = &RelayMessage{
				BlockUpdates:  make([][]byte, 0),
				ReceiptProofs: make([][]byte, 0),
			}
		}

		s.l.Tracef("Segment: at %d BlockUpdates[%d]", bu.Height, i)
		msg.BlockUpdates = append(msg.BlockUpdates, encodedParaBu)
		msg.height = bu.Height
		msg.numberOfBlockUpdate += 1
	}

	var bp []byte
	if bp, err = codec.RLP.MarshalToBytes(rm.BlockProof); err != nil {
		return nil, err
	}

	for i, rp := range rm.ReceiptProofs {
		if len(rp.Proof) > 0 && len(msg.BlockUpdates) == 0 {
			if rm.BlockProof == nil {
				return segments, nil
			}
			msg.BlockProof = bp
			msg.ReceiptProofs = append(msg.ReceiptProofs, rp.Proof)

			segment := &chain.Segment{
				Height:              rm.BlockProof.BlockWitness.Height,
				EventSequence:       rp.Events[len(rp.Events)-1].Sequence,
				NumberOfEvent:       len(rp.Events),
				NumberOfBlockUpdate: 0,
			}

			if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
				return nil, err
			}

			segments = append(segments, segment)
			s.l.Tracef("Segment: at %d StateProofs[%d]", rp.Height, i)
		} else {
			msg.eventSequence = rp.Events[len(rp.Events)-1].Sequence
			msg.numberOfEvent = len(rp.Events)
			msg.ReceiptProofs = append(msg.ReceiptProofs, rp.Proof)
			s.l.Tracef("Segment: at %d StateProofs[%d]", rp.Height, i)
		}
	}

	if len(msg.BlockUpdates) > 0 || len(msg.ReceiptProofs) > 0 {
		segment := &chain.Segment{
			Height:              msg.height,
			NumberOfBlockUpdate: msg.numberOfBlockUpdate,
			EventSequence:       msg.eventSequence,
			NumberOfEvent:       msg.numberOfEvent,
		}

		if segment.TransactionParam, err = s.newTransactionParam(rm.From.String(), msg); err != nil {
			return nil, err
		}

		segments = append(segments, segment)
	}

	return segments, nil
}
