// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./interfaces/IBMV.sol";
import "./interfaces/IBMC.sol";

import "./libraries/LibBase64.sol";
import "./libraries/LibMTA.sol";
import "./libraries/LibString.sol";
import "./libraries/LibTypes.sol";
import "./libraries/LibMsgDecoder.sol";
import "./libraries/LibVerifier.sol";

contract BMV is IBMV {
    using LibMerkleTreeAccumulator for LibMerkleTreeAccumulator.MTA;
    using LibMerkleTreeAccumulator for bytes;
    using LibString for string;
    using LibString for address;
    using LibBase64 for bytes;
    using LibBase64 for string;
    using LibMsgDecoder for bytes;
    using LibMsgDecoder for string;
    using LibMsgDecoder for LibTypes.Validators;
    using LibMsgDecoder for LibTypes.EventLog;
    using LibVerifier for LibTypes.BlockUpdate;
    using LibVerifier for LibTypes.BlockProof;
    using LibVerifier for LibTypes.ReceiptProof;

    address private bmcAddr;
    string private netAddr;
    uint256 private lastBlockHeight;
    bytes32 private lastBlockHash;
    LibTypes.Validators private validators;
    LibMerkleTreeAccumulator.MTA private mta;
    IBMC private bmc;
    bytes[] private msgs;

    constructor(
        address _bmcAddr,
        string memory _netAddr,
        string memory _validators,
        uint256 _offset,
        bytes32 _lastBlockHash
    ) {
        bmcAddr = _bmcAddr;
        netAddr = _netAddr;
        validators.decodeValidators(_validators.decode());
        mta.setOffset(_offset);
        lastBlockHash = _lastBlockHash;

        bmc = IBMC(_bmcAddr);
        bmc.addVerifier(_netAddr, address(this));
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
                //     "BMV Exception: Invalid validators in block updates"
                // );
                receiptHash = relayMsg.blockUpdates[i]
                    .blockHeader
                    .spr
                    .receiptHash;
                lastHeight = relayMsg.blockUpdates[i].blockHeader.height;
                lastBlockHash = relayMsg.blockUpdates[i].blockHeader.blockHash;
            }

            delete validators;
            if (relayMsg.blockUpdates[i].nextValidators.length > 0)
                validators.decodeValidators(
                    relayMsg.blockUpdates[i].nextValidatorsRlp
                );
            mta.add(relayMsg.blockUpdates[i].blockHeader.blockHash);
        }

        if (relayMsg.blockProof.blockWitness.witnesses.length != 0) {
            relayMsg.blockProof.verifyMTAProof(mta);
            receiptHash = relayMsg.blockProof.blockHeader.spr.receiptHash;
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
            contractAddr.compareTo(bmcAddr.addressToString(false)),
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

        LibTypes.RelayMessage memory relayMsg =
            _msg.decode().decodeRelayMessage();
        if (relayMsg.blockUpdates.length == 0 && relayMsg.isBPEmpty)
            revert(
                "BMVRevert: Invalid relay message - not exists BlockUpdate or BlockProof"
            );

        bytes32 receiptHash;
        uint256 lastHeight;
        (receiptHash, lastHeight) = getLastReceiptHash(relayMsg);

        uint256 nextSeq = _seq + 1;
        LibTypes.Receipt memory receipt;
        LibTypes.MessageEvent memory messageEvent;
        string memory contractAddr;
        if (msgs.length > 0) delete msgs;
        for (uint256 i = 0; i < relayMsg.receiptProof.length; i++) {
            receipt = relayMsg.receiptProof[i].verifyMPTProof(receiptHash);
            for (uint256 j = 0; j < receipt.eventLogs.length; j++) {
                (, contractAddr) = _prev.splitBTPAddress();
                if (!receipt.eventLogs[j].addr.compareTo(contractAddr))
                    continue;
                messageEvent = receipt.eventLogs[j].toMessageEvent();
                if (bytes(messageEvent.nextBmc).length != 0) {
                    if (messageEvent.seq > nextSeq)
                        revert("BMVRevertInvalidSequenceHigher");
                    else if (messageEvent.seq < nextSeq)
                        revert("BMVRevertInvalidSequence");
                    else if (
                        // TODO: pending for integration test
                        // messageEvent.nextBmc.compareTo(_bmc)
                        bytes(messageEvent.nextBmc).length > 0
                    ) {
                        msgs.push(messageEvent.message);
                        nextSeq += 1;
                    }
                }
            }
        }

        if (msgs.length > 0) lastBlockHeight = lastHeight;
        return msgs;
    }
}
