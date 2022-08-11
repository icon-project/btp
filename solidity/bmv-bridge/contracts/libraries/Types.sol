// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

library Types {
    /**
     * @Notice List of ALL Struct being used to Encode and Decode RLP Messages
     */

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
