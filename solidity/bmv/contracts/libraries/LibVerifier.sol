// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./LibRLPDecode.sol";
import "./LibTypes.sol";
import "./LibMsgDecoder.sol";
import "./LibRLPEncode.sol";
import "./LibMTA.sol";
import "./LibMPT.sol";
import "./LibBytes.sol";

library LibVerifier {
    using LibRLPDecode for bytes;
    using LibRLPDecode for LibRLPDecode.RLPItem;
    using LibRLPEncode for bytes;
    using LibRLPEncode for uint256;
    using LibMerkleTreeAccumulator for uint256;
    using LibMerkleTreeAccumulator for bytes32;
    using LibMerkleTreeAccumulator for LibMerkleTreeAccumulator.MTA;
    using LibMerklePatriciaTrie for bytes32;
    using LibMsgDecoder for bytes;
    using LibBytes for bytes;

    function verifyMTAProof(
        LibTypes.BlockProof memory _blockProof,
        LibMerkleTreeAccumulator.MTA storage mta
    ) internal view {
        require(
            _blockProof.blockWitness.witnesses.length != 0,
            "BMVRevertInvalidBlockWitness"
        );
        if (mta.height < _blockProof.blockHeader.height)
            revert(
                "BMVRevertInvalidBlockProofHigher"
            );
        mta.verify(
            _blockProof.blockWitness.witnesses,
            _blockProof.blockHeader.blockHash,
            _blockProof.blockHeader.height,
            _blockProof.blockWitness.height
        );
    }

    function verifyValidators(
        LibTypes.BlockUpdate memory _blockUpdate,
        LibTypes.Validators storage validators
    ) internal returns (bool) {
        require(
            _blockUpdate.votes.ts.length != 0,
            "BMVRevertInvalidBlockUpdate: Not exists votes"
        );

        // pending for SHA3-256 and EDCSA
        verifyVotes(
            _blockUpdate.votes,
            _blockUpdate.blockHeader.height,
            _blockUpdate.blockHeader.blockHash,
            validators
        );

        if (
            _blockUpdate.blockHeader.nextValidatorsHash !=
            validators.validatorsHash
        ) {
            if (_blockUpdate.nextValidators.length == 0) {
                revert("BMVRevertInvalidBlockUpdate: Not exists next validators");
            } else if (
                _blockUpdate.nextValidatorsHash ==
                _blockUpdate.blockHeader.nextValidatorsHash
            ) {
                return true;
            } else
                revert(
                    "BMVRevertInvalidBlockUpdate: Invalid next validator hash"
                );
        }
        return false;
    }

    function verifyVotes(
        LibTypes.Votes memory _votes,
        uint256 _blockHeight,
        bytes32 _blockHash,
        LibTypes.Validators storage validators
    ) internal {
        // Calculate RLP of vote item [block height, vote.round, vote_type precommit = 1, block hash, vote.bpsi]
        bytes memory serializedVoteMsg;
        bytes[] memory list = new bytes[](2);
        list[0] = LibRLPEncode.encodeUint(_votes.blockPartSetID.n);
        list[2] = LibRLPEncode.encodeBytes(_votes.blockPartSetID.b);
        serializedVoteMsg = abi.encodePacked(
            serializedVoteMsg,
            LibRLPEncode.encodeUint(_blockHeight),
            LibRLPEncode.encodeUint(_votes.round),
            LibRLPEncode.encodeUint(1),
            LibRLPEncode.encodeBytes(abi.encodePacked(_blockHash)),
            LibRLPEncode.encodeList(list)
        );

        address[] memory blockValidators = new address[](_votes.ts.length); // contained validator's addresses
        bytes32 msgHash;
        bytes memory encodedVoteMsg;
        for (uint256 i = 0; i < _votes.ts.length; i++) {
            encodedVoteMsg = abi.encodePacked(
                serializedVoteMsg,
                LibRLPEncode.encodeUint(_votes.ts[i].timestamp)
            );
            encodedVoteMsg = abi.encodePacked(
                encodedVoteMsg.length.addLength(false),
                encodedVoteMsg
            );

            // TODO: use SHA3_256 instead of keccak, and recover address from signature and msg hash
            msgHash = keccak256(encodedVoteMsg);
            // pulbicKey = edcsa(msgHash, votes.ts.signature)
            // addr = addressByPublicKey(publicKey)
            // TODO: blockValidators[i] = addr
            blockValidators[i] = validators.validatorAddrs[i];
            require(
                validators.containedValidators[blockValidators[i]],
                "BMVRevertInvalidVotes: Invalid signature"
            );

            // check duplicated votes
            require(
                validators.checkDuplicateVotes[blockValidators[i]] == false,
                "BMVRevertInvalidVotes: Duplicated votes"
            );
            validators.checkDuplicateVotes[blockValidators[i]] = true;
        }

        require(
            blockValidators.length > (validators.validatorAddrs.length * 2) / 3,
            "BMVRevertInvalidVotes: Require votes > 2/3"
        );
    }

    function verifyMPTProof(
        LibTypes.ReceiptProof memory _receiptProof,
        bytes32 _receiptHash
    ) internal returns (LibTypes.Receipt memory) {
        bytes memory leaf =
            _receiptHash.prove(_receiptProof.mptKey, _receiptProof.mptProofs);

        LibTypes.Receipt memory receipt;
        receipt.eventLogHash = leaf.toRlpItem().toList()[8]
            .toBytes()
            .bytesToBytes32();

        receipt.eventLogs = new LibTypes.EventLog[](
            _receiptProof.eventProofs.length
        );
        bytes[] memory serializedEventlLogs =
            new bytes[](_receiptProof.eventProofs.length);
        for (uint256 i = 0; i < _receiptProof.eventProofs.length; i++) {
            serializedEventlLogs[i] = receipt.eventLogHash.prove(
                _receiptProof.eventProofs[i].mptKey,
                _receiptProof.eventProofs[i].mptProofs
            );
            receipt.eventLogs[i] = serializedEventlLogs[i].decodeEventLog();
        }
        return receipt;
    }
}
