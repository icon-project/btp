// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./BlockUpdateLib.sol";
import "./MessageProofLib.sol";
import "./RLPReader.sol";

library RelayMessageLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    uint constant TypeBlockUpdate = 1;
    uint constant TypeMessageProof = 2;

    struct RelayMessage {
        uint typ;
        bytes mesg;
    }

    function decode(bytes memory enc) internal returns (RelayMessage[] memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();

        RelayMessage[] memory rms = new RelayMessage[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            RLPReader.RLPItem[] memory ms = tl[i].toList();
            rms[i].typ = ms[0].toUint();
            rms[i].mesg = ms[1].toBytes();
        }

        return rms;
    }

    function toBlockUpdate(RelayMessage memory rm)
    internal
    returns (BlockUpdateLib.BlockUpdate memory)
    {
        require(rm.typ == TypeBlockUpdate);
        return BlockUpdateLib.decode(rm.mesg);
    }

    function toMessageProof(RelayMessage memory rm)
    internal
    returns (MessageProofLib.MessageProof memory)
    {
        require(rm.typ == TypeMessageProof);
        return MessageProofLib.decode(rm.mesg);
    }
}
