// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../BSHCore.sol";

contract MockBSHCore is BSHCore {
    function mintMock(
        address _acc,
        uint256 _erc20Address,
        uint256 _value
    ) external {
        IERC20Tradable(_erc20Address).mint(_acc, _value);
    }

    function burnMock(
        address _acc,
        uint256 _erc20Address,
        uint256 _value
    ) external {
        IERC20Tradable(_erc20Address).mint(_acc, _value);
    }

    function setAggregationFee(string calldata _coinName, uint256 _value)
        external
    {
        aggregationFee[_coinName] += _value;
    }

    function clearAggregationFee() external {
        for (uint256 i = 0; i < coinsName.length; i++) {
            delete aggregationFee[coinsName[i]];
        }
    }

    function clearBSHPerifSetting() external {
        bshPeriphery = IBSHPeriphery(address(0));
    }

    function setRefundableBalance(
        address _acc,
        string calldata _coinName,
        uint256 _value
    ) external {
        balances[_acc][_coinName].refundableBalance += _value;
    }
}
