// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
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

    function decodeBMCService(bytes memory _rlp)
        internal
        pure
        returns (Types.BMCService memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.BMCService(
                string(ls[0].toBytes()),
                ls[1].toBytes() //  bytes array of RLPEncode(Data)
            );
    }

    function decodePropagateMessage(bytes memory _rlp)
        internal
        pure
        returns (string memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return string(ls[0].toBytes());
    }

    function decodeInitMessage(bytes memory _rlp)
        internal
        pure
        returns (string[] memory _links)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        RLPDecode.RLPItem[] memory rlpLinks = ls[0].toList();
        _links = new string[](rlpLinks.length);
        for (uint256 i = 0; i < rlpLinks.length; i++)
            _links[i] = string(rlpLinks[i].toBytes());
    }

    function decodeBMCMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.BMCMessage memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.BMCMessage(
                string(ls[0].toBytes()),
                string(ls[1].toBytes()),
                string(ls[2].toBytes()),
                ls[3].toInt(),
                ls[4].toBytes() //  bytes array of RLPEncode(ServiceMessage)
            );
    }

    function decodeResponse(bytes memory _rlp)
        internal
        pure
        returns (Types.Response memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return Types.Response(ls[0].toUint(), string(ls[1].toBytes()));
    }

}
