// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

library Utils {

    function recoverSigner(bytes32 message, bytes memory signature)
    internal
    pure
    returns (address signer)
    {
            (bytes32 r, bytes32 s, uint8 v) = splitSignature(signature);
            return ecrecover(message, v, r, s);
    }

    function splitSignature(bytes memory signature)
    private
    pure
    returns (bytes32, bytes32, uint8)
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
}

