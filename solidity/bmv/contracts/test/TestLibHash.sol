// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../libraries/LibHash.sol";

contract TestLibHash {
    using LibHash for bytes;

    function sha3FIPS256(bytes memory _input) external returns (bytes memory) {
        return _input.sha3FIPS256();
    }

    function ecrecoverPublicKey(bytes memory _input) external returns (bytes memory) {
        return _input.ecrecoverPublicKey();
    }
}