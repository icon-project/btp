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

pragma solidity >=0.5.0 <=0.8.0;

import "./Bytes.sol";

library Hash {
    struct RecoverPubKey {
        bytes32 _hash;
        uint8 _v;
        bytes32 _r;
        bytes32 _s;
    }
    using Hash for RecoverPubKey;
    using Bytes for bytes;

    function sha3FIPS256(bytes memory data) internal returns (bytes32) {
        bytes32[1] memory h;
        assembly {
            if iszero(
                staticcall(not(0), 0x66, add(data, 32), mload(data), h, 32)
            ) {
                invalid()
            }
        }
        return h[0];
    }


    function ecRecoverPublicKey(bytes32 _hash, bytes memory _signature)
    internal
    returns (bytes memory)
    {
        bytes memory pubkey = new bytes(65);
        bytes memory _r = _signature.slice(0, 32);
        bytes memory _s = _signature.slice(32, 64);
        bytes memory _v = _signature.slice(64, 65);

        assembly {
            let input := mload(0x40)
            mstore(input, _hash)
            mstore(add(input, 32), _v)
            mstore(add(input, 64), _r)
            mstore(add(input, 96), _s)

            // NOTE: we can reuse the request memory
            if iszero(staticcall(not(0), 0x67, input, 128, input, 65)) {
                revert(0, 0)
            }
            mstore(add(pubkey, 0x20), mload(input))
            mstore(add(pubkey, 0x40), mload(add(input, 0x20)))
            mstore(add(pubkey, 0x60), mload(add(input, 0x40)))
        }

        return pubkey;

    }

    function ecrecoverPublicKey(RecoverPubKey memory pubkeyParams)
        internal
        returns (bytes memory)
    {
        bytes memory pubkey = new bytes(65);
        bytes32 _hash = pubkeyParams._hash;
        uint8 _v = pubkeyParams._v;
        bytes32 _r = pubkeyParams._r;
        bytes32 _s = pubkeyParams._s;

        assembly {
            let input := mload(0x40)
            mstore(input, _hash)
            mstore(add(input, 32), _v)
            mstore(add(input, 64), _r)
            mstore(add(input, 96), _s)

            // NOTE: we can reuse the request memory
            if iszero(staticcall(not(0), 0x67, input, 128, input, 65)) {
                revert(0, 0)
            }
            mstore(add(pubkey, 0x20), mload(input))
            mstore(add(pubkey, 0x40), mload(add(input, 0x20)))
            mstore(add(pubkey, 0x60), mload(add(input, 0x40)))
        }

        return pubkey;
    }
}
