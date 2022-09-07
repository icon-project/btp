// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../libraries/RLPEncodeStruct.sol";
import "../libraries/RLPDecodeStruct.sol";

contract LibRLPStruct {
    using RLPEncodeStruct for *;
    using RLPDecodeStruct for *;

    function encodeBTPMessage(Types.BTPMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeBTPMessage();
    }

    function encodeErrorMessage(Types.ErrorMessage memory self)
    external pure returns (bytes memory) {
        return self.encodeErrorMessage();
    }

    function encodeBMCService(Types.BMCService memory self)
    external pure returns (bytes memory) {
        return self.encodeBMCService();
    }

    function encodeInitMessage(string[] memory self)
    external pure returns (bytes memory) {
        return self.encodeInitMessage();
    }

    function encodePropagateMessage(string memory self)
    external pure returns (bytes memory) {
        return self.encodePropagateMessage();
    }

    function decodeBTPMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.BTPMessage memory)
    {
        return rlpBytes.decodeBTPMessage();
    }

    function decodeErrorMessage(bytes memory rlpBytes)
    external
    pure
    returns (Types.ErrorMessage memory)
    {
        return rlpBytes.decodeErrorMessage();
    }

    function decodeBMCService(bytes memory rlpBytes)
    external
    pure
    returns (Types.BMCService memory)
    {
        return rlpBytes.decodeBMCService();
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

}
