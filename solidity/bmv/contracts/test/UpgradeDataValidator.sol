// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../interfaces/IDataValidator.sol";

import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";

contract DataValidatorV2 is IDataValidator, Initializable {
    bytes[] private msgs;

    function initialize() public initializer {}

    function validateReceipt(
        string memory, /* _bmc */
        string memory, /* _prev */
        uint256, /* _seq */
        bytes memory, /* _serializedMsg */
        bytes32 /* _receiptHash */
    ) external override returns (bytes[] memory) {
        msgs.push(bytes("Succeed to upgrade Data Validator contract"));
        return msgs;
    }
}
