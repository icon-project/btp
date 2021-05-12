// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;

/**
 * @title EncodeBase64
 * @dev A simple Base64 encoding library.
 * The original library was modified to make it work for our case
 * For more info, please check the link: https://github.com/OpenZeppelin/solidity-jwt.git
 */
library EncodeBase64 {
    bytes private constant BASE64URLCHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    function encode(bytes memory _bs) internal pure returns (string memory) {
        // bytes memory _bs = bytes(_str);
        uint256 rem = _bs.length % 3;
        uint256 resLength;
        if (_bs.length % 3 != 0) {
            resLength = (_bs.length / 3) * 4 + 4;
        } else {
            resLength = (_bs.length / 3) * 4;
        }

        //   uint256 res_length = (_bs.length + 2) / 3 * 4 - ((3 - rem) % 3);
        bytes memory res = new bytes(resLength);
        uint256 i = 0;
        uint256 j = 0;

        for (; i + 3 <= _bs.length; i += 3) {
            (res[j], res[j + 1], res[j + 2], res[j + 3]) = encode3(
                uint8(_bs[i]),
                uint8(_bs[i + 1]),
                uint8(_bs[i + 2])
            );
            j += 4;
        }

        if (rem != 0) {
            uint8 la0 = uint8(_bs[_bs.length - rem]);
            uint8 la1 = 0;
            if (rem == 2) {
                la1 = uint8(_bs[_bs.length - 1]);
            }
            (bytes1 b0, bytes1 b1, bytes1 b2, ) = encode3(la0, la1, 0);
            res[j] = b0;
            res[j + 1] = b1;
            if (rem == 1) {
                res[j + 2] = "=";
                res[j + 3] = "=";
            } else if (rem == 2) {
                res[j + 2] = b2;
                res[j + 3] = "=";
            }
        }
        return string(res);
    }

    function encode3(
        uint256 a0,
        uint256 a1,
        uint256 a2
    )
        private
        pure
        returns (
            bytes1 b0,
            bytes1 b1,
            bytes1 b2,
            bytes1 b3
        )
    {
        uint256 n = (a0 << 16) | (a1 << 8) | a2;
        uint256 c0 = (n >> 18) & 63;
        uint256 c1 = (n >> 12) & 63;
        uint256 c2 = (n >> 6) & 63;
        uint256 c3 = (n) & 63;
        b0 = BASE64URLCHARS[c0];
        b1 = BASE64URLCHARS[c1];
        b2 = BASE64URLCHARS[c2];
        b3 = BASE64URLCHARS[c3];
    }
}
