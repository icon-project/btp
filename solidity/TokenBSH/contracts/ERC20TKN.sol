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

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract ERC20TKN is ERC20 {
    // modify token name
    string public constant NAME = "ERC20TKN";
    // modify token symbol
    string public constant SYMBOL = "ETH";
    // modify token decimals
    uint8 public constant DECIMALS = 18;
    // modify initial token supply
    uint256 public constant INITIAL_SUPPLY = 10000 * (10**uint256(DECIMALS)); // 10000 tokens

    /**
     * @dev Constructor that gives msg.sender all of existing tokens.
     */
    constructor() public ERC20(NAME, SYMBOL) {
        _mint(msg.sender, INITIAL_SUPPLY);
    }
}
