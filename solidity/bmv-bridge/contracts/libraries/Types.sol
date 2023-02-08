// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

library Types {
    struct ReceiptProof {
        uint256 index;
        MessageEvent[] events;
        uint256 height;
    }

    struct MessageEvent {
        string nextBmc;
        uint256 seq;
        bytes message;
    }

    struct RelayMessage {
        ReceiptProof[] receiptProofs;
    }

}
