// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;

/**
 * @title RLPEncode
 * @dev A simple RLP encoding library.
 * @author Bakaoh
 * The original code was modified. For more info, please check the link:
 * https://github.com/bakaoh/solidity-rlp-encode.git
 */
library RLPEncode {
    uint8 public constant LIST_SHORT_START = 0xc0;
    uint8 public constant LIST_LONG_START = 0xf7;

    uint8 internal constant MAX_UINT8 = type(uint8).max;
    uint16 internal constant MAX_UINT16 = type(uint16).max;
    uint24 internal constant MAX_UINT24 = type(uint24).max;
    uint32 internal constant MAX_UINT32 = type(uint32).max;
    uint40 internal constant MAX_UINT40 = type(uint40).max;
    uint48 internal constant MAX_UINT48 = type(uint48).max;
    uint56 internal constant MAX_UINT56 = type(uint56).max;
    uint64 internal constant MAX_UINT64 = type(uint64).max;
    uint72 internal constant MAX_UINT72 = type(uint72).max;
    uint80 internal constant MAX_UINT80 = type(uint80).max;
    uint88 internal constant MAX_UINT88 = type(uint88).max;
    uint96 internal constant MAX_UINT96 = type(uint96).max;
    uint104 internal constant MAX_UINT104 = type(uint104).max;
    uint112 internal constant MAX_UINT112 = type(uint112).max;
    uint120 internal constant MAX_UINT120 = type(uint120).max;
    uint128 internal constant MAX_UINT128 = type(uint128).max;

    /*
     * Internal functions
     */

    /**
     * @dev RLP encodes a byte string.
     * @param self The byte string to encode.
     * @return The RLP encoded string in bytes.
     */
    function encodeBytes(bytes memory self)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory encoded;
        if (self.length == 1 && uint8(self[0]) <= 128) {
            encoded = self;
        } else {
            encoded = concat(encodeLength(self.length, 128), self);
        }
        return encoded;
    }

    /**
     * @dev RLP encodes a list of RLP encoded byte byte strings.
     * @param self The list of RLP encoded byte strings.
     * @return The RLP encoded list of items in bytes.
     */
    function encodeList(bytes[] memory self)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory list = flatten(self);
        return concat(encodeLength(list.length, 192), list);
    }

    /**
     * @dev RLP encodes a uint.
     * @param self The uint to encode.
     * @return The RLP encoded uint in bytes.
     */
    function encodeUint(uint256 self) internal pure returns (bytes memory) {
        uint nBytes = bitLength(self)/8 + 1;
        bytes memory uintBytes = encodeUintByLength(self);
        if (nBytes - uintBytes.length > 0) {
            uintBytes = abi.encodePacked(bytes1(0), uintBytes);
        }
        return encodeBytes(uintBytes);
    }

    /**
     * @dev RLP encodes an int.
     * @param self The int to encode.
     * @return The RLP encoded int in bytes.
     */
    function encodeInt(int256 self) internal pure returns (bytes memory) {
        return encodeUint(uint256(self));
    }

    /**
     * @dev RLP encodes a bool.
     * @param self The bool to encode.
     * @return The RLP encoded bool in bytes.
     */
    function encodeBool(bool self) internal pure returns (bytes memory) {
        bytes memory encoded = new bytes(1);
        encoded[0] = (self ? bytes1(0x01) : bytes1(0x00));
        return encoded;
    }

    /*
     * Private functions
     */

    /**
     * @dev Encode the first byte, followed by the `len` in binary form if `length` is more than 55.
     * @param len The length of the string or the payload.
     * @param offset 128 if item is string, 192 if item is list.
     * @return RLP encoded bytes.
     */
    function encodeLength(uint256 len, uint256 offset)
        private
        pure
        returns (bytes memory)
    {
        bytes memory encoded;
        if (len < 56) {
            encoded = new bytes(1);
            encoded[0] = bytes32(len + offset)[31];
        } else {
            uint256 lenLen;
            uint256 i = 1;
            while (len / i != 0) {
                lenLen++;
                i *= 256;
            }

            encoded = new bytes(lenLen + 1);
            encoded[0] = bytes32(lenLen + offset + 55)[31];
            for (i = 1; i <= lenLen; i++) {
                encoded[i] = bytes32((len / (256**(lenLen - i))) % 256)[31];
            }
        }
        return encoded;
    }

    /**
     * @dev Encode integer in big endian binary form with no leading zeroes.
     * @notice TODO: This should be optimized with assembly to save gas costs.
     * @param _x The integer to encode.
     * @return RLP encoded bytes.
     */
    function toBinary(uint256 _x) private pure returns (bytes memory) {
        //  Modify library to make it work properly when _x = 0
        if (_x == 0) {
            return abi.encodePacked(uint8(_x));
        }
        bytes memory b = new bytes(32);
        assembly {
            mstore(add(b, 32), _x)
        }
        uint256 i;
        for (i = 0; i < 32; i++) {
            if (b[i] != 0) {
                break;
            }
        }
        bytes memory res = new bytes(32 - i);
        for (uint256 j = 0; j < res.length; j++) {
            res[j] = b[i++];
        }
        return res;
    }

    /**
     * @dev Copies a piece of memory to another location.
     * @notice From: https://github.com/Arachnid/solidity-stringutils/blob/master/src/strings.sol.
     * @param _dest Destination location.
     * @param _src Source location.
     * @param _len Length of memory to copy.
     */
    function memcpy(
        uint256 _dest,
        uint256 _src,
        uint256 _len
    ) private pure {
        uint256 dest = _dest;
        uint256 src = _src;
        uint256 len = _len;

        for (; len >= 32; len -= 32) {
            assembly {
                mstore(dest, mload(src))
            }
            dest += 32;
            src += 32;
        }

        uint256 mask = 256**(32 - len) - 1;
        assembly {
            let srcpart := and(mload(src), not(mask))
            let destpart := and(mload(dest), mask)
            mstore(dest, or(destpart, srcpart))
        }
    }

    /**
     * @dev Flattens a list of byte strings into one byte string.
     * @notice From: https://github.com/sammayo/solidity-rlp-encoder/blob/master/RLPEncode.sol.
     * @param _list List of byte strings to flatten.
     * @return The flattened byte string.
     */
    function flatten(bytes[] memory _list) private pure returns (bytes memory) {
        if (_list.length == 0) {
            return new bytes(0);
        }

        uint256 len;
        uint256 i;
        for (i = 0; i < _list.length; i++) {
            len += _list[i].length;
        }

        bytes memory flattened = new bytes(len);
        uint256 flattenedPtr;
        assembly {
            flattenedPtr := add(flattened, 0x20)
        }

        for (i = 0; i < _list.length; i++) {
            bytes memory item = _list[i];

            uint256 listPtr;
            assembly {
                listPtr := add(item, 0x20)
            }

            memcpy(flattenedPtr, listPtr, item.length);
            flattenedPtr += _list[i].length;
        }

        return flattened;
    }

    /**
     * @dev Concatenates two bytes.
     * @notice From: https://github.com/GNSPS/solidity-bytes-utils/blob/master/contracts/BytesLib.sol.
     * @param _preBytes First byte string.
     * @param _postBytes Second byte string.
     * @return Both byte string combined.
     */
    function concat(bytes memory _preBytes, bytes memory _postBytes)
        private
        pure
        returns (bytes memory)
    {
        bytes memory tempBytes;

        assembly {
            tempBytes := mload(0x40)

            let length := mload(_preBytes)
            mstore(tempBytes, length)

            let mc := add(tempBytes, 0x20)
            let end := add(mc, length)

            for {
                let cc := add(_preBytes, 0x20)
            } lt(mc, end) {
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
                mstore(mc, mload(cc))
            }

            length := mload(_postBytes)
            mstore(tempBytes, add(length, mload(tempBytes)))

            mc := end
            end := add(mc, length)

            for {
                let cc := add(_postBytes, 0x20)
            } lt(mc, end) {
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
                mstore(mc, mload(cc))
            }

            mstore(
                0x40,
                and(
                    add(add(end, iszero(add(length, mload(_preBytes)))), 31),
                    not(31)
                )
            )
        }

        return tempBytes;
    }

    function addLength(uint256 length, bool isLongList)
        internal
        pure
        returns (bytes memory)
    {
        if (length > 55 && !isLongList) {
            bytes memory payLoadSize = encodeUintByLength(length);
            uint256 lengthSize = payLoadSize.length;
            bytes memory listHeadStart = addLength(lengthSize, true);
            return abi.encodePacked(listHeadStart, payLoadSize);
        } else if (length <= 55 && !isLongList) {
            return abi.encodePacked(uint8(LIST_SHORT_START + length));
        }
        return abi.encodePacked(uint8(LIST_LONG_START + length));
    }

    /**
     * @dev convert uint to strict bytes.
     * @notice only handle to uint128 due to contract code size limit
     * @param length The uint to convert.
     * @return The uint in strict bytes without padding.
     */
    function encodeUintByLength(uint256 length)
        internal
        pure
        returns (bytes memory)
    {
        if (length < MAX_UINT8) {
            return abi.encodePacked(uint8(length));
        } else if (length >= MAX_UINT8 && length < MAX_UINT16) {
            return abi.encodePacked(uint16(length));
        } else if (length >= MAX_UINT16 && length < MAX_UINT24) {
            return abi.encodePacked(uint24(length));
        } else if (length >= MAX_UINT24 && length < MAX_UINT32) {
            return abi.encodePacked(uint32(length));
        } else if (length >= MAX_UINT32 && length < MAX_UINT40) {
            return abi.encodePacked(uint40(length));
        } else if (length >= MAX_UINT40 && length < MAX_UINT48) {
            return abi.encodePacked(uint48(length));
        } else if (length >= MAX_UINT48 && length < MAX_UINT56) {
            return abi.encodePacked(uint56(length));
        } else if (length >= MAX_UINT56 && length < MAX_UINT64) {
            return abi.encodePacked(uint64(length));
        } else if (length >= MAX_UINT64 && length < MAX_UINT72) {
            return abi.encodePacked(uint72(length));
        } else if (length >= MAX_UINT72 && length < MAX_UINT80) {
            return abi.encodePacked(uint80(length));
        } else if (length >= MAX_UINT80 && length < MAX_UINT88) {
            return abi.encodePacked(uint88(length));
        } else if (length >= MAX_UINT88 && length < MAX_UINT96) {
            return abi.encodePacked(uint96(length));
        } else if (length >= MAX_UINT96 && length < MAX_UINT104) {
            return abi.encodePacked(uint104(length));
        } else if (length >= MAX_UINT104 && length < MAX_UINT112) {
            return abi.encodePacked(uint112(length));
        } else if (length >= MAX_UINT112 && length < MAX_UINT120) {
            return abi.encodePacked(uint120(length));
        }
        require(length >= MAX_UINT120 && length < MAX_UINT128, "outOfBounds: [0, 2^128]");
        return abi.encodePacked(uint128(length));
    }

    function bitLength(uint256 n) internal pure returns (uint256) {
        uint256 count;
        while (n != 0) {
            count += 1;
            n >>= 1;
        }
        return count;
    }

    function bitLength(uint256 n) internal pure returns (uint256) {
        uint256 count;
        while (n != 0) {
            count += 1;
            n >>= 1;
        }
        return count;
    }
}
