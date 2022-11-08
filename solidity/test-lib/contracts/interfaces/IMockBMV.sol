// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

interface IMockBMV {

    function setHeight(
        uint256 _height
    ) external;

    function setOffset(
        uint256 _offset
    ) external;

    function setLastHeight(
        uint256 _lastHeight
    ) external;

    event HandleRelayMessage(
        bytes _ret
    );
}
