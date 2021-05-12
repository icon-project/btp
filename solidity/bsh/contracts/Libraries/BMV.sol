// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../Interfaces/IBMV.sol";

contract BMV is IBMV {

    struct MTA {
        uint256 height;
        bytes32[] roots;
        uint256 offset;
        uint256 rootsSize;
        uint256 cacheSize;
        bytes32[] cache;
        bool isAllowNewerWitness;
    }

    MTA internal mta;
    uint internal lastBTPHeight;
    /**
        @return base64EncodedMTA Base64 encode of Merkle Tree
     */
    function getMTA()
        external
        view
        override
        returns (string memory base64EncodedMTA)
    {}

    /**
        @return addr connected BMC address
     */
    function getConnectedBMC() external view override returns (address addr) {}

    /**
        @return net network address of the blockchain
     */
    function getNetAddress()
        external
        view
        override
        returns (string memory net)
    {}

    /**
        @return serializedHash hash of RLP encode from given list of validators
        @return addresses list of validators' addresses
     */
    function getValidators()
        external
        view
        override
        returns (bytes32 serializedHash, address[] memory addresses)
    {}

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
        override
        returns (
            uint256 height,
            uint256 offset,
            uint256 lastHeight
        )
    {
        return (
            mta.height,
            mta.offset,
            lastBTPHeight
        );
    }

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
        string calldata _bmc,
        string calldata _prev,
        string calldata _seq,
        string calldata _msg
    ) external override returns (bytes[] memory serializedMessages) {}
}
