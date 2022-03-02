// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;

/**
 * @title DecodeBase64
 * @dev A simple Base64 decoding library.
 * @author Quang Tran
 */

library DecodeBase64 {
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

    function mapBase64Char(bytes1 _char) private pure returns (uint8) {
        // solhint-disable-next-line
        uint8 A = 0;
        uint8 a = 26;
        uint8 zero = 52;
        if (uint8(_char) == 45) {
            return 62;
        } else if (uint8(_char) == 95) {
            return 63;
        } else if (uint8(_char) >= 48 && uint8(_char) <= 57) {
            return zero + (uint8(_char) - 48);
        } else if (uint8(_char) >= 65 && uint8(_char) <= 90) {
            return A + (uint8(_char) - 65);
        } else if (uint8(_char) >= 97 && uint8(_char) <= 122) {
            return a + (uint8(_char) - 97);
        }
        return 0;
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
        uint256 n = ((a0 & 63) << 18) |
            ((a1 & 63) << 12) |
            ((a2 & 63) << 6) |
            (a3 & 63);
        uint256 b0 = (n >> 16) & 255;
        uint256 b1 = (n >> 8) & 255;
        uint256 b2 = (n) & 255;
        return (bytes1(uint8(b0)), bytes1(uint8(b1)), bytes1(uint8(b2)));
    }
}
