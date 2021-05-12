// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Libraries/BMV.sol";

contract MockBMV is BMV {
    function setStatus(
        uint _height,
        uint _offset,
        uint _lastHeight
    ) 
        external
    {
        mta.height = _height;
        mta.offset = _offset;
        lastBTPHeight = _lastHeight;
    }
}