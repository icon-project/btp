// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

struct Path {
    uint256 direction;
    bytes32 hash;
}

library MerkleTreeLib {
    function calculate(bytes32 leaf, Path[] memory pathes) internal pure returns (bytes32) {
        bytes32 temp = leaf;
        for (uint256 i = 0; i < pathes.length; i++) {
            temp = pathes[i].direction == 0
                ? keccak256(bytes.concat(abi.encodePacked(pathes[i].hash), abi.encodePacked(temp)))
                : keccak256(bytes.concat(abi.encodePacked(temp), abi.encodePacked(pathes[i].hash)));
        }
        return temp;
    }
}
