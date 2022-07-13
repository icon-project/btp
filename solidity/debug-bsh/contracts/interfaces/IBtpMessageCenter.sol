// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

interface IBtpMessageCenter {

    function sendMessage(
        string calldata _to,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external;

}
