// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

interface IBMV {
    /**
        @notice Used by the relay to resolve next BTP Message to send.
                Called by BMC.
        @return height Last verified block height
        @return extra  extra rlp encoded bytes
     */
    function getStatus()
        external
        view
        returns (
            uint256 height,
            bytes memory extra
        );

    /**
        @notice Decodes Relay Messages and process BTP Messages.
                If there is an error, then it sends a BTP Message containing the Error Message.
                BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
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
        bytes calldata _msg
    ) external returns (bytes[] memory);
}
