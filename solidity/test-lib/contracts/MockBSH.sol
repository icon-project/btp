// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMC.sol";
import "./interfaces/IBSH.sol";

contract MockBSH is IBSH {
    constructor() {}

    function sendMessage(
        address _bmc,
        string calldata _to,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external {
        IBMC(_bmc).sendMessage(_to, _svc, _sn, _msg);
    }

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        emit HandleBTPMessage(_from, _svc, _sn, _msg);
    }

    event HandleBTPMessage(string _from, string _svc, uint256 _sn, bytes _msg);

    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override {
        emit HandleBTPError(_src, _svc, _sn, _code, _msg);
    }

    event HandleBTPError(string _src, string _svc, uint256 _sn, uint256 _code, string _msg);
}
