// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../interfaces/IBMV.sol";
import "../libraries/String.sol";

contract MockBMC {
    using String for string;
    using String for address;

    string public btpAddr;
    IBMV public bmv;

    constructor(string memory _net) {
        btpAddr = string(
            abi.encodePacked(
                "btp://",
                _net,
                "/",
                address(this).addressToString(false)
            )
        );
    }

    function addVerifier(string calldata _net, address _addr) external {}

    function testHandleRelayMessage(
        address _bmvAddr,
        string calldata _prev,
        uint256 _seq,
        string memory _msg
    ) public returns (bytes[] memory) {
        bmv = IBMV(_bmvAddr);
        return bmv.handleRelayMessage(btpAddr, _prev, _seq, fromHex(_msg));
    }

    // Convert an hexadecimal character to their value
    function fromHexChar(uint8 c) private pure returns (uint8) {
        if (bytes1(c) >= bytes1("0") && bytes1(c) <= bytes1("9")) {
            return c - uint8(bytes1("0"));
        }
        if (bytes1(c) >= bytes1("a") && bytes1(c) <= bytes1("f")) {
            return 10 + c - uint8(bytes1("a"));
        }
        if (bytes1(c) >= bytes1("A") && bytes1(c) <= bytes1("F")) {
            return 10 + c - uint8(bytes1("A"));
        }
    }

    // Convert an hexadecimal string to raw bytes
    function fromHex(string memory s) private pure returns (bytes memory) {
        bytes memory ss = bytes(s);
        require(ss.length % 2 == 0); // length must be even
        bytes memory r = new bytes(ss.length / 2);
        for (uint256 i = 0; i < ss.length / 2; ++i) {
            r[i] = bytes1(
                fromHexChar(uint8(ss[2 * i])) *
                    16 +
                    fromHexChar(uint8(ss[2 * i + 1]))
            );
        }
        return r;
    }
}
