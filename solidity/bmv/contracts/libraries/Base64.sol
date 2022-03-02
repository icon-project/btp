// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;

library Base64 {
    bytes private constant BASE64URLCHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    function decode(string memory _str) internal pure returns (bytes memory) {
        bytes memory _bs = bytes(_str);
        uint256 remove = 0;
        if (_bs[_bs.length - 1] == "=" && _bs[_bs.length - 2] == "=") {
            remove += 2;
        } else if (_bs[_bs.length - 1] == "=") {
            remove++;
        }
        uint256 resultLength = (_bs.length / 4) * 3 - remove;
        bytes memory result = new bytes(resultLength);

        uint256 i = 0;
        uint256 j = 0;
        for (; i + 4 < _bs.length; i += 4) {
            (result[j], result[j + 1], result[j + 2]) = decode4(
                mapBase64Char(_bs[i]),
                mapBase64Char(_bs[i + 1]),
                mapBase64Char(_bs[i + 2]),
                mapBase64Char(_bs[i + 3])
            );
            j += 3;
        }
        if (remove == 1) {
            (result[j], result[j + 1], ) = decode4(
                mapBase64Char(_bs[_bs.length - 4]),
                mapBase64Char(_bs[_bs.length - 3]),
                mapBase64Char(_bs[_bs.length - 2]),
                0
            );
        } else if (remove == 2) {
            (result[j], , ) = decode4(
                mapBase64Char(_bs[_bs.length - 4]),
                mapBase64Char(_bs[_bs.length - 3]),
                0,
                0
            );
        } else {
            (result[j], result[j + 1], result[j + 2]) = decode4(
                mapBase64Char(_bs[_bs.length - 4]),
                mapBase64Char(_bs[_bs.length - 3]),
                mapBase64Char(_bs[_bs.length - 2]),
                mapBase64Char(_bs[_bs.length - 1])
            );
        }
        return result;
    }

    //solhint-disable-next-line
    function mapBase64Char(bytes1 _char) private pure returns (uint8 res) {
        //solhint-disable-next-line var-name-mixedcase
        uint8 A = 0;
        uint8 a = 26;
        uint8 zero = 52;
        if (uint8(_char) == 45) {
            res = 62;
        } else if (uint8(_char) == 95) {
            res = 63;
        } else if (uint8(_char) >= 48 && uint8(_char) <= 57) {
            res = zero + (uint8(_char) - 48);
        } else if (uint8(_char) >= 65 && uint8(_char) <= 90) {
            res = A + (uint8(_char) - 65);
        } else if (uint8(_char) >= 97 && uint8(_char) <= 122) {
            res = a + (uint8(_char) - 97);
        }
    }

    function decode4(
        uint256 a0,
        uint256 a1,
        uint256 a2,
        uint256 a3
    )
        private
        pure
        returns (
            bytes1,
            bytes1,
            bytes1
        )
    {
        uint256 n =
            ((a0 & 63) << 18) |
                ((a1 & 63) << 12) |
                ((a2 & 63) << 6) |
                (a3 & 63);
        uint256 b0 = (n >> 16) & 255;
        uint256 b1 = (n >> 8) & 255;
        uint256 b2 = (n) & 255;
        return (bytes1(uint8(b0)), bytes1(uint8(b1)), bytes1(uint8(b2)));
    }

    function encode(bytes memory _bs) internal pure returns (string memory) {
        uint256 rem = _bs.length % 3;
        uint256 resLength;
        if (_bs.length % 3 != 0) {
            resLength = (_bs.length / 3) * 4 + 4;
        } else {
            resLength = (_bs.length / 3) * 4;
        }

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
