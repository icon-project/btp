// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./LibRLPDecode.sol";
import "./LibTypes.sol";
import "./LibString.sol";
import "./LibBytes.sol";

library LibMsgDecoder {
    using LibRLPDecode for LibRLPDecode.RLPItem;
    using LibRLPDecode for bytes;
    using LibString for string;
    using LibString for address;
    using LibBytes for bytes;
    using LibBytes for bytes;
    using LibMsgDecoder for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeBlockHeader(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.BlockHeader memory)
    {
        //  Decode RLP bytes into a list of items
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        bool isSPREmpty = true;
        if (ls[10].toBytes().length == 0) {
            return
                LibTypes.BlockHeader(
                    keccak256(_rlp),
                    ls[0].toUint(),
                    ls[1].toUint(),
                    ls[2].toUint(),
                    ls[3].toBytes(),
                    ls[4].toBytes().bytesToBytes32(),
                    ls[5].toBytes().bytesToBytes32(),
                    ls[6].toBytes().bytesToBytes32(),
                    ls[7].toBytes(),
                    ls[8].toBytes(),
                    ls[9].toBytes(),
                    LibTypes.SPR("", "", ""),
                    isSPREmpty
                );
        }
        LibRLPDecode.RLPItem[] memory subList =
            ls[10].toBytes().toRlpItem().toList();
        isSPREmpty = false;
        return
            LibTypes.BlockHeader(
                keccak256(_rlp),
                ls[0].toUint(),
                ls[1].toUint(),
                ls[2].toUint(),
                ls[3].toBytes(),
                ls[4].toBytes().bytesToBytes32(),
                ls[5].toBytes().bytesToBytes32(),
                ls[6].toBytes().bytesToBytes32(),
                ls[7].toBytes(),
                ls[8].toBytes(),
                ls[9].toBytes(),
                LibTypes.SPR(
                    subList[0].toBytes().bytesToBytes32(),
                    subList[1].toBytes().bytesToBytes32(),
                    subList[2].toBytes().bytesToBytes32()
                ),
                isSPREmpty
            );
    }

    //  Votes item consists of:
    //  round as integer
    //  blockPartSetID is a list that consists of two items - integer and bytes
    //  and TS[] ts_list (an array of list)
    function decodeVotes(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.Votes memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        LibTypes.TS memory item;
        LibTypes.TS[] memory tsList = new LibTypes.TS[](ls[2].toList().length);
        LibRLPDecode.RLPItem[] memory rlpTs = ls[2].toList();
        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            item = LibTypes.TS(
                rlpTs[i].toList()[0].toUint(),
                rlpTs[i].toList()[1].toBytes()
            );
            tsList[i] = item;
        }
        return
            LibTypes.Votes(
                ls[0].toUint(),
                LibTypes.BPSI(
                    ls[1].toList()[0].toUint(),
                    ls[1].toList()[1].toBytes()
                ),
                tsList
            );
    }

    function decodeBlockWitness(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.BlockWitness memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        bytes32[] memory witnesses = new bytes32[](ls[1].toList().length);
        //  witnesses is an array of hash of leaf node
        //  The array size may also vary, thus loop is needed therein
        for (uint256 i = 0; i < ls[1].toList().length; i++) {
            witnesses[i] = ls[1].toList()[i].toBytes().bytesToBytes32();
        }
        return LibTypes.BlockWitness(ls[0].toUint(), witnesses);
    }

    function decodeEventProof(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.EventProof memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        LibRLPDecode.RLPItem[] memory data =
            ls[1].toBytes().toRlpItem().toList();

        bytes[] memory mptProofs = new bytes[](data.length);
        for (uint256 i = 0; i < data.length; i++) {
            mptProofs[i] = data[i].toBytes();
        }
        return
            LibTypes.EventProof(ls[0].toUint(), ls[0].toRlpBytes(), mptProofs);
    }

    function decodeBlockUpdate(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.BlockUpdate memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        LibTypes.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        LibTypes.Votes memory _v = ls[1].toBytes().decodeVotes();
        // LibTypes.Votes memory _v;

        //  BlockUpdate may or may not include the RLP of addresses of validators
        //  In that case, RLP_ENCODE([bytes]) == EMPTY_LIST_HEAD_START == 0xF800
        //  Thus, length of data will be 0. Therein, loop will be skipped
        //  and the _validators[] will be empty
        //  Otherwise, executing normally to read and assign value into the array _validators[]
        address[] memory _validators;
        bytes32 _validatorsHash;
        bytes memory _rlpBytes;
        if (ls[2].toBytes().length != 0) {
            _rlpBytes = ls[2].toBytes();
            // TODO: should use SHA3_256 instead
            _validatorsHash = keccak256(_rlpBytes);
            _validators = new address[](ls[2].toList().length);
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                _validators[i] = ls[2].toList()[i].toAddress();
            }
        }
        return
            LibTypes.BlockUpdate(
                _bh,
                _v,
                _validators,
                _rlpBytes,
                _validatorsHash
            );
    }

    function decodeReceiptProof(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.ReceiptProof memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        LibRLPDecode.RLPItem[] memory serializedMptProofs =
            ls[1].toBytes().toRlpItem().toList();

        bytes[] memory mptProofs = new bytes[](serializedMptProofs.length);
        for (uint256 i = 0; i < serializedMptProofs.length; i++) {
            mptProofs[i] = serializedMptProofs[i].toBytes();
        }

        LibTypes.EventProof[] memory eventProofs =
            new LibTypes.EventProof[](ls[2].toList().length);

        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            eventProofs[i] = ls[2].toList()[i].toRlpBytes().decodeEventProof();
        }

        return
            LibTypes.ReceiptProof(
                ls[0].toUint(),
                ls[0].toRlpBytes(),
                mptProofs,
                eventProofs
            );
    }

    function decodeEventLog(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.EventLog memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        string memory addr =
            ls[0].toBytes().slice(1, 21).bytesToAddress().addressToString(true);

        LibRLPDecode.RLPItem[] memory temp = ls[1].toList();
        bytes[] memory idxed = new bytes[](temp.length);
        for (uint256 i = 0; i < ls.length; i++) {
            idxed[i] = temp[i].toBytes();
        }
        delete temp;

        temp = ls[2].toList();
        bytes[] memory data = new bytes[](temp.length);
        for (uint256 i = 0; i < data.length; i++) {
            data[i] = temp[i].toBytes();
        }
        return LibTypes.EventLog(addr, idxed, data);
    }

    function decodeBlockProof(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.BlockProof memory)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        LibTypes.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        LibTypes.BlockWitness memory _bw = ls[1].toBytes().decodeBlockWitness();

        return LibTypes.BlockProof(_bh, _bw);
    }

    function decodeRelayMessage(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.RelayMessage memory)
    {
        //  _rlp.toRlpItem() removes the LIST_HEAD_START of RelayMessage
        //  then .toList() to itemize all fields in the RelayMessage
        //  which are [RLP_ENCODE(BlockUpdate)], RLP_ENCODE(BlockProof), and
        //  the RLP_ENCODE(ReceiptProof)
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        // return ls[0].toList()[0].toBytes().toRlpItem().toList()[0].toBytes().toRlpItem().toList()[8].toBytes();

        //  If [RLP_ENCODE(BlockUpdate)] was empty, it should be started by 0xF800
        //  therein, ls[0].toBytes() will be null (length = 0)
        //  Otherwise, create an array of BlockUpdate struct to decode
        LibTypes.BlockUpdate[] memory _buArray;
        if (ls[0].toBytes().length != 0) {
            _buArray = new LibTypes.BlockUpdate[](ls[0].toList().length);
            for (uint256 i = 0; i < ls[0].toList().length; i++) {
                //  Each of items inside an array [RLP_ENCODE(BlockUpdate)]
                //  is a string which defines RLP_ENCODE(BlockUpdate)
                //  that contains a LIST_HEAD_START and multiple RLP of data
                //  ls[0].toList()[i].toBytes() returns bytes presentation of
                //  RLP_ENCODE(BlockUpdate)
                _buArray[i] = ls[0].toList()[i].toBytes().decodeBlockUpdate();
            }
        }

        bool isBPEmpty = true;
        LibTypes.BlockProof memory _bp;
        //  If RLP_ENCODE(BlockProof) is omitted,
        //  ls[1].toBytes() should be null (length = 0)
        if (ls[1].toBytes().length != 0) {
            _bp = ls[1].toBytes().decodeBlockProof();
            isBPEmpty = false; //  add this field into RelayMessage
            //  to specify whether BlockProof is omitted
            //  to make it easy on encoding
            //  it will not be serialized thereafter
        }

        return LibTypes.RelayMessage(_buArray, _bp, isBPEmpty);
    }

    function decodeReceiptProofs(bytes memory _rlp)
        internal
        pure
        returns (LibTypes.ReceiptProof[] memory _rp)
    {
        LibRLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        if (ls[2].toBytes().length != 0) {
            _rp = new LibTypes.ReceiptProof[](ls[2].toList().length);
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                _rp[i] = ls[2].toList()[i].toBytes().decodeReceiptProof();
            }
        }
    }

    function decodeValidators(
        LibTypes.Validators storage validators,
        bytes memory _rlp
    ) internal {
        validators.serializedBytes = _rlp;
        // TODO: should use SHA3_256 instead
        validators.validatorsHash = keccak256(validators.serializedBytes);

        LibRLPDecode.RLPItem[] memory validatorsList =
            validators.serializedBytes.toRlpItem().toList();
        address[] memory newVals = new address[](validatorsList.length);
        for (uint256 i = 0; i < validatorsList.length; i++) {
            newVals[i] = validatorsList[i].toAddress();
            validators.containedValidators[newVals[i]] = true;
        }
        validators.validatorAddrs = newVals;
    }

    function toMessageEvent(LibTypes.EventLog memory eventLog)
        internal
        pure
        returns (LibTypes.MessageEvent memory)
    {
        string memory method = string(eventLog.idx[0]).split("(")[0];
        if (method.compareTo("Message")) {
            return
                LibTypes.MessageEvent(
                    string(eventLog.idx[1]),
                    eventLog.idx[2].bytesToUint8(0),
                    eventLog.data[0]
                );
        }
        return LibTypes.MessageEvent("", 0, "");
    }
}
