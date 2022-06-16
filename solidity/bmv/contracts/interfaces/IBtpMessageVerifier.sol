// SPDX-License-Identifier: Apache-2.0
pragma solidity >= 0.8.1;

interface IBtpMessageVerifier {

    function getStatus() external view returns (uint256);

    // NOTE: Using bytes message instead of base64url during development
    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        // string memory _msg
        bytes memory _msg
    )
    external
    returns (bytes[] memory);

}
