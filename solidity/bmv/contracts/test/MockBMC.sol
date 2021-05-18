// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../interfaces/IBMV.sol";
import "../libraries/LibString.sol";

contract MockBMC {
    using LibString for string;
    using LibString for address;

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
        string calldata _msg
    ) public returns (bytes[] memory) {
        bmv = IBMV(_bmvAddr);
        return bmv.handleRelayMessage(btpAddr, _prev, _seq, _msg);
    }
}