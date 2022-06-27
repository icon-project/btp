// SPDX-License-Identifier: Apache-2.0
pragma solidity >= 0.8.1;

interface IBtpMessageVerifier {

    function getStatus() external view returns (uint256);

    function handleRelayMessage(
        string memory _bmc,
        string calldata _prev,
        uint256 _seq,
        bytes calldata _msg
    )
    external
    returns (bytes[] memory);

}
