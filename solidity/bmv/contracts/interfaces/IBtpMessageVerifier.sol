// SPDX-License-Identifier: Apache-2.0
pragma solidity > 0.5.8;
pragma experimental ABIEncoderV2;

interface IBtpMessageVerifier {

    function getStatus() external view returns (uint, uint, uint, uint);

    /**
        @notice Decodes Relay Messages and process BTP Messages.
                If there is an error, then it sends a BTP Message containing the Error Message.
                BTP Messages with old sequence numbers are ignored. A BTP Message contains future
                sequence number will fail.
        @param _bmc BTP Address of the BMC handling the message
        @param _prev BTP Address of the previous BMC
        @param _seq next sequence number to get a message
        @param _msg serialized bytes of Relay Message
        @return serializedMessages List of serialized bytes of a BTP Message
    */
    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    )
    external
    returns (bytes[] memory);

}
