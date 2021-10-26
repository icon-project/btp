// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./interfaces/IBMV.sol";
import "./interfaces/IDataValidator.sol";

import "./libraries/Base64.sol";
import "./libraries/MerkleTreeAccumulator.sol";
import "./libraries/String.sol";
import "./libraries/Types.sol";
import "./libraries/MessageDecoder.sol";
import "./libraries/Verifier.sol";

import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";

contract BMV is IBMV, Initializable {
    using MerkleTreeAccumulator for MerkleTreeAccumulator.MTA;
    using MerkleTreeAccumulator for bytes;
    using String for string;
    using String for address;
    using Base64 for bytes;
    using Base64 for string;
    using MessageDecoder for bytes;
    using MessageDecoder for Types.Validators;
    using Verifier for Types.BlockUpdate;
    using Verifier for Types.BlockProof;

    address private bmcAddr;
    address private subBmvAddr;
    string private netAddr;
    uint256 private lastBlockHeight;
    bytes32 internal lastBlockHash;
    Types.Validators private validators;
    MerkleTreeAccumulator.MTA internal mta;

    function initialize(
        address _bmcAddr,
        address _subBmvAddr,
        string memory _netAddr,
        bytes memory _rlpValidators,
        uint256 _offset,
        uint256 _rootsSize,
        uint256 _cacheSize,
        bytes32 _lastBlockHash
    ) public initializer {
        bmcAddr = _bmcAddr;
        subBmvAddr = _subBmvAddr;
        netAddr = _netAddr;
        validators.decodeValidators(_rlpValidators);
        mta.setOffset(_offset);
        mta.rootsSize = _rootsSize;
        mta.cacheSize = _cacheSize;
        mta.isAllowNewerWitness = true;
        lastBlockHeight = _offset;
        lastBlockHash = _lastBlockHash;
    }

    /**
        @return Base64 encode of Merkle Tree
     */
    function getMTA() external view override returns (string memory) {
        return mta.toBytes().encode();
    }

    /**
        @return connected BMC address
     */
    function getConnectedBMC() external view override returns (address) {
        return bmcAddr;
    }

    /**
        @return network address of the blockchain
     */
    function getNetAddress() external view override returns (string memory) {
        return netAddr;
    }

    /**
        @return hash of RLP encode from given list of validators
        @return list of validators' addresses
     */
    function getValidators()
        external
        view
        override
        returns (bytes32, address[] memory)
    {
        return (validators.validatorsHash, validators.validatorAddrs);
    }

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
            uint256,
            uint256,
            uint256
        )
    {
        return (mta.height, mta.offset, lastBlockHeight);
    }

    function getLastReceiptHash(Types.RelayMessage memory relayMsg)
        internal
        returns (bytes32 receiptHash, uint256 lastHeight)
    {
        for (uint256 i = 0; i < relayMsg.blockUpdates.length; i++) {
            // verify height
            require(
                relayMsg.blockUpdates[i].blockHeader.height <= mta.height + 1,
                "BMVRevertInvalidBlockUpdateHigher"
            );

            require(
                relayMsg.blockUpdates[i].blockHeader.height == mta.height + 1,
                "BMVRevertInvalidBlockUpdateLower"
            );

            // verify prev block hash
            if (i == 0)
                require(
                    relayMsg.blockUpdates[i].blockHeader.prevHash ==
                        lastBlockHash,
                    "BMVRevertInvalidBlockUpdate: Invalid block hash"
                );
            else {
                require(
                    relayMsg.blockUpdates[i].blockHeader.prevHash ==
                        relayMsg.blockUpdates[i - 1].blockHeader.blockHash,
                    "BMVRevertInvalidBlockUpdate: Invalid block hash"
                );
            }

            if (i == relayMsg.blockUpdates.length - 1) {
                receiptHash = relayMsg.blockUpdates[i]
                    .blockHeader
                    .result
                    .receiptHash;
                lastHeight = relayMsg.blockUpdates[i].blockHeader.height;
                lastBlockHash = relayMsg.blockUpdates[i].blockHeader.blockHash;
            }

            if (
                validators.validatorsHash !=
                relayMsg.blockUpdates[i].nextValidatorsHash ||
                i == relayMsg.blockUpdates.length - 1
            ) {
                if (relayMsg.blockUpdates[i].verifyValidators(validators)) {
                    delete validators;
                    validators.decodeValidators(
                        relayMsg.blockUpdates[i].nextValidatorsRlp
                    );
                }
            }

            mta.add(relayMsg.blockUpdates[i].blockHeader.blockHash);
        }

        if (!relayMsg.isBPEmpty) {
            relayMsg.blockProof.verifyMTAProof(mta);
            receiptHash = relayMsg.blockProof.blockHeader.result.receiptHash;
            lastHeight = relayMsg.blockProof.blockHeader.height;
        }

        return (receiptHash, lastHeight);
    }

    function checkAccessible(
        string memory _currentAddr,
        string memory _fromAddr
    ) internal view {
        (string memory _net, ) = _fromAddr.splitBTPAddress();
        require(netAddr.compareTo(_net), "BMVRevert: Invalid previous BMC");
        require(msg.sender == bmcAddr, "BMVRevert: Invalid BMC");
        (, string memory _contractAddr) = _currentAddr.splitBTPAddress();
        require(
            _contractAddr.parseAddress() == bmcAddr,
            "BMVRevert: Invalid BMC"
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
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    ) external override returns (bytes[] memory) {
        checkAccessible(_bmc, _prev);
        Types.RelayMessage memory relayMsg = _msg.decodeRelayMessage();
        require(
            relayMsg.blockUpdates.length != 0 || !relayMsg.isBPEmpty,
            "BMVRevert: Invalid relay message"
        );

        (bytes32 _receiptHash, uint256 _lastHeight) =
            getLastReceiptHash(relayMsg);

        bytes[] memory msgs =
            IDataValidator(subBmvAddr).validateReceipt(
                _bmc,
                _prev,
                _seq,
                _msg,
                _receiptHash
            );

        if (msgs.length > 0) lastBlockHeight = _lastHeight;
        return msgs;
    }
}
