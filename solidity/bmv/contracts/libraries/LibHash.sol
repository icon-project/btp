// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;

import "./LibBytes.sol";

/**
    @notice pre-compiles contract address https://github.com/PureStake/moonbeam/blob/master/runtime/moonbeam/src/precompiles.rs
 */
library LibHash {
    using LibBytes for bytes;

    function sha3FIPS256(bytes memory _input) internal returns (bytes32) {
        address sha3FIPS256Address =
            address(0x0000000000000000000000000000000000000400); // hash(1024)
        (, bytes memory _returnData) = sha3FIPS256Address.call(_input);
        return _returnData.bytesToBytes32();
    }

    function ecRecoverPublicKey(
        bytes32 _messageHash,
        bytes memory _voteSignature
    ) internal returns (bytes memory) {
        bytes memory _r = _voteSignature.slice(0, 32);
        bytes memory _s = _voteSignature.slice(32, 64);
        bytes memory _v = _voteSignature.slice(64, 65);
        bytes memory _prefix = new bytes(31);
        bytes memory _input =
            abi.encodePacked(_messageHash, _prefix, _v, _r, _s);
        address ecRecoverAddress =
            address(0x0000000000000000000000000000000000000402); // hash(1026)
        (, bytes memory _returnData) = ecRecoverAddress.call(_input);
        return _returnData;
    }
}
