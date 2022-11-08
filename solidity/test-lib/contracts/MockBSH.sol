// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMC.sol";
import "./interfaces/IBSH.sol";
import "./interfaces/IMockBSH.sol";

contract MockBSH is IBSH, IMockBSH {
    constructor() {}

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        emit HandleBTPMessage(_from, _svc, _sn, _msg);
    }

    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override {
        emit HandleBTPError(_src, _svc, _sn, _code, _msg);
    }

    function sendMessage(
        address _bmc,
        string calldata _to,
        string calldata _svc,
        int256 _sn,
        bytes memory _msg
    ) external payable override {
        int256 nsn = IBMC(_bmc).sendMessage{value: msg.value}(_to, _svc, _sn, _msg);
        emit SendMessage(nsn, _to, _svc, _sn, _msg);
    }
}
