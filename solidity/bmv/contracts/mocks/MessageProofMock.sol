// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "../libraries/MessageProofLib.sol";

contract MessageProofMock {
    using MessageProofLib for MessageProofLib.MessageProof;

    function decode(bytes memory enc) public pure returns (MessageProofLib.MessageProof memory) {
        MessageProofLib.MessageProof memory mp = MessageProofLib.decode(enc);
        return mp;
    }

    function calculate(bytes memory enc) public pure returns (bytes32) {
        MessageProofLib.MessageProof memory mp = MessageProofLib.decode(enc);
        (bytes32 root, ) = mp.calculate();
        return root;
    }
}
