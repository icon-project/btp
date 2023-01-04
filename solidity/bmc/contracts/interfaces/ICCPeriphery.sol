// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/Types.sol";

interface ICCPeriphery {

    function sendInternal(
        string memory _next,
        bytes memory _msg
    ) external;

    function dropMessage(
        string memory _prev,
        uint256 _seq,
        Types.BTPMessage memory _msg
    ) external;

    function clearSeq(
        string memory _link
    ) external;

    function emitClaimRewardResult(
        address _sender,
        string memory _network,
        int256 _nsn,
        uint256 _result
    ) external;
}
