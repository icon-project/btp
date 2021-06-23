// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./interfaces/IBMV.sol";
import "./interfaces/IDataValidator.sol";

import "./libraries/LibBase64.sol";
import "./libraries/LibMTA.sol";
import "./libraries/LibString.sol";
import "./libraries/LibTypes.sol";
import "./libraries/LibMsgDecoder.sol";
import "./libraries/LibVerifier.sol";

import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";

contract BMV is IBMV, Initializable {
    using LibMerkleTreeAccumulator for LibMerkleTreeAccumulator.MTA;
    using LibMerkleTreeAccumulator for bytes;
    using LibString for string;
    using LibString for address;
    using LibBase64 for bytes;
    using LibBase64 for string;
    using LibMsgDecoder for bytes;
    using LibMsgDecoder for LibTypes.Validators;
    using LibVerifier for LibTypes.BlockUpdate;
    using LibVerifier for LibTypes.BlockProof;

    address private bmcAddr;
    address private subBmvAddr;
    string private netAddr;
    uint256 private lastBlockHeight;
    bytes32 private lastBlockHash;
    LibTypes.Validators private validators;
    LibMerkleTreeAccumulator.MTA private mta;

    /**
        @notice Shouldn't check tx.origin == BMC operator's address since it has a security issue.
                Ref https://docs.soliditylang.org/en/v0.6.2/security-considerations.html#tx-origin.
                BMC Operator should add BMV manually using addVerifier() function.
     */
    function initialize(
        address _bmcAddr,
        address _subBmvAddr,
        string memory _netAddr,
        string memory _validators,
        uint256 _offset,
        uint256 _rootsSize,
        uint256 _cacheSize,
        bytes32 _lastBlockHash
    ) public initializer {
        bmcAddr = _bmcAddr;
        subBmvAddr = _subBmvAddr;
        netAddr = _netAddr;
        validators.decodeValidators(_validators.decode());
        mta.setOffset(_offset);
        mta.rootsSize = _rootsSize;
        mta.cacheSize = _cacheSize;
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

    function getLastReceiptHash(LibTypes.RelayMessage memory relayMsg)
        internal
        returns (bytes32 receiptHash, uint256 lastHeight)
    {
        for (uint256 i = 0; i < relayMsg.blockUpdates.length; i++) {
            // // verify prev block hash
            // if (i == 0)
            //     require(
            //         relayMsg.blockUpdates[i].blockHeader.prevHash ==
            //             lastBlockHash,
            //         "BMVRevertInvalidBlockUpdate: Invalid block hash"
            //     );
            // else {
            //     require(
            //         relayMsg.blockUpdates[i].blockHeader.prevHash ==
            //             relayMsg.blockUpdates[i - 1].blockHeader.blockHash,
            //         "BMVRevertInvalidBlockUpdate: Invalid block hash"
            //     );
            // }

            // // verify height
            // require(
            //     relayMsg.blockUpdates[i].blockHeader.height >= mta.height + 1,
            //     "BMVRevertInvalidBlockUpdateHigher"
            // );

            // require(
            //     relayMsg.blockUpdates[i].blockHeader.height < mta.height + 1,
            //     "BMVRevertInvalidBlockUpdateLower"
            // );

            // only verify validators of last block for saving gas
            if (i == relayMsg.blockUpdates.length - 1) {
                // require(
                //     relayMsg.blockUpdates[i].verifyValidators(validators),
                //     "BMVRevertInvalidBlockUpdate"
                // );
                receiptHash = relayMsg.blockUpdates[i]
                    .blockHeader
                    .result
                    .receiptHash;
                lastHeight = relayMsg.blockUpdates[i].blockHeader.height;
                lastBlockHash = relayMsg.blockUpdates[i].blockHeader.blockHash;
            }

            // TODO: pending for SHA3-256
            // if (validators.validatorsHash != relayMsg.blockUpdates[i].nextValidatorsHash) {
            if (relayMsg.blockUpdates[i].nextValidators.length > 0) {
                delete validators;
                validators.decodeValidators(
                    relayMsg.blockUpdates[i].nextValidatorsRlp
                );
            }

            mta.add(relayMsg.blockUpdates[i].blockHeader.blockHash);
        }

        if (relayMsg.blockProof.blockWitness.witnesses.length != 0) {
            relayMsg.blockProof.verifyMTAProof(mta);
            receiptHash = relayMsg.blockProof.blockHeader.result.receiptHash;
            lastHeight = relayMsg.blockProof.blockHeader.height;
        }

        return (receiptHash, lastHeight);
    }

    function checkAccessible(string memory currentAddr, string memory fromAddr)
        internal
        view
    {
        string memory net;
        string memory contractAddr;
        (net, ) = fromAddr.splitBTPAddress();
        require(netAddr.compareTo(net), "BMVRevert: Invalid previous BMC");
        require(msg.sender == bmcAddr, "BMVRevert: Invalid BMC");
        (, contractAddr) = currentAddr.splitBTPAddress();
        require(
            contractAddr.parseAddress() == bmcAddr,
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
        string calldata _msg
    ) external override returns (bytes[] memory) {
        checkAccessible(_bmc, _prev);

        bytes memory _serializedMsg = _msg.decode();
        LibTypes.RelayMessage memory relayMsg =
            _serializedMsg.decodeRelayMessage();

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
                _serializedMsg,
                _receiptHash
            );

        if (msgs.length > 0) lastBlockHeight = _lastHeight;
        return msgs;
    }
}
