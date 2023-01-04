// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

interface IMockBMC {

    function setNetworkAddress(
        string memory _net
    ) external;

    function setFee(
        uint256 _forward,
        uint256 _backward
    ) external;

    //for BMV
    function handleRelayMessage(
        address _addr,
        string calldata _prev,
        uint256 _seq,
        bytes calldata _msg
    ) external;

    event HandleRelayMessage(
        bytes[] _ret
    );

    //for BSH
    event SendMessage(
        int256 _nsn,
        string _to,
        string _svc,
        int256 _sn,
        bytes _msg
    );

    function addResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) external;

    function hasResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) external view returns (
        bool
    );

    function clearResponse(
    ) external;

    function handleBTPMessage(
        address _addr,
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external;

    function handleBTPError(
        address _addr,
        string memory _src,
        string memory _svc,
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) external;
}
