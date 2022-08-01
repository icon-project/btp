// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../BMCManagement.sol";
import "../libraries/Utils.sol";

contract MockBMCManagement is BMCManagement {
    using Utils for uint256;

    struct RelayInfo {
        address r;
        uint256 cb;
        uint256 rh;
    }

    RelayInfo private relay;

    function getRelay() external view returns (RelayInfo memory) {
        return relay;
    }

    function mineOneBlock() external {}
}
