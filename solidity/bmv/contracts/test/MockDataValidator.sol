// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../DataValidator.sol";
import "../libraries/LibString.sol";
import "../libraries/LibMsgDecoder.sol";
import "../libraries/LibVerifier.sol";

contract MockDataValidator is DataValidator {
    using LibString for string;
    using LibMsgDecoder for bytes;
    using LibMsgDecoder for string;
    using LibMsgDecoder for LibTypes.EventLog;
    using LibVerifier for LibTypes.ReceiptProof;

    function validateReceipt(
        string memory, /* _bmc */
        string memory _prev,
        uint256 _seq,
        bytes memory _serializedMsg,
        bytes32 _receiptHash
    ) external override returns (bytes[] memory) {
        uint256 nextSeq = _seq + 1;
        LibTypes.Receipt memory receipt;
        LibTypes.MessageEvent memory messageEvent;

        LibTypes.ReceiptProof[] memory receiptProofs =
            _serializedMsg.decodeReceiptProofs();

        (, string memory contractAddr) = _prev.splitBTPAddress();
        if (msgs.length > 0) delete msgs;
        for (uint256 i = 0; i < receiptProofs.length; i++) {
            receipt = receiptProofs[i].verifyMPTProof(_receiptHash);
            for (uint256 j = 0; j < receipt.eventLogs.length; j++) {
                if (!receipt.eventLogs[j].addr.compareTo(contractAddr))
                    continue;
                messageEvent = receipt.eventLogs[j].toMessageEvent();
                if (bytes(messageEvent.nextBmc).length != 0) {
                    if (messageEvent.seq > nextSeq)
                        revert("BMVRevertInvalidSequenceHigher");
                    else if (messageEvent.seq < nextSeq)
                        revert("BMVRevertInvalidSequence");
                    else if (
                        // @dev mock implementation for testing
                        // messageEvent.nextBmc.compareTo(_bmc)
                        bytes(messageEvent.nextBmc).length > 0
                    ) {
                        msgs.push(messageEvent.message);
                        nextSeq += 1;
                    }
                }
            }
        }
        return msgs;
    }
}
