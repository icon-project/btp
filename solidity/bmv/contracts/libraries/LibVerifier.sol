// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./LibRLPDecode.sol";
import "./LibTypes.sol";
import "./LibMsgDecoder.sol";
import "./LibRLPEncode.sol";
import "./LibMTA.sol";
import "./LibMPT.sol";
import "./LibBytes.sol";
import "./LibHash.sol";

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
    using LibHash for bytes;
    using LibHash for bytes32;

    function verifyMTAProof(
        LibTypes.BlockProof memory _blockProof,
        LibMerkleTreeAccumulator.MTA storage mta
    ) internal {
        require(
            _blockProof.blockWitness.witnesses.length != 0,
            "BMVRevertInvalidBlockWitness"
        );

        require(
            mta.height >= _blockProof.blockHeader.height,
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

        verifyVotes(
            _blockUpdate.votes,
            _blockUpdate.blockHeader.height,
            _blockUpdate.blockHeader.blockHash,
            validators,
            _blockUpdate.blockHeader.nextValidatorsHash !=
                validators.validatorsHash
        );

        if (
            _blockUpdate.blockHeader.nextValidatorsHash !=
            validators.validatorsHash
        ) {
            if (_blockUpdate.nextValidators.length == 0) {
                revert(
                    "BMVRevertInvalidBlockUpdate: Not exists next validators"
                );
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
        LibTypes.Validators storage validators,
        bool _isNextValidatorsUpdated
    ) internal {
        // Calculate RLP of vote item [block height, vote.round, vote_type precommit = 1, block hash, vote.bpsi]
        bytes memory serializedVoteMsg;
        bytes[] memory _blockPartSetId = new bytes[](2);
        _blockPartSetId[0] = LibRLPEncode.encodeUint(_votes.blockPartSetID.n);
        _blockPartSetId[1] = LibRLPEncode.encodeBytes(_votes.blockPartSetID.b);
        serializedVoteMsg = abi.encodePacked(
            LibRLPEncode.encodeUint(_blockHeight),
            LibRLPEncode.encodeUint(_votes.round),
            LibRLPEncode.encodeUint(1),
            LibRLPEncode.encodeBytes(abi.encodePacked(_blockHash)),
            LibRLPEncode.encodeList(_blockPartSetId)
        );

        bytes32 _msgHash;
        bytes memory _encodedVoteMsg;
        for (uint256 i = 0; i < _votes.ts.length; i++) {
            _encodedVoteMsg = abi.encodePacked(
                serializedVoteMsg,
                LibRLPEncode.encodeUint(_votes.ts[i].timestamp)
            );
            _encodedVoteMsg = abi.encodePacked(
                _encodedVoteMsg.length.addLength(false),
                _encodedVoteMsg
            );

            _msgHash = _encodedVoteMsg.sha3FIPS256();
            bytes memory _publicKey =
                _msgHash.ecRecoverPublicKey(_votes.ts[i].signature);
            bytes memory _hashKey = abi.encodePacked(_publicKey.sha3FIPS256());
            address _address = _hashKey.slice(12, 32).bytesToAddress();

            require(
                validators.containedValidators[_address],
                "BMVRevertInvalidVotes: Invalid signature"
            );

            require(
                !validators.checkDuplicateVotes[_address],
                "BMVRevertInvalidVotes: Duplicated votes"
            );
            validators.checkDuplicateVotes[_address] = true;
        }

        require(
            _votes.ts.length > (validators.validatorAddrs.length * 2) / 3,
            "BMVRevertInvalidVotes: Require votes > 2/3"
        );

        for (uint256 i = 0; i < validators.validatorAddrs.length; i++) {
            delete validators.checkDuplicateVotes[validators.validatorAddrs[i]];
            if (_isNextValidatorsUpdated)
                delete validators.containedValidators[
                    validators.validatorAddrs[i]
                ];
        }
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
