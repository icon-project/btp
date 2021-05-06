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
