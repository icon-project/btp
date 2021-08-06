// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../DataValidator.sol";
import "../libraries/String.sol";
import "../libraries/Bytes.sol";
import "../libraries/MessageDecoder.sol";
import "../libraries/Verifier.sol";

contract MockDataValidator is DataValidator {
    using String for string;
    using Bytes for bytes;
    using MessageDecoder for bytes;
    using MessageDecoder for string;
    using MessageDecoder for Types.EventLog;
    using Verifier for Types.ReceiptProof;
using String for uint256;
event Debug(bytes[] msg);
    function validateReceipt(
        string memory, /* _bmc */
        string memory _prev,
        uint256 _seq,
        bytes memory _serializedMsg,
        bytes32 _receiptHash
    ) external override returns (bytes[] memory) {
        uint256 nextSeq = _seq + 1;
        Types.Receipt memory receipt;
        Types.MessageEvent memory messageEvent;

        Types.ReceiptProof[] memory receiptProofs =
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
                   //revert(messageEvent.seq.toString().concat(nextSeq.toString()));
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
            //emit Debug(msgs);
            // bytes32 msg1=msgs[0].bytesToBytes32();
            // revert(bytes32ToString(msg1));
        }
        return msgs;
    }

    function bytes32ToString(bytes32 _bytes32) public pure returns (string memory) {
        uint8 i = 0;
        while(i < 32 && _bytes32[i] != 0) {
            i++;
        }
        bytes memory bytesArray = new bytes(i);
        for (i = 0; i < 32 && _bytes32[i] != 0; i++) {
            bytesArray[i] = _bytes32[i];
        }
        return string(bytesArray);
    }
}
