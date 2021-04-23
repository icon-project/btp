// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.5.0 <=0.8.5;
pragma experimental ABIEncoderV2;

import "./RLPEncodeLib.sol";
import "./TypesLib.sol";

library RLPEncodeStruct {
    using RLPEncode for bytes;
    using RLPEncode for string;
    using RLPEncode for uint256;
    using RLPEncode for address;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

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

    function encodeData(Types.TransferToken memory _data)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory _rlp =
            concat4(
                _data.from.encodeString(),
                _data.to.encodeString(),
                _data.tokenName.encodeString(),
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
