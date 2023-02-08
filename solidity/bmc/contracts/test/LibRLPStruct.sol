// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/RLPEncodeStruct.sol";
import "../libraries/RLPDecodeStruct.sol";

contract LibRLPStruct {
    using RLPEncodeStruct for *;
    using RLPDecodeStruct for *;

    function encodeFeeInfo(Types.FeeInfo memory self)
    external pure returns (bytes memory) {
        return self.encodeFeeInfo();
    }

    function encodeBTPMessage(Types.BTPMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeBTPMessage();
    }

    function encodeResponseMessage(Types.ResponseMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeResponseMessage();
    }

    function encodeBMCMessage(Types.BMCMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeBMCMessage();
    }

    function encodeInitMessage(string[] memory self)
    external pure returns (bytes memory) {
        return self.encodeInitMessage();
    }

    function encodePropagateMessage(string memory self)
    external pure returns (bytes memory) {
        return self.encodePropagateMessage();
    }

    function encodeClaimMessage(Types.ClaimMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeClaimMessage();
    }

    function decodeFeeInfo(bytes memory rlpBytes)
    external
    pure
    returns (Types.FeeInfo memory)
    {
        return rlpBytes.decodeFeeInfo();
    }

    function decodeBTPMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.BTPMessage memory)
    {
        return rlpBytes.decodeBTPMessage();
    }

    function decodeResponseMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.ResponseMessage memory)
    {
        return rlpBytes.decodeResponseMessage();
    }

    function decodeBMCMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.BMCMessage memory)
    {
        return rlpBytes.decodeBMCMessage();
    }

    function decodeInitMessage(bytes memory rlpBytes)
    external
    pure
    returns (string[] memory)
    {
        return rlpBytes.decodeInitMessage();
    }

    function decodePropagateMessage(bytes memory rlpBytes)
    external
    pure
    returns (string memory)
    {
        return rlpBytes.decodePropagateMessage();
    }

    function decodeClaimMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.ClaimMessage memory)
    {
        return rlpBytes.decodeClaimMessage();
    }

}
