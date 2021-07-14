// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;

import "./StringsLib.sol";

library Utils {
    using Strings for string;

    /**
    @notice this function return a ceiling value of division
    @dev No need to check validity of num2 (num2 != 0)
    @dev It is checked before calling this function
    */
    function ceilDiv(uint256 num1, uint256 num2)
        internal
        pure
        returns (uint256)
    {
        if (num1 % num2 == 0) {
            return num1 / num2;
        }
        return (num1 / num2) + 1;
    }

    function getScale(uint256 _blockIntervalSrc, uint256 _blockIntervalDst)
        internal
        pure
        returns (uint256)
    {
        if (_blockIntervalSrc < 1 || _blockIntervalDst < 1) {
            return 0;
        }
        return ceilDiv(_blockIntervalSrc * 10**6, _blockIntervalDst);
    }

    function getRotateTerm(uint256 _maxAggregation, uint256 _scale)
        internal
        pure
        returns (uint256)
    {
        if (_scale > 0) {
            return ceilDiv(_maxAggregation * 10**6, _scale);
        }
        return 0;
    }

    function remove(string[] storage arr, string memory _str) internal {
        for (uint256 i = 0; i < arr.length; i++)
            if (arr[i].compareTo(_str)) {
                arr[i] = arr[arr.length - 1];
                arr.pop();
                break;
            }
    }
}
