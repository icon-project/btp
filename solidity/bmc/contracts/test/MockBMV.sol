// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../interfaces/IBMV.sol";
import "../libraries/DecodeBase64.sol";

contract MockBMV is IBMV {
    using DecodeBase64 for string;

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
    uint256 internal lastBTPHeight;

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
        return (mta.height, mta.offset, lastBTPHeight);
    }

    /**
        @notice Decodes Relay Messages and process BTP Messages.
                If there is an error, then it sends a BTP Message containing the Error Message.
                BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
        @param _msg serialized bytes of Relay Message
        @return serializedMessages List of serialized bytes of a BTP Message
     */
    function handleRelayMessage(
        string memory,
        string memory,
        uint256,
        bytes calldata _msg
    ) external pure override returns (bytes[] memory) {
        bytes[] memory btpMsgs = new bytes[](1);
        btpMsgs[0] = _msg;
        return btpMsgs;
    }

    function setStatus(
        uint256 _height,
        uint256 _offset,
        uint256 _lastHeight
    ) external {
        mta.height = _height;
        mta.offset = _offset;
        lastBTPHeight = _lastHeight;
    }
}
