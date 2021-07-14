// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../BMV.sol";
import "../libraries/LibMTA.sol";

contract MockBMV is BMV {
    using LibMerkleTreeAccumulator for LibMerkleTreeAccumulator.MTA;

    function addRootToMTA(bytes32 _blockHash) external {
        mta.add(_blockHash);
    }

    function setMTAHeight(uint256 _height) external {
        mta.height = mta.offset = _height;
    }
}
