// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../BSHPeriphery.sol";
import "../BSHCore.sol";

contract MockBSHPeriphery is BSHPeriphery {
    using Strings for string;

    function getFees(uint256 _sn) external view returns (Types.Asset[] memory) {
        return pendingFA[_sn];
    }

    function getAggregationFeeOf(string calldata _coinName)
        external
        view
        returns (uint256 _fee)
    {
        Types.Asset[] memory _fees = bshCore.getAccumulatedFees();
        for (uint256 i = 0; i < _fees.length; i++) {
            if (_coinName.compareTo(_fees[i].coinName)) return _fees[i].value;
        }
    }
}
