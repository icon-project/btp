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

    function decodeRelayMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.RelayMessage memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        Types.ReceiptProof[] memory _rpArray;
        if (ls[0].toBytes().length != 0) {
            _rpArray = new Types.ReceiptProof[](ls[0].toList().length);
            for (uint256 i = 0; i < ls[0].toList().length; i++) {
                _rpArray[i] = ls[0].toList()[i].toBytes().decodeReceiptProof();
            }
        }
        return Types.RelayMessage(_rpArray);
    }
//
//    function decodeReceiptProofs(bytes memory _rlp)
//        internal
//        pure
//        returns (Types.ReceiptProof[] memory _rp)
//    {
//        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
//        if (ls[0].toBytes().length != 0) {
//            _rp = new Types.ReceiptProof[](ls[0].toList().length);
//            for (uint256 i = 0; i < ls[0].toList().length; i++) {
//                _rp[i] = ls[0].toList()[i].toBytes().decodeReceiptProof();
//            }
//        }
//    }

    function decodeReceiptProof(bytes memory _rlp)
        internal
        pure
        returns (Types.ReceiptProof memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();

        Types.MessageEvent[] memory events = new Types.MessageEvent[](
            ls[1].toBytes().toRlpItem().toList().length
        );

        for (
            uint256 i = 0;
            i < ls[1].toBytes().toRlpItem().toList().length;
            i++
        ) {
            events[i] = ls[1]
            .toBytes()
            .toRlpItem()
            .toList()[i].toRlpBytes().toMessageEvent();
        }

        return Types.ReceiptProof(ls[0].toUint(), events, ls[2].toUint());
    }

    function toMessageEvent(bytes memory _rlp)
        internal
        pure
        returns (Types.MessageEvent memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.MessageEvent(
                string(ls[0].toBytes()),
                ls[1].toUint(),
                ls[2].toBytes()
            );
    }

    function decodeExtra(bytes memory _rlp)
        internal
        pure
    returns (address, string memory)
    {
        RLPDecode.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return (ls[0].toAddress(), string(ls[1].toBytes()));
    }
}
