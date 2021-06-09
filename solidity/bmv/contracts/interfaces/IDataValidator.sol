// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

interface IDataValidator {
    /**
        @notice validate receipt proofs and return btp messages
        @param _bmc BTP Address of the BMC handling the message
        @param _prev BTP Address of the previous BMC
        @param _seq next sequence number to get a message
        @param _serializedMsg serialized bytes of Relay Message
        @param _receiptHash receipt root hash of MPT
        @return serializedMessages List of serialized bytes of a BTP Message
     */
    function validateReceipt(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _serializedMsg,
        bytes32 _receiptHash
    ) external returns (bytes[] memory);
}
