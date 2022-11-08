// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "./Strings.sol";

library Utils {
    using Strings for string;

    function removeFromStrings(string[] storage arr, string memory _str) internal returns (bool) {
        uint256 last = arr.length - 1;
        for (uint256 i = 0; i <= last; i++) {
            if (arr[i].compareTo(_str)) {
                if (i < last) {
                    arr[i] = arr[last];
                }
                arr.pop();
                return true;
            }
        }
        return false;
    }

    function containsFromStrings(string[] memory arr, string memory _str) internal pure returns (bool) {
        for (uint256 i = 0; i < arr.length; i++) {
            if (arr[i].compareTo(_str)) {
                return true;
            }
        }
        return false;
    }

    function removeFromAddresses(address[] storage arr, address _addr) internal {
        uint256 last = arr.length - 1;
        for (uint256 i = 0; i <= last; i++) {
            if (arr[i] == _addr) {
                if (i < last) {
                    arr[i] = arr[last];
                }
                arr.pop();
                break;
            }
        }
    }

    function containsFromAddresses(address[] memory arr, address _addr) internal pure returns (bool) {
        for (uint256 i = 0; i < arr.length; i++) {
            if (arr[i] == _addr) {
                return true;
            }
        }
        return false;
    }

    function removeFromUints(uint256[] storage arr, uint256 _value) internal returns (bool) {
        uint256 last = arr.length - 1;
        for (uint256 i = 0; i <= last; i++) {
            if (arr[i] == _value) {
                if (i < last) {
                    arr[i] = arr[last];
                }
                arr.pop();
                return true;
            }
        }
        return false;
    }

    function containsFromUints(uint256[] memory arr, uint256 _value) internal pure returns (bool) {
        for (uint256 i = 0; i < arr.length; i++) {
            if (arr[i] == _value) {
                return true;
            }
        }
        return false;
    }

    function sumFromUints(uint256[] memory arr) internal pure returns (uint256) {
        uint256 sum = 0;
        for (uint256 i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }
}
