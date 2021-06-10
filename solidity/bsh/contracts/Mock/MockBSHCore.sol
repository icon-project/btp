// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../BSHCore.sol";

contract MockBSHCore is BSHCore {
    function mintMock(address _acc, uint256 _id, uint256 _value) external {
        _mint(_acc, _id, _value, "");
    }

    function burnMock(address _acc, uint256 _id, uint256 _value) external {
        _burn(_acc, _id, _value);
    }

    function setAggregationFee(string calldata _coinName, uint256 _value) external {
        aggregationFee[_coinName] += _value;
    }

    function clearAggregationFee() external {
        for (uint i = 0; i < coinsName.length; i++) {
            delete aggregationFee[coinsName[i]];
        }
    }

    function setRefundableBalance(address _acc, string calldata _coinName, uint256 _value) external {
        balances[_acc][_coinName].refundableBalance += _value;
    }
}