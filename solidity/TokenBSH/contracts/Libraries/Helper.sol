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

/*
 *  Change supporting solidity compiler version
 *  The original code can be found via this link: https://github.com/hamdiallam/Solidity-RLP.git
 */
import "./RLPReaderLib.sol";

contract Helper {
    using RLPReader for bytes;
    using RLPReader for uint256;
    using RLPReader for RLPReader.RLPItem;
    using RLPReader for RLPReader.Iterator;

    function isList(bytes memory item) public pure returns (bool) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.isList();
    }

    function itemLength(bytes memory item) public pure returns (uint256) {
        uint256 memPtr;
        assembly {
            memPtr := add(item, 0x20)
        }

        return _itemLength(memPtr);
    }

    function rlpLen(bytes memory item) public pure returns (uint256) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.rlpLen();
    }

    function payloadLen(bytes memory item) public pure returns (uint256) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.payloadLen();
    }

    function numItems(bytes memory item) public pure returns (uint256) {
        RLPReader.RLPItem[] memory rlpItem = item.toRlpItem().toList();
        return rlpItem.length;
    }

    function toRlpBytes(bytes memory item) public pure returns (bytes memory) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toRlpBytes();
    }

    function toBytes(bytes memory item) public pure returns (bytes memory) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toBytes();
    }

    function toUint(bytes memory item) public pure returns (uint256) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toUint();
    }

    function toUintStrict(bytes memory item) public pure returns (uint256) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toUintStrict();
    }

    function toAddress(bytes memory item) public pure returns (address) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toAddress();
    }

    function toBoolean(bytes memory item) public pure returns (bool) {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return rlpItem.toBoolean();
    }

    function bytesToString(bytes memory item)
        public
        pure
        returns (string memory)
    {
        RLPReader.RLPItem memory rlpItem = item.toRlpItem();
        return string(rlpItem.toBytes());
    }

    function toIterator(bytes memory item) public pure {
        // we just care that this does not revert
        item.toRlpItem().iterator();
    }

    // expects [["somestring"]]
    function nestedIteration(bytes memory item)
        public
        pure
        returns (string memory)
    {
        RLPReader.Iterator memory iter = item.toRlpItem().iterator();
        RLPReader.Iterator memory subIter = iter.next().iterator();
        string memory result = string(subIter.next().toBytes());

        require(!iter.hasNext(), "Must end here");
        require(!subIter.hasNext(), "Must end here");

        return result;
    }

    /* Copied verbatim from the reader contract due to scope */
    uint8 private constant STRING_SHORT_START = 0x80;
    uint8 private constant STRING_LONG_START = 0xb8;
    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf8;

    function _itemLength(uint256 memPtr) private pure returns (uint256) {
        uint256 itemLen;
        uint256 byte0;
        assembly {
            byte0 := byte(0, mload(memPtr))
        }

        if (byte0 < STRING_SHORT_START) itemLen = 1;
        else if (byte0 < STRING_LONG_START)
            itemLen = byte0 - STRING_SHORT_START + 1;
        else if (byte0 < LIST_SHORT_START) {
            assembly {
                let byteLen := sub(byte0, 0xb7) // # of bytes the actual length is
                memPtr := add(memPtr, 1) // skip over the first byte

                /* 32 byte word size */
                let dataLen := div(mload(memPtr), exp(256, sub(32, byteLen))) // right shifting to get the len
                itemLen := add(dataLen, add(byteLen, 1))
            }
        } else if (byte0 < LIST_LONG_START) {
            itemLen = byte0 - LIST_SHORT_START + 1;
        } else {
            assembly {
                let byteLen := sub(byte0, 0xf7)
                memPtr := add(memPtr, 1)

                let dataLen := div(mload(memPtr), exp(256, sub(32, byteLen))) // right shifting to the correct length
                itemLen := add(dataLen, add(byteLen, 1))
            }
        }

        return itemLen;
    }
}
