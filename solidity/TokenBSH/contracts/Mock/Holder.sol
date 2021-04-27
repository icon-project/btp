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

import "../TokenBSH.sol";

contract Holder {
    TokenBSH private bsh;

    function addBSHContract(address _bsh) external {
        bsh = TokenBSH(_bsh);
    }

    function setApprove(address _operator, uint256 amount) external {
        bsh.approve(_operator, amount);
    }

    function callTransfer(
        string calldata _tokenName,
        uint256 _value,
        string calldata _to
    ) external {
        bsh.transfer(_tokenName, _value, _to);
    }
}
