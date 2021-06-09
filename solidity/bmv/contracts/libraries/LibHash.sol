// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;

/**
    @notice pre-compiles contract address https://github.com/hicommonwealth/edgeware-node/blob/drew-erup-4/node/runtime/src/precompiles.rs
 */
library LibHash {
    function sha3FIPS256(bytes memory _input) internal returns (bytes memory) {
        address sha3FIPS256Address = address(0x0000000000000000000000000000000000001024);
        (, bytes memory returnData) = sha3FIPS256Address.call(_input);
        return returnData;
    }

    function ecrecoverPublicKey(bytes memory input) internal returns(bytes memory) {
        address ecrecoverAddress = address(0x0000000000000000000000000000000000001027);
        (, bytes memory returnData) = ecrecoverAddress.call(input);
        return returnData;
    }
}