// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "./String.sol";

library Utils {
    using String for string;

    function remove(string[] storage arr, string memory _str) internal {
        for (uint256 i = 0; i < arr.length; i++)
            if (arr[i].compareTo(_str)) {
                arr[i] = arr[arr.length - 1];
                arr.pop();
                break;
            }
    }
}
