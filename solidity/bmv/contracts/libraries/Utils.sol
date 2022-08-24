// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

library Utils {
    function recoverSigner(bytes32 message, bytes memory signature) internal pure returns (address signer) {
        (bytes32 r, bytes32 s, uint8 v) = splitSignature(signature);
        return ecrecover(message, v + 27, r, s);
    }

    function splitSignature(bytes memory signature)
        private
        pure
        returns (
            bytes32,
            bytes32,
            uint8
        )
    {
        require(signature.length == 65);

        bytes32 r;
        bytes32 s;
        uint8 v;
        assembly {
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
            v := byte(0, mload(add(signature, 96)))
        }
        return (r, s, v);
    }

    function append(bytes[] memory v, bytes[] memory w) internal pure returns (bytes[] memory) {
        if (w.length <= 0) {
            return v;
        }

        bytes[] memory t = v;
        uint256 l = v.length;
        v = new bytes[](v.length + w.length);
        for (uint256 i = 0; i < l; i++) {
            v[i] = t[i];
        }
        for (uint256 i = 0; i < w.length; i++) {
            v[l + i] = w[i];
        }
        return v;
    }
}
