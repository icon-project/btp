// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../libraries/MerkleTreeAccumulator.sol";

contract TestLibMTA {
    using MerkleTreeAccumulator for MerkleTreeAccumulator.MTA;
    using MerkleTreeAccumulator for bytes;

    MerkleTreeAccumulator.MTA public mta;

    constructor() {}

    function initFromSerialized(bytes memory serializedData) external {
        mta.initFromSerialized(serializedData);
    }

    function getMTA()
        public
        view
        returns (MerkleTreeAccumulator.MTA memory)
    {
        return mta;
    }

    function setOffset(uint256 offset) public {
        mta.setOffset(offset);
    }

    function getRoot(uint256 idx) public view returns (bytes32) {
        return mta.getRoot(idx);
    }

    function doesIncludeCache(bytes32 _hash) public view returns (bool) {
        return mta.doesIncludeCache(_hash);
    }

    function putCache(bytes32 _hash) public {
        mta.putCache(_hash);
    }

    function add(bytes32 _hash) public {
        mta.add(_hash);
    }

    function getRootIndexByHeight(uint256 height)
        public
        view
        returns (uint256)
    {
        return mta.getRootIndexByHeight(height);
    }

    function verify(
        bytes32[] calldata proof,
        bytes32 leaf,
        uint256 height,
        uint256 at
    ) public {
        mta.verify(proof, leaf, height, at);
    }

    function toRlpBytes() public view returns (bytes memory) {
        return mta.toBytes();
    }
}
