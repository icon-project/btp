// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

interface IBMV {
    /**
        @return base64EncodedMTA Base64 encode of Merkle Tree
     */
    function getMTA() external view returns (string memory);

    /**
        @return addr connected BMC address
     */
    function getConnectedBMC() external view returns (address);

    /**
        @return net network address of the blockchain
     */
    function getNetAddress() external view returns (string memory);

    /**
        @return serializedHash hash of RLP encode from given list of validators
        @return addresses list of validators' addresses
     */
    function getValidators() external view returns (bytes32, address[] memory);

    /**
        @notice Used by the relay to resolve next BTP Message to send.
                Called by BMC.
        @return height height of MerkleTreeAccumulator
        @return offset offset of MerkleTreeAccumulator
        @return lastHeight block height of last relayed BTP Message
     */
    function getStatus()
        external
        view
        returns (
            uint256 height,
            uint256 offset,
            uint256 lastHeight
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
