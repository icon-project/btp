// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "../libraries/MerkleTreeLib.sol";
import "../libraries/BlockUpdateLib.sol";
import "../libraries/RLPReader.sol";
import "../libraries/RLPEncode.sol";

contract BlockUpdateMock {
    using BlockUpdateLib for BlockUpdateLib.Header;
    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    function decodeHeader(bytes calldata enc) public pure returns (BlockUpdateLib.Header memory) {
        return BlockUpdateLib.decodeHeader(enc);
    }

    function decodeProof(bytes calldata enc) public pure returns (BlockUpdateLib.Proof memory) {
        return BlockUpdateLib.decodeProof(enc);
    }
}
