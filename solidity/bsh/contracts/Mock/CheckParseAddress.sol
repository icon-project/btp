// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../Libraries/ParseAddressLib.sol";

contract CheckParseAddress {
    using ParseAddress for address;
    using ParseAddress for string;

    function convertAddressToString(address _addr) external pure returns (string memory strAddr) {
        strAddr = _addr.toString();
    }

    function convertStringToAddress(string calldata _addr) external pure returns (address addr) {
        addr = _addr.parseAddress();
    }
}
