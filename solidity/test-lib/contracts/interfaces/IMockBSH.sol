// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

interface IMockBSH {

    function sendMessage(
        address _bmc,
        string memory _to,
        string memory _svc,
        int256 _sn,
        bytes memory _msg
    ) external payable;

    event SendMessage(
        int256 _nsn,
        string _to,
        string _svc,
        int256 _sn,
        bytes _msg
    );

    event HandleBTPMessage(
        string _from,
        string _svc,
        uint256 _sn,
        bytes _msg
    );

    event HandleBTPError(
        string _src,
        string _svc,
        uint256 _sn,
        uint256 _code,
        string _msg
    );
}
