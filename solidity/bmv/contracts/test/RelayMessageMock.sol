// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "../libraries/RelayMessageLib.sol";

contract RelayMessageMock {
    using RelayMessageLib for RelayMessage;

    function decode(bytes calldata enc) public pure returns (RelayMessage[] memory) {
        return RelayMessageLib.decode(enc);
    }
}
