// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./RLPReaderLib.sol";
import "./TypesLib.sol";

library RLPDecodeStruct {
    using RLPReader for RLPReader.RLPItem;
    using RLPReader for RLPReader.Iterator;
    using RLPReader for bytes;

    using RLPDecodeStruct for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeBMCService(bytes memory _rlp)
        internal
        pure
        returns (Types.BMCService memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.BMCService(
                string(ls[0].toBytes()),
                ls[1].toBytes() //  bytes array of RLPEncode(Data)
            );
    }

    function decodeGatherFeeMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.GatherFeeMessage memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPReader.RLPItem[] memory subList = ls[1].toList();
        string[] memory _svcs = new string[](subList.length);
        for (uint256 i = 0; i < subList.length; i++) {
            _svcs[i] = string(subList[i].toBytes());
        }
        return Types.GatherFeeMessage(string(ls[0].toBytes()), _svcs);
    }

    function decodeEventMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.EventMessage memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.EventMessage(
                string(ls[0].toBytes()),
                Types.Connection(
                    string(ls[1].toList()[0].toBytes()),
                    string(ls[1].toList()[1].toBytes())
                )
            );
    }

    function decodeRegisterCoin(bytes memory _rlp)
        internal
        pure
        returns (Types.RegisterCoin memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.RegisterCoin(
                string(ls[0].toBytes()),
                ls[1].toUint(),
                string(ls[2].toBytes())
            );
    }

    function decodeBMCMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.BMCMessage memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.BMCMessage(
                string(ls[0].toBytes()),
                string(ls[1].toBytes()),
                string(ls[2].toBytes()),
                ls[3].toInt(),
                ls[4].toBytes() //  bytes array of RLPEncode(ServiceMessage)
            );
    }

    function decodeServiceMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.ServiceMessage memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.ServiceMessage(
                Types.ServiceType(ls[0].toUint()),
                ls[1].toBytes() //  bytes array of RLPEncode(Data)
            );
    }

    function decodeTransferCoinMsg(bytes memory _rlp)
        internal
        pure
        returns (Types.TransferCoin memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        Types.Asset[] memory assets = new Types.Asset[](ls[2].toList().length);
        RLPReader.RLPItem[] memory rlpAssets = ls[2].toList();
        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            assets[i] = Types.Asset(
                string(rlpAssets[i].toList()[0].toBytes()),
                rlpAssets[i].toList()[1].toUint()
            );
        }
        return
            Types.TransferCoin(
                string(ls[0].toBytes()),
                string(ls[1].toBytes()),
                assets
            );
    }

    function decodeResponse(bytes memory _rlp)
        internal
        pure
        returns (Types.Response memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return Types.Response(ls[0].toUint(), string(ls[1].toBytes()));
    }

    function decodeBlockHeader(bytes memory _rlp)
        internal
        pure
        returns (Types.BlockHeader memory)
    {
        //  Decode RLP bytes into a list of items
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        bool isSPREmpty = true;
        if (ls[10].toBytes().length == 0) {
            return
                Types.BlockHeader(
                    ls[0].toUint(),
                    ls[1].toUint(),
                    ls[2].toUint(),
                    ls[3].toBytes(),
                    ls[4].toBytes(),
                    ls[5].toBytes(),
                    ls[6].toBytes(),
                    ls[7].toBytes(),
                    ls[8].toBytes(),
                    ls[9].toBytes(),
                    Types.SPR("", "", ""),
                    isSPREmpty
                );
        }
        RLPReader.RLPItem[] memory subList =
            ls[10].toBytes().toRlpItem().toList();
        isSPREmpty = false;
        return
            Types.BlockHeader(
                ls[0].toUint(),
                ls[1].toUint(),
                ls[2].toUint(),
                ls[3].toBytes(),
                ls[4].toBytes(),
                ls[5].toBytes(),
                ls[6].toBytes(),
                ls[7].toBytes(),
                ls[8].toBytes(),
                ls[9].toBytes(),
                Types.SPR(
                    subList[0].toBytes(),
                    subList[1].toBytes(),
                    subList[2].toBytes()
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
        returns (Types.Votes memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.TS[] memory tsList = new Types.TS[](ls[2].toList().length);
        RLPReader.RLPItem[] memory rlpTs = ls[2].toList();
        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            tsList[i] = Types.TS(
                rlpTs[i].toList()[0].toUint(),
                rlpTs[i].toList()[1].toBytes()
            );
        }
        return
            Types.Votes(
                ls[0].toUint(),
                Types.BPSI(
                    ls[1].toList()[0].toUint(),
                    ls[1].toList()[1].toBytes()
                ),
                tsList
            );
    }

    //  Wait for confirmation
    function decodeBlockWitness(bytes memory _rlp)
        internal
        pure
        returns (Types.BlockWitness memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        bytes[] memory witnesses = new bytes[](ls[1].toList().length);
        //  witnesses is an array of hash of leaf node
        //  The array size may also vary, thus loop is needed therein
        for (uint256 i = 0; i < ls[1].toList().length; i++) {
            witnesses[i] = ls[1].toList()[i].toBytes();
        }
        return Types.BlockWitness(ls[0].toUint(), witnesses);
    }

    function decodeEventProof(bytes memory _rlp)
        internal
        pure
        returns (Types.EventProof memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPReader.RLPItem[] memory data = ls[1].toBytes().toRlpItem().toList();

        bytes[] memory eventMptNode = new bytes[](data.length);
        for (uint256 i = 0; i < data.length; i++) {
            eventMptNode[i] = data[i].toBytes();
        }
        return Types.EventProof(ls[0].toUint(), eventMptNode);
    }

    function decodeBlockUpdate(bytes memory _rlp)
        internal
        pure
        returns (Types.BlockUpdate memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        // Types.BlockHeader memory _bh;
        Types.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        Types.Votes memory _v = ls[1].toBytes().decodeVotes();
        // Types.Votes memory _v;

        //  BlockUpdate may or may not include the RLP of addresses of validators
        //  In that case, RLP_ENCODE([bytes]) == EMPTY_LIST_HEAD_START == 0xF800
        //  Thus, length of data will be 0. Therein, loop will be skipped
        //  and the _validators[] will be empty
        //  Otherwise, executing normally to read and assign value into the array _validators[]
        bytes[] memory _validators;
        if (ls[2].toBytes().length != 0) {
            _validators = new bytes[](ls[2].toList().length);
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                _validators[i] = ls[2].toList()[i].toBytes();
            }
        }
        return Types.BlockUpdate(_bh, _v, _validators);
    }

    function decodeReceiptProof(bytes memory _rlp)
        internal
        pure
        returns (Types.ReceiptProof memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPReader.RLPItem[] memory receiptList =
            ls[1].toBytes().toRlpItem().toList();

        bytes[] memory txReceipts = new bytes[](receiptList.length);
        for (uint256 i = 0; i < receiptList.length; i++) {
            txReceipts[i] = receiptList[i].toBytes();
        }

        Types.EventProof[] memory _ep =
            new Types.EventProof[](ls[2].toList().length);
        for (uint256 i = 0; i < ls[2].toList().length; i++) {
            _ep[i] = Types.EventProof(
                ls[2].toList()[i].toList()[0].toUint(),
                ls[2].toList()[i].toList()[1].toBytes().decodeEventLog()
            );
        }

        return Types.ReceiptProof(ls[0].toUint(), txReceipts, _ep);
    }

    function decodeEventLog(bytes memory _rlp)
        internal
        pure
        returns (bytes[] memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        bytes[] memory eventMptNode = new bytes[](ls.length);
        for (uint256 i = 0; i < ls.length; i++) {
            eventMptNode[i] = ls[i].toBytes();
        }
        return eventMptNode;
    }

    function decodeBlockProof(bytes memory _rlp)
        internal
        pure
        returns (Types.BlockProof memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.BlockHeader memory _bh = ls[0].toBytes().decodeBlockHeader();
        Types.BlockWitness memory _bw = ls[1].toBytes().decodeBlockWitness();

        return Types.BlockProof(_bh, _bw);
    }

    function decodeRelayMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.RelayMessage memory)
    {
        //  _rlp.toRlpItem() removes the LIST_HEAD_START of RelayMessage
        //  then .toList() to itemize all fields in the RelayMessage
        //  which are [RLP_ENCODE(BlockUpdate)], RLP_ENCODE(BlockProof), and
        //  the RLP_ENCODE(ReceiptProof)
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        // return (
        //     ls[0].toList()[0].toBytes().toRlpItem().toList()[1].toBytes().toRlpItem().toList()[2].toList()[0].toList()[1].toBytes()
        // );

        //  If [RLP_ENCODE(BlockUpdate)] was empty, it should be started by 0xF800
        //  therein, ls[0].toBytes() will be null (length = 0)
        //  Otherwise, create an array of BlockUpdate struct to decode
        Types.BlockUpdate[] memory _buArray;
        if (ls[0].toBytes().length != 0) {
            _buArray = new Types.BlockUpdate[](ls[0].toList().length);
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

        bool isRPEmpty = true;
        Types.ReceiptProof[] memory _rp;
        //  If [RLP_ENCODE(ReceiptProof)] is omitted,
        //  ls[2].toBytes() should be null (length = 0)
        if (ls[2].toBytes().length != 0) {
            _rp = new Types.ReceiptProof[](ls[2].toList().length);
            for (uint256 i = 0; i < ls[2].toList().length; i++) {
                _rp[i] = ls[2].toList()[i].toBytes().decodeReceiptProof();
            }
            isRPEmpty = false; //  add this field into RelayMessage
            //  to specify whether ReceiptProof is omitted
            //  to make it easy on encoding
            //  it will not be serialized thereafter
        }
        return Types.RelayMessage(_buArray, _bp, isBPEmpty, _rp, isRPEmpty);
    }
}
