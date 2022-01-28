// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./RLPDecode.sol";
import "./Types.sol";
import "./String.sol";
import "./Bytes.sol";
import "./Hash.sol";

library MessageDecoder {
    using RLPDecode for RLPDecode.RLPItem;
    using RLPDecode for bytes;
    using String for string;
    using String for address;
    using Bytes for bytes;
    using Bytes for bytes;
    using MessageDecoder for bytes;
    using MessageDecoder for RLPDecode.RLPItem;
    using Hash for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeBlockHeader(bytes memory _rlp)
        internal
        returns (Types.BlockHeader memory)
    {
        //  Decode RLP bytes into a list of items
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        bool isResultEmpty = true;
        if (ls[10].toBytes().length == 0) {
            return
                Types.BlockHeader(
                    _rlp.sha3FIPS256(),
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
                    Types.Result("", "", "", ""),
                    isResultEmpty
                );
        }
        RLPDecode.RLPItem[] memory subList =
            ls[10].toBytes().toRlpItem().toList();
        isResultEmpty = false;
        return
            Types.BlockHeader(
                _rlp.sha3FIPS256(),
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
                Types.Result(
                    subList[0].toBytes().bytesToBytes32(),
                    subList[1].toBytes().bytesToBytes32(),
                    subList[2].toBytes().bytesToBytes32(),
                    subList.length == 4 ? subList[3].toBytes() : bytes("")
                ),
                isResultEmpty
            );
    }

    //  Votes item consists of:
    //  round as integer
    //  blockPartSetID is a list that consists of two items - integer and bytes
    //  and TS[] ts_list (an array of list)
    function decodeVotes(bytes memory _rlp)
        internal
        pure
        returns (Types.Votes memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.TS memory item;
        Types.TS[] memory tsList;
        Types.Votes memory votes;
        if (ls.length > 0) {
            tsList = new Types.TS[](ls[2].toList().length);
            RLPDecode.RLPItem[] memory rlpTs = ls[2].toList();
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                item = Types.TS(
                    rlpTs[i].toList()[0].toUint(),
                    rlpTs[i].toList()[1].toBytes()
                );
                tsList[i] = item;
            }

            votes = Types.Votes(
                ls[0].toUint(),
                Types.BPSI(
                    ls[1].toList()[0].toUint(),
                    ls[1].toList()[1].toBytes()
                ),
                tsList
            );
        }
        return votes;
    }

    function decodeBlockWitness(RLPDecode.RLPItem memory _rlpItem)
        internal
        pure
        returns (Types.BlockWitness memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlpItem.toList();

        bytes32[] memory witnesses = new bytes32[](ls[1].toList().length);

        for (uint256 i = 0; i < ls[1].toList().length; i++) {
            witnesses[i] = ls[1].toList()[i].toBytes().bytesToBytes32();
        }
        return Types.BlockWitness(ls[0].toUint(), witnesses);
    }

    function decodeEventProof(bytes memory _rlp)
        internal
        pure
        returns (Types.EventProof memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPDecode.RLPItem[] memory data = ls[1].toBytes().toRlpItem().toList();

        bytes[] memory mptProofs = new bytes[](data.length);
        for (uint256 i = 0; i < data.length; i++) {
            mptProofs[i] = data[i].toBytes();
        }
        return Types.EventProof(ls[0].toUint(), ls[0].toRlpBytes(), mptProofs);
    }

    function decodeBlockUpdate(bytes memory _rlp, bool _isLastBlock)
        internal
        returns (Types.BlockUpdate memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        Types.Votes memory _v;

        if(_isLastBlock == true){
            _v = ls[1].toBytes().decodeVotes();
        }
        // Types.Votes memory _v;

        //  BlockUpdate may or may not include the RLP of addresses of validators
        //  In that case, RLP_ENCODE([bytes]) == EMPTY_LIST_HEAD_START == 0xF800
        //  Thus, length of data will be 0. Therein, loop will be skipped
        //  and the _validators[] will be empty
        //  Otherwise, executing normally to read and assign value into the array _validators[]
        address[] memory _validators;
        bytes32 _validatorsHash;
        bytes memory _rlpBytes;
        if (ls[2].toBytes().length != 0) {
            RLPDecode.RLPItem[] memory _rlpItems =
                ls[2].toBytes().toRlpItem().toList();
            _rlpBytes = ls[2].toBytes();
            _validatorsHash = _rlpBytes.sha3FIPS256();
            _validators = new address[](_rlpItems.length);
            for (uint256 i = 0; i < _rlpItems.length; i++) {
                _validators[i] = _rlpItems[i]
                    .toBytes()
                    .slice(1, 21)
                    .bytesToAddress();
            }
        }
        return
            Types.BlockUpdate(_bh, _v, _validators, _rlpBytes, _validatorsHash);
    }

    function decodeReceiptProof(bytes memory _rlp)
        internal
        pure
        returns (Types.ReceiptProof memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPDecode.RLPItem[] memory serializedMptProofs =
            ls[1].toBytes().toRlpItem().toList();

        bytes[] memory mptProofs = new bytes[](serializedMptProofs.length);
        for (uint256 i = 0; i < serializedMptProofs.length; i++) {
            mptProofs[i] = serializedMptProofs[i].toBytes();
        }

        Types.EventProof[] memory eventProofs =
            new Types.EventProof[](ls[2].toList().length);

        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            eventProofs[i] = ls[2].toList()[i].toRlpBytes().decodeEventProof();
        }

        return
            Types.ReceiptProof(
                ls[0].toUint(),
                ls[0].toRlpBytes(),
                mptProofs,
                eventProofs
            );
    }

    function decodeEventLog(bytes memory _rlp)
        internal
        pure
        returns (Types.EventLog memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        string memory addr =
            ls[0].toBytes().slice(1, 21).bytesToAddress().addressToString(true);

        RLPDecode.RLPItem[] memory temp = ls[1].toList();
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
        return Types.EventLog(addr, idxed, data);
    }

    function decodeBlockProof(bytes memory _rlp)
        internal
        returns (Types.BlockProof memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        Types.BlockWitness memory _bw = ls[1].decodeBlockWitness();

        return Types.BlockProof(_bh, _bw);
    }

    function decodeRelayMessage(bytes memory _rlp)
        internal
        returns (Types.RelayMessage memory)
    {
        //  _rlp.toRlpItem() removes the LIST_HEAD_START of RelayMessage
        //  then .toList() to itemize all fields in the RelayMessage
        //  which are [RLP_ENCODE(BlockUpdate)], RLP_ENCODE(BlockProof), and
        //  the RLP_ENCODE(ReceiptProof)
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        // return ls[0].toList()[0].toBytes().toRlpItem().toList()[0].toBytes().toRlpItem().toList()[8].toBytes();

        //  If [RLP_ENCODE(BlockUpdate)] was empty, it should be started by 0xF800
        //  therein, ls[0].toBytes() will be null (length = 0)
        //  Otherwise, create an array of BlockUpdate struct to decode
        Types.BlockUpdate[] memory _buArray;
        if (ls[0].toBytes().length != 0) {
            uint256 _buLength = ls[0].toList().length;
            uint256 _lastIndex = _buLength - 1;
            _buArray = new Types.BlockUpdate[](_buLength);
            for (uint256 i = 0; i < _buLength; i++) {
                //  Each of items inside an array [RLP_ENCODE(BlockUpdate)]
                //  is a string which defines RLP_ENCODE(BlockUpdate)
                //  that contains a LIST_HEAD_START and multiple RLP of data
                //  ls[0].toList()[i].toBytes() returns bytes presentation of
                //  RLP_ENCODE(BlockUpdate)
                bool _isLastBlock = false;

                if (_lastIndex == i) {
                    _isLastBlock = true;
                }

                _buArray[i] = ls[0].toList()[i].toBytes().decodeBlockUpdate(_isLastBlock);
            }
        }

        bool isBPEmpty = true;
        Types.BlockProof memory _bp;
        //  If RLP_ENCODE(BlockProof) is omitted,
        //  ls[1].toBytes() should be null (length = 0)
        if (ls[1].toBytes().length != 0) {
            _bp = ls[1].toBytes().decodeBlockProof();
            isBPEmpty = false; //  add this field into RelayMessage
            //  to specify whether BlockProof is omitted
            //  to make it easy on encoding
            //  it will not be serialized thereafter
        }

        return Types.RelayMessage(_buArray, _bp, isBPEmpty);
    }

    function decodeReceiptProofs(bytes memory _rlp)
        internal
        pure
        returns (Types.ReceiptProof[] memory _rp)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        if (ls[2].toBytes().length != 0) {
            _rp = new Types.ReceiptProof[](ls[2].toList().length);
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                _rp[i] = ls[2].toList()[i].toBytes().decodeReceiptProof();
            }
        }
    }

    function decodeValidators(
        Types.Validators storage validators,
        bytes memory _rlp
    ) internal {
        validators.serializedBytes = _rlp;
        validators.validatorsHash = validators.serializedBytes.sha3FIPS256();

        RLPDecode.RLPItem[] memory _rlpItems =
            validators.serializedBytes.toRlpItem().toList();
        address[] memory newVals = new address[](_rlpItems.length);
        for (uint256 i = 0; i < _rlpItems.length; i++) {
            newVals[i] = _rlpItems[i].toBytes().slice(1, 21).bytesToAddress();
            validators.containedValidators[newVals[i]] = true;
        }
        validators.validatorAddrs = newVals;
    }

    function toMessageEvent(Types.EventLog memory eventLog)
        internal
        pure
        returns (Types.MessageEvent memory)
    {
        string memory method = string(eventLog.idx[0]).split("(")[0];
        if (method.compareTo("Message")) {
            return
                Types.MessageEvent(
                    string(eventLog.idx[1]),
                    eventLog.idx[2].toRlpItem().toUint(),
                    eventLog.data[0]
                );
        }
        return Types.MessageEvent("", 0, "");
    }
}
