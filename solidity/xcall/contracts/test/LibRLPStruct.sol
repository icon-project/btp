// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/RLPEncodeStruct.sol";
import "../libraries/RLPDecodeStruct.sol";

contract LibRLPStruct {
    using RLPEncodeStruct for *;
    using RLPDecodeStruct for *;

    function encodeCSMessage(Types.CSMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeCSMessage();
    }

    function encodeCSMessageRequest(Types.CSMessageRequest memory self)
    external pure returns (bytes memory) {
        return self.encodeCSMessageRequest();
    }

    function encodeCSMessageResponse(Types.CSMessageResponse memory self)
    external pure returns (bytes memory) {
        return self.encodeCSMessageResponse();
    }

    function decodeCSMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.CSMessage memory)
    {
        return rlpBytes.decodeCSMessage();
    }

    function decodeCSMessageRequest(bytes memory rlpBytes)
    external
    pure
    returns (Types.CSMessageRequest memory)
    {
        return rlpBytes.decodeCSMessageRequest();
    }

    function decodeCSMessageResponse(bytes memory rlpBytes)
    external
    pure
    returns (Types.CSMessageResponse memory)
    {
        return rlpBytes.decodeCSMessageResponse();
    }

}
