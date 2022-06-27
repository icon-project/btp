// SPDX-License-Identifier: MIT
pragma solidity >= 0.4.22 < 0.9.0;

import "../libraries/RelayMessageLib.sol";

contract RelayMessageMock {

    using RelayMessageLib for RelayMessageLib.RelayMessage;

    function decode(bytes calldata enc) public pure returns (RelayMessageLib.RelayMessage[] memory) {
        return RelayMessageLib.decode(enc);
    }

}

