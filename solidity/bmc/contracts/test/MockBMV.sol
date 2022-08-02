// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../interfaces/IBMV.sol";
import "../libraries/RLPEncode.sol";

contract MockBMV is IBMV {

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
        returns (string memory base64EncodedMTA)
    {}

    /**
        @return addr connected BMC address
     */
    function getConnectedBMC() external view returns (address addr) {}

    /**
        @return net network address of the blockchain
     */
    function getNetAddress()
        external
        view
        returns (string memory net)
    {}

    /**
        @return serializedHash hash of RLP encode from given list of validators
        @return addresses list of validators' addresses
     */
    function getValidators()
        external
        view
        returns (bytes32 serializedHash, address[] memory addresses)
    {}

    /**
        @notice Used by the relay to resolve next BTP Message to send.
                Called by BMC.
        @return height Last verified block height
        @return extra  extra rlp encoded bytes
                (offset of MerkleTreeAccumulator, block height of last relayed BTP Message)
     */
    function getStatus()
        external
        view
        override
        returns (
            uint256 height,
            bytes memory extra
        )
    {
        bytes[] memory l = new bytes[](2);
        l[0] = RLPEncode.encodeUint(mta.offset);
        l[1] = RLPEncode.encodeUint(lastBTPHeight);
        return (mta.height, RLPEncode.encodeList(l));
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
