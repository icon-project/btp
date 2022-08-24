// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "../libraries/MessageProofLib.sol";

contract MessageProofMock {
    using MessageProofLib for MessageProof;

    function decode(bytes memory enc) public pure returns (MessageProof memory) {
        MessageProof memory mp = MessageProofLib.decode(enc);
        return mp;
    }

    function calculate(bytes memory enc) public pure returns (bytes32) {
        MessageProof memory mp = MessageProofLib.decode(enc);
        (bytes32 root, ) = mp.calculate();
        return root;
    }
}
