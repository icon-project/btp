// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../libraries/RLPEncode.sol";
import "../libraries/RLPDecode.sol";

contract TestLibRLP {
    using RLPEncode for *;
    using RLPDecode for *;

    function encodeBytes(bytes memory self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeBytes();
    }

    function encodeString(string memory self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeString();
    }

    function encodeAddress(address self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeAddress();
    }

    function encodeUint(uint256 self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeUint();
    }

    function encodeInt(int256 self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeInt();
    }

    function encodeBool(bool self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeBool();
    }

    function encodeList(bytes[] memory self)
        external
        pure
        returns (bytes memory)
    {
        return self.encodeList();
    }

    function decodeBytes(bytes memory rlpBytes)
        external
        pure
        returns (bytes memory)
    {
        return rlpBytes.toRlpItem().toBytes();
    }

    function decodeString(bytes memory rlpBytes)
        external
        pure
        returns (string memory)
    {
        return string(rlpBytes.toRlpItem().toBytes());
    }

    function decodeAddress(bytes memory rlpBytes)
        external
        pure
        returns (address)
    {
        return rlpBytes.toRlpItem().toAddress();
    }

    function decodeUint(bytes memory rlpBytes)
        external
        pure
        returns (uint256)
    {
        return rlpBytes.toRlpItem().toUint();
    }

    function decodeInt(bytes memory rlpBytes)
        external
        pure
        returns (int256)
    {
        return rlpBytes.toRlpItem().toInt();
    }

    function decodeBool(bytes memory rlpBytes)
        external
        pure
        returns (bool)
    {
        return rlpBytes.toRlpItem().toBoolean();
    }

    function decodeList(bytes memory rlpBytes)
        external
        pure
        returns (bytes[] memory)
    {
        RLPDecode.RLPItem[] memory rlpItems = rlpBytes.toRlpItem().toList();
        bytes[] memory rlpBytesList = new bytes[](rlpItems.length);
        for (uint256 i = 0; i < rlpItems.length; i++)
            rlpBytesList[i] = rlpItems[i].toRlpBytes();
        return rlpBytesList;
    }
}
