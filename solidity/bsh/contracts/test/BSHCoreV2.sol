// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../BSHCore.sol";

contract BSHCoreV2 is BSHCore {
    using String for string;
    using SafeMathUpgradeable for uint256;

    mapping(address => mapping(string => uint256)) private stakes;

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function addStake(string calldata _coinName, uint256 _value)
        external
        payable
    {
        if (_coinName.compareTo(nativeCoinName)) {
            require(msg.value == _value, "InvalidAmount");
        } else {
            address _erc20Address = coins[_coinName];
            IERC20Tradable(_erc20Address).transferFrom(
                msg.sender,
                address(this),
                _value
            );
        }
        stakes[msg.sender][_coinName] = stakes[msg.sender][_coinName].add(
            _value
        );
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function mintMock(
        address _acc,
        uint256 _erc20Address,
        uint256 _value
    ) external {
        IERC20Tradable(_erc20Address).mint(_acc, _value);
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function burnMock(
        address _acc,
        uint256 _erc20Address,
        uint256 _value
    ) external {
        IERC20Tradable(_erc20Address).burn(_acc, _value);
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function setAggregationFee(string calldata _coinName, uint256 _value)
        external
    {
        aggregationFee[_coinName] += _value;
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function clearAggregationFee() external {
        for (uint256 i = 0; i < coinsName.length; i++) {
            delete aggregationFee[coinsName[i]];
        }
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function clearBSHPerifSetting() external {
        bshPeriphery = IBSHPeriphery(address(0));
    }

    //  @notice This is just an example to show how to add more function in upgrading a contract
    function setRefundableBalance(
        address _acc,
        string calldata _coinName,
        uint256 _value
    ) external {
        balances[_acc][_coinName].refundableBalance += _value;
    }
}
