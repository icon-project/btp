// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../libraries/LibMPT.sol";

contract TestLibMPT {
    using LibMerklePatriciaTrie for LibMerklePatriciaTrie.MPT;
    using LibMerklePatriciaTrie for bytes32;
    using LibMerklePatriciaTrie for bytes;

    function bytesToNibbles(bytes memory data, bytes memory nibbles)
        public
        pure
        returns (bytes memory)
    {
        return data.bytesToNibbles(nibbles);
    }

    function matchNibbles(bytes memory src, bytes memory dst)
        public
        pure
        returns (uint256)
    {
        return src.matchNibbles(dst);
    }

    function prove(
        bytes32 root,
        bytes memory key,
        bytes[] memory proofs
    ) public returns (bytes memory) {
        return root.prove(key, proofs);
    }
}
