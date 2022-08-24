// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import {Header, Proof, BlockUpdateLib} from "./BlockUpdateLib.sol";
import {MessageProof, MessageProofLib} from "./MessageProofLib.sol";
import "./RLPReader.sol";

struct RelayMessage {
    uint256 typ;
    bytes mesg;
}

library RelayMessageLib {
    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    uint256 constant TYPE_BLOCK_UPDATE = 1;
    uint256 constant TYPE_MESSAGE_PROOF = 2;

    function decode(bytes memory enc) internal pure returns (RelayMessage[] memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();

        RelayMessage[] memory rms = new RelayMessage[](tl.length);
        for (uint256 i = 0; i < tl.length; i++) {
            RLPReader.RLPItem[] memory ms = tl[i].toList();
            rms[i].typ = ms[0].toUint();
            rms[i].mesg = ms[1].toBytes();
        }

        return rms;
    }

    function toBlockUpdate(RelayMessage memory rm) internal pure returns (Header memory, Proof memory) {
        require(rm.typ == TYPE_BLOCK_UPDATE, "RelayMessage: Support only BlockUpdate type");
        return BlockUpdateLib.decode(rm.mesg);
    }

    function toMessageProof(RelayMessage memory rm) internal pure returns (MessageProof memory) {
        require(rm.typ == TYPE_MESSAGE_PROOF, "RelayMessage: Support only MessageProof type");
        return MessageProofLib.decode(rm.mesg);
    }
}
