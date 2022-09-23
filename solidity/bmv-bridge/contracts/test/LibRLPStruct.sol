// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../libraries/RLPDecodeStruct.sol";

contract LibRLPStruct {
    using RLPDecodeStruct for *;

//    function decodeRelayMessage(bytes memory rlpBytes)
//    external
//    pure
//    returns (Types.RelayMessage memory)
//    {
//        return rlpBytes.decodeRelayMessage();
//    }
//
//    function decodeReceiptProof(bytes memory rlpBytes)
//    external
//    pure
//    returns (Types.ReceiptProof memory)
//    {
//        return rlpBytes.decodeReceiptProof();
//    }

    function decodeMessageEvent(bytes memory rlpBytes)
    external
    pure
    returns (Types.MessageEvent memory)
    {
        return rlpBytes.toMessageEvent();
    }

}
