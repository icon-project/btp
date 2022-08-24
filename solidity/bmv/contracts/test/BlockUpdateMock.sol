// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "../libraries/MerkleTreeLib.sol";
import "../libraries/BlockUpdateLib.sol";
import "../libraries/RLPReader.sol";
import "../libraries/RLPEncode.sol";

contract BlockUpdateMock {
    using BlockUpdateLib for Header;
    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    function decodeHeader(bytes calldata enc) public pure returns (Header memory) {
        return BlockUpdateLib.decodeHeader(enc);
    }

    function decodeProof(bytes calldata enc) public pure returns (Proof memory) {
        return BlockUpdateLib.decodeProof(enc);
    }
}
