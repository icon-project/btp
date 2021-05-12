// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Interfaces/BMV.sol";

contract MockBMV is BMV {
    function setStatus(
        uint256 _height,
        uint256 _offset,
        uint256 _lastHeight
    ) external {
        mta.height = _height;
        mta.offset = _offset;
        lastBTPHeight = _lastHeight;
    }
}
