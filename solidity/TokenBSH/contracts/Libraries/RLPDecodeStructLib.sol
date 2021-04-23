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

import "./RLPReaderLib.sol";
import "./Helper.sol";
import "./TypesLib.sol";

library RLPDecodeStruct {
    using RLPReader for RLPReader.RLPItem;
    using RLPReader for RLPReader.Iterator;
    using RLPReader for bytes;

    using RLPDecodeStruct for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeServiceMessage(bytes memory _rlp)
        internal
        pure
        returns (Types.ServiceMessage memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.ServiceMessage(
                Types.ServiceType(ls[0].toUint()),
                ls[1].toBytes() //  bytes array of RLPEncode(Data)
            );
    }

    function decodeData(bytes memory _rlp)
        internal
        pure
        returns (Types.TransferToken memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return
            Types.TransferToken(
                string(ls[0].toBytes()),
                string(ls[1].toBytes()),
                string(ls[2].toBytes()),
                ls[3].toUint()
            );
    }

    function decodeResponse(bytes memory _rlp)
        internal
        pure
        returns (Types.Response memory)
    {
        RLPReader.RLPItem[] memory ls = _rlp.toRlpItem().toList();
        return Types.Response(ls[0].toUint(), string(ls[1].toBytes()));
    }
}
