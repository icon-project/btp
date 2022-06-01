// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "solidity-bytes-utils/contracts/BytesLib.sol";

library MerkleTreeLib {

    using BytesLib for bytes;

    struct Path {
        uint direction;
        bytes32 hash;
    }

    function calculate(bytes32 leaf, Path[] memory pathes) internal returns (bytes32) {
        bytes32 temp = leaf;
        for (uint i = 0; i < pathes.length; i++) {
            temp = pathes[i].direction == 0
                ? keccak256(abi.encodePacked(pathes[i].hash).concat(abi.encodePacked(temp)))
                : keccak256(abi.encodePacked(temp).concat(abi.encodePacked(pathes[i].hash)));
        }
        return temp;
    }
}
