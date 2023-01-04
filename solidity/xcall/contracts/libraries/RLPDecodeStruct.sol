// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "./RLPDecode.sol";
import "./Types.sol";

library RLPDecodeStruct {
    using RLPDecode for RLPDecode.RLPItem;
    using RLPDecode for RLPDecode.Iterator;
    using RLPDecode for bytes;

    using RLPDecodeStruct for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeCSMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.CSMessage memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.CSMessage(
                ls[0].toInt(),
                ls[1].toBytes() //  bytes array of RLPEncode(Data)
            );
    }

    function decodeCSMessageRequest(bytes memory _rlp)
        internal
        pure
    returns (Types.CSMessageRequest memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
        Types.CSMessageRequest(
            string(ls[0].toBytes()),
            string(ls[1].toBytes()),
            ls[2].toUint(),
            ls[3].toBoolean(),
            ls[4].toBytes()
        );
    }

    function decodeCSMessageResponse(bytes memory _rlp)
        internal
        pure
    returns (Types.CSMessageResponse memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
        Types.CSMessageResponse(
            ls[0].toUint(),
            int(ls[1].toInt()),
            string(ls[2].toBytes())
        );
    }

}
