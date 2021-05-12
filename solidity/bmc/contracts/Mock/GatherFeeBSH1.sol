// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Interfaces/NativeCoinBSH.sol";

contract GatherFeeBSH is NativeCoinBSH {
    constructor(
        address _bmc,
        string memory _serviceName,
        string memory _nativeCoinName,
        string memory _symbol,
        uint256 _decimals,
        uint256 _fee
    ) NativeCoinBSH(_bmc, _serviceName, _nativeCoinName, _symbol, _decimals, _fee) {}

    function getFees(uint _sn) external view returns (Types.Asset[] memory) {
        return pendingFA[_sn];
    }

    function getFAOf(string calldata _coin) external view returns (uint) {
        return aggregationFee[_coin];
    }
}