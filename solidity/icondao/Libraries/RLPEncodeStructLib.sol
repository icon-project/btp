pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./RLPEncodeLib.sol";
import "./TypesLib.sol";

library RLPEncodeStruct {
    using RLPEncode for bytes;
    using RLPEncode for string;
    using RLPEncode for uint256;
    using RLPEncode for address;

    using RLPEncodeStruct for Types.BlockHeader;
    using RLPEncodeStruct for Types.BlockWitness;
    using RLPEncodeStruct for Types.BlockUpdate;
    using RLPEncodeStruct for Types.BlockProof;
    using RLPEncodeStruct for Types.EventProof;
    using RLPEncodeStruct for Types.ReceiptProof;
    using RLPEncodeStruct for Types.Votes;
    using RLPEncodeStruct for Types.RelayMessage;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function encodeEventMessage(Types.EventMessage memory _em)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat3(
                uint256(_em.eventType).encodeUint(),
                _em.src.encodeString(),
                _em.dst.encodeString()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeRegisterCoin(Types.RegisterCoin memory _rc)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat3(
                _rc.coinName.encodeString(),
                _rc.id.encodeUint(),
                _rc.symbol.encodeString()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeBMCMessage(Types.BMCMessage memory _bm)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat5(
                _bm.src.encodeString(),
                _bm.dst.encodeString(),
                _bm.svc.encodeString(),
                _bm.sn.encodeUint(),
                _bm.message.encodeBytes()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeServiceMessage(Types.ServiceMessage memory _sm)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat2(
                uint256(_sm.serviceType).encodeUint(),
                _sm.data.encodeBytes()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeData(Types.TransferCoin memory _data)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat4(
                _data.from.encodeString(),
                _data.to.encodeString(),
                _data.coinName.encodeString(),
                _data.value.encodeUint()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeResponse(Types.Response memory _res)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat2(_res.code.encodeUint(), _res.message.encodeString());
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeBlockHeader(Types.BlockHeader memory _bh)
        internal
        pure
        returns (bytes memory)
    {
        // Serialize the first 10 items in the BlockHeader
        //  patchTxHash and txHash might be empty.
        //  In that case, encoding these two items gives the result as 0xF800
        //  Similarly, logsBloom might be also empty
        //  But, encoding this item gives the result as 0x80
        bytes memory _rlp =
            concat5(
                _bh.version.encodeUint(),
                _bh.height.encodeUint(),
                _bh.timestamp.encodeUint(),
                _bh.proposer.encodeBytes(),
                _bh.prevHash.encodeBytes()
            );
        bytes memory temp1;
        bytes memory temp2;
        if (_bh.patchTxHash.length != 0) {
            temp1 = _bh.patchTxHash.encodeBytes();
        } else {
            temp1 = emptyListHeadStart();
        }

        if (_bh.txHash.length != 0) {
            temp2 = _bh.txHash.encodeBytes();
        } else {
            temp2 = emptyListHeadStart();
        }

        bytes memory temp3 =
            concat5(
                _bh.voteHash.encodeBytes(),
                _bh.nextValidators.encodeBytes(),
                temp1,
                temp2,
                _bh.logsBloom.encodeBytes()
            );
        _rlp = concat2(_rlp, temp3);

        uint256 length;
        //  SPR struct could be an empty struct
        //  In that case, serialize(SPR) = 0xF800
        if (_bh.isSPREmpty) {
            temp3 = emptyListHeadStart();
        } else {
            //  patchReceiptHash and receiptHash might be empty
            //  In that case, encoding these two items gives the result as 0xF800
            if (_bh.spr.patchReceiptHash.length != 0) {
                temp1 = _bh.spr.patchReceiptHash.encodeBytes();
            } else {
                temp1 = emptyListHeadStart();
            }

            if (_bh.spr.receiptHash.length != 0) {
                temp2 = _bh.spr.receiptHash.encodeBytes();
            } else {
                temp2 = emptyListHeadStart();
            }
            temp3 = concat3(_bh.spr.stateHash.encodeBytes(), temp1, temp2);
            length = numOfBytes(temp3);
            temp1 = addLength(length, false);
            temp3 = concat2(temp1, temp3).encodeBytes();
        }
        _rlp = concat2(_rlp, temp3);
        length = numOfBytes(_rlp);
        temp1 = addLength(length, false);
        return concat2(temp1, _rlp);
    }

    function encodeVotes(Types.Votes memory _vote)
        internal
        pure
        returns (bytes memory)
    {
        uint256 length;
        bytes memory listSize;
        bytes memory _rlp;
        bytes memory temp;

        //  First, serialize an array of TS
        for (uint256 i = 0; i < _vote._ts.length; i++) {
            temp = concat2(
                _vote._ts[i].timestamp.encodeUint(),
                _vote._ts[i].signature.encodeBytes()
            );
            length = numOfBytes(temp);
            listSize = addLength(length, false);
            temp = concat2(listSize, temp);
            // temp = concat2(listSize, temp).encodeBytes();
            _rlp = concat2(_rlp, temp);
        }
        length = numOfBytes(_rlp);
        listSize = addLength(length, false);
        _rlp = concat2(listSize, _rlp);
        // _rlp = concat2(listSize, _rlp).encodeBytes();

        //  Next, serialize the blockPartSetID
        temp = concat2(
            _vote.blockPartSetID.n.encodeUint(),
            _vote.blockPartSetID.b.encodeBytes()
        );
        length = numOfBytes(temp);
        listSize = addLength(length, false);
        temp = concat2(listSize, temp);
        // temp = concat2(listSize, temp).encodeBytes();

        //  Combine all of them
        _rlp = concat3(_vote.round.encodeUint(), temp, _rlp);
        //  Calculate the LIST_HEAD_START and attach
        length = numOfBytes(_rlp);
        listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeBlockWitness(Types.BlockWitness memory _bw)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp;
        bytes memory temp;
        for (uint256 i = 0; i < _bw.witnesses.length; i++) {
            temp = _bw.witnesses[i].encodeBytes();
            _rlp = concat2(_rlp, temp);
        }
        //  Attach the LIST_HEAD_START
        uint256 length = numOfBytes(_rlp);
        temp = addLength(length, false);
        _rlp = concat2(temp, _rlp);
        // _rlp = concat2(temp, _rlp).encodeBytes();

        //  Combine height and witnesses
        _rlp = concat2(_bw.height.encodeUint(), _rlp);

        //  Attach the LIST_HEAD_START
        length = numOfBytes(_rlp);
        temp = addLength(length, false);
        return concat2(temp, _rlp);
    }

    function encodeEventProof(Types.EventProof memory _ep)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp;
        bytes memory temp;
        for (uint256 i = 0; i < _ep.eventMptNode.length; i++) {
            temp = _ep.eventMptNode[i].encodeBytes();
            _rlp = concat2(_rlp, temp);
        }
        //  Attach the LIST_HEAD_START
        uint256 length = numOfBytes(_rlp);
        temp = addLength(length, false);
        // _rlp = concat2(temp, _rlp);
        _rlp = concat2(temp, _rlp).encodeBytes();

        //  Combine index and eventMptNode
        _rlp = concat2(_ep.index.encodeUint(), _rlp);

        //  Attach the LIST_HEAD_START
        length = numOfBytes(_rlp);
        temp = addLength(length, false);
        return concat2(temp, _rlp);
    }

    function encodeBlockUpdate(Types.BlockUpdate memory _bu)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory temp;
        bytes memory _rlp;
        uint256 length;
        //  In the case that _validators[] is an empty array, loop will be skipped
        //  and RLP_ENCODE([bytes]) == EMPTY_LIST_HEAD_START (0xF800) instead
        if (_bu._validators.length != 0) {
            for (uint256 i = 0; i < _bu._validators.length; i++) {
                temp = _bu._validators[i].encodeBytes();
                _rlp = concat2(_rlp, temp);
            }
            length = numOfBytes(_rlp);
            temp = addLength(length, false);
            _rlp = concat2(temp, _rlp).encodeBytes();
        } else {
            _rlp = emptyListHeadStart();
        }

        _rlp = concat3(
            _bu._bh.encodeBlockHeader().encodeBytes(),
            _bu._votes.encodeVotes().encodeBytes(),
            _rlp
        );
        length = numOfBytes(_rlp);
        temp = addLength(length, false);
        return concat2(temp, _rlp);
    }

    function encodeReceiptProof(Types.ReceiptProof memory _rp)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory temp;
        bytes memory _rlp;
        //  Serialize [bytes] which are transaction receipts
        for (uint256 i = 0; i < _rp.txReceipts.length; i++) {
            temp = _rp.txReceipts[i].encodeBytes();
            _rlp = concat2(_rlp, temp);
        }
        uint256 length = numOfBytes(_rlp);
        temp = addLength(length, false);
        _rlp = concat2(temp, _rlp).encodeBytes();

        bytes memory eventProof;
        for (uint256 i = 0; i < _rp._ep.length; i++) {
            temp = _rp._ep[i].encodeEventProof();
            eventProof = concat2(eventProof, temp);
        }
        length = numOfBytes(eventProof);
        temp = addLength(length, false); // LIST_HEAD_START of [EVENT_PROOF]
        eventProof = concat2(temp, eventProof);

        _rlp = concat3(_rp.index.encodeUint(), _rlp, eventProof);
        length = numOfBytes(_rlp);
        temp = addLength(length, false);
        return concat2(temp, _rlp);
    }

    function encodeBlockProof(Types.BlockProof memory _bp)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat2(
                _bp._bh.encodeBlockHeader().encodeBytes(),
                _bp._bw.encodeBlockWitness().encodeBytes()
            );
        uint256 length = numOfBytes(_rlp);
        bytes memory listSize = addLength(length, false);
        return concat2(listSize, _rlp);
    }

    function encodeRelayMessage(Types.RelayMessage memory _rm)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory temp;
        bytes memory _rlp;
        uint256 length;
        if (_rm._buArray.length != 0) {
            for (uint256 i = 0; i < _rm._buArray.length; i++) {
                temp = _rm._buArray[i].encodeBlockUpdate().encodeBytes();
                _rlp = concat2(_rlp, temp);
            }
            length = numOfBytes(_rlp);
            temp = addLength(length, false);
            _rlp = concat2(temp, _rlp);
        } else {
            _rlp = emptyListShortStart();
        }

        if (_rm.isBPEmpty == false) {
            temp = _rm._bp.encodeBlockProof();
        } else {
            temp = emptyListHeadStart();
        }
        _rlp = concat2(_rlp, temp);

        bytes memory receiptProof;
        if (_rm.isRPEmpty == false) {
            for (uint256 i = 0; i < _rm._rp.length; i++) {
                temp = _rm._rp[i].encodeReceiptProof().encodeBytes();
                receiptProof = concat2(receiptProof, temp);
            }
            length = numOfBytes(receiptProof);
            temp = addLength(length, false);
            receiptProof = concat2(temp, receiptProof);
        } else {
            receiptProof = emptyListShortStart();
        }
        _rlp = concat2(_rlp, receiptProof);

        length = numOfBytes(_rlp);
        temp = addLength(length, false);
        return concat2(temp, _rlp);
    }

    //  Adding LIST_HEAD_START by length
    //  There are two cases:
    //  1. List contains less than or equal 55 elements (total payload of the RLP) -> LIST_HEAD_START = LIST_SHORT_START + [0-55] = [0xC0 - 0xF7]
    //  2. List contains more than 55 elements:
    //  - Total Payload = 512 elements = 0x0200
    //  - Length of Total Payload = 2
    //  => LIST_HEAD_START = \x (LIST_LONG_START + length of Total Payload) \x (Total Payload) = \x(F7 + 2) \x(0200) = \xF9 \x0200 = 0xF90200
    function addLength(uint256 length, bool isLongList)
        internal
        pure
        returns (bytes memory)
    {
        if (length > 55 && !isLongList) {
            bytes memory payLoadSize = encodeUintByLength(length);
            uint256 lengthSize = numOfBytes(payLoadSize);
            bytes memory listHeadStart = addLength(lengthSize, true);
            return concat2(listHeadStart, payLoadSize);
        } else if (length <= 55 && !isLongList) {
            return abi.encodePacked(uint8(LIST_SHORT_START + length));
        }
        return abi.encodePacked(uint8(LIST_LONG_START + length));
    }

    function emptyListHeadStart() internal pure returns (bytes memory) {
        bytes memory payLoadSize = encodeUintByLength(0);
        uint256 lengthSize = numOfBytes(payLoadSize);
        bytes memory listHeadStart =
            abi.encodePacked(uint8(LIST_LONG_START + lengthSize));
        return concat2(listHeadStart, payLoadSize);
    }

    function emptyListShortStart() internal pure returns (bytes memory) {
        return abi.encodePacked(LIST_SHORT_START);
    }

    //  return length in bytes format
    //  i.e. 81 = 0x51, 512 = 0x0200
    function encodeUintByLength(uint256 length)
        internal
        pure
        returns (bytes memory)
    {
        if (length <= 255) {
            //  return 0x00 - 0xFF
            return abi.encodePacked(uint8(length));
        } else if (length > 255 && length <= 65535) {
            //  return 0x0100 - 0xFFFF
            return abi.encodePacked(uint16(length));
        } else if (length > 65535 && length <= 16777215) {
            //  return 0x010000 - 0xFFFFFF
            return abi.encodePacked(uint24(length));
        } else if (length > 16777215 && length <= 4294967295) {
            //  return 0x01000000 - 0xFFFFFFFF
            return abi.encodePacked(uint32(length));
        } else if (length > 4294967295 && length <= 1099511627775) {
            //  return 0x0100000000 - 0xFFFFFFFFFF
            return abi.encodePacked(uint40(length));
        } else if (length > 1099511627775 && length <= 281474976710655) {
            //  return 0x010000000000 - 0xFFFFFFFFFFFF
            return abi.encodePacked(uint48(length));
        } else if (length > 281474976710655 && length <= 72057594037927935) {
            //  return 0x01000000000000 - 0xFFFFFFFFFFFFFF
            return abi.encodePacked(uint56(length));
        }
        return abi.encodePacked(uint64(length));
    }

    //  Find a number of bytes in the bytes format
    //  i.e. numOfBytes(0x51) = 1, numOfBytes(0x0200) = 2, numOfBytes(0x01234567) = 4
    function numOfBytes(bytes memory data) internal pure returns (uint256) {
        return bytes(string(data)).length;
    }

    //  Concaternate two encoding RLP
    function concat2(bytes memory _rlp1, bytes memory _rlp2)
        internal
        pure
        returns (bytes memory)
    {
        return abi.encodePacked(_rlp1, _rlp2);
    }

    //  Concaternate three encoding RLP
    function concat3(
        bytes memory _rlp1,
        bytes memory _rlp2,
        bytes memory _rlp3
    ) internal pure returns (bytes memory) {
        return abi.encodePacked(_rlp1, _rlp2, _rlp3);
    }

    //  Concaternate four encoding RLP
    function concat4(
        bytes memory _rlp1,
        bytes memory _rlp2,
        bytes memory _rlp3,
        bytes memory _rlp4
    ) internal pure returns (bytes memory) {
        return abi.encodePacked(_rlp1, _rlp2, _rlp3, _rlp4);
    }

    function concat5(
        bytes memory _rlp1,
        bytes memory _rlp2,
        bytes memory _rlp3,
        bytes memory _rlp4,
        bytes memory _rlp5
    ) internal pure returns (bytes memory) {
        return abi.encodePacked(_rlp1, _rlp2, _rlp3, _rlp4, _rlp5);
    }
}
