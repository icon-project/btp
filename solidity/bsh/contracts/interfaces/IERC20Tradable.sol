// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.5.8 <0.8.10;

import "@openzeppelin/contracts-upgradeable/token/ERC20/IERC20Upgradeable.sol";

interface IERC20Tradable is IERC20Upgradeable {
    function burn(address account, uint256 amount) external;

    function mint(address account, uint256 amount) external;
}
