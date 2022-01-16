// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ERC20Tradable is ERC20, Ownable {
    constructor(
        string memory _name,
        string memory _symbol,
        uint8 _decimals
    ) ERC20(_name, _symbol) {
        ERC20._setupDecimals(_decimals);
    }

    function burn(address account, uint256 amount) public onlyOwner {
        _burn(account, amount);
    }

    function mint(address account, uint256 amount) public onlyOwner {
        _mint(account, amount);
    }
}
