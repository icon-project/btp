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

import "../../../icondao/Libraries/Precompiles.sol";

contract PrecompilesMock {
    using Precompiles for Precompiles.RecoverPubKey;
    using Precompiles for bytes;

    function sha3fips(bytes memory data) public view returns (bytes32) {
        return Precompiles.sha3fips(data);
    }

    function ecrecoverPublicKey(
        bytes32 _hash,
        uint8 _v,
        bytes32 _r,
        bytes32 _s
    ) public view returns (bytes memory) {
        return
            Precompiles.RecoverPubKey(_hash, _v, _r, _s).ecrecoverPublicKey();
    }
}
