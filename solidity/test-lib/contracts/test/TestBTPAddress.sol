// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/BTPAddress.sol";

contract TestBTPAddress {
    using BTPAddress for string;

    function parseBTPAddress(
        string memory _base
    ) external pure returns (
        string memory,
        string memory
    ) {
        return _base.parseBTPAddress();
    }

    function networkAddress(
        string memory _base
    ) external pure returns (
        string memory
    ) {
        return _base.networkAddress();
    }

    function btpAddress(
        string memory _net,
        string memory _addr
    ) external pure returns (
        string memory
    ) {
        return _net.btpAddress(_addr);
    }

}
