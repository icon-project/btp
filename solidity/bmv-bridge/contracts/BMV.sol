// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMV.sol";

import "./libraries/String.sol";
import "./libraries/Types.sol";
import "./libraries/RLPDecodeStruct.sol";
import "./libraries/RLPEncode.sol";

contract BMV is IBMV {
    using Strings for string;
    using RLPDecodeStruct for bytes;

    address private bmcAddr;
    string private netAddr;
    uint256 private height;
    bytes[] internal msgs;

    constructor (
        address _bmcAddr,
        string memory _netAddr,
        uint256 _height
    ) {
        bmcAddr = _bmcAddr;
        netAddr = _netAddr;
        height = _height;
    }

    /**
        @return connected BMC address
     */
    function getConnectedBMC() external view returns (address) {
        return bmcAddr;
    }

    /**
        @notice Used by the relay to resolve next BTP Message to send.
                Called by BMC.
        @return height Last verified block height
        @return extra  extra rlp encoded bytes
     */
    function getStatus()
        external
        view
        override
        returns (uint256, bytes memory)
    {
        return (height, bytes(""));
    }

    function checkAccessible(
        string memory _currentAddr,
        string memory _fromAddr
    ) internal view {
        (string memory _net, ) = _fromAddr.splitBTPAddress();
        require(netAddr.compareTo(_net), "BMVRevert: Invalid previous BMC");
        require(msg.sender == bmcAddr, "BMVRevert: Invalid BMC");
        (, string memory _contractAddr) = _currentAddr.splitBTPAddress();

        require(
            _contractAddr.parseAddress() == bmcAddr,
            "BMVRevert: Invalid BMC"
        );
    }

    /**
        @notice Decodes Relay Messages and process BTP Messages.
                If there is an error, then it sends a BTP Message containing the Error Message.
                BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
        @param _bmc BTP Address of the BMC handling the message
        @param _prev BTP Address of the previous BMC
        @param _seq next sequence number to get a message
        @param _msg serialized bytes of Relay Message
        @return serializedMessages List of serialized bytes of a BTP Message
     */
    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes calldata _msg
    ) external override returns (bytes[] memory) {
        checkAccessible(_bmc, _prev);
        Types.RelayMessage memory relayMsg = _msg.decodeRelayMessage();
        Types.ReceiptProof memory rp;
        Types.MessageEvent memory ev;
        uint256 next_seq = _seq + 1;
        if (msgs.length > 0) delete msgs;
        for (uint256 i = 0; i < relayMsg.receiptProofs.length; i++) {
            rp = relayMsg.receiptProofs[i];
            if (rp.height < height) {
                continue; // ignore lower block height
            }
            height = rp.height;
            for (uint256 j = 0; j < rp.events.length; j++) {
                ev = rp.events[j];
                if (ev.seq < next_seq) {
                    continue;  // ignore lower sequence number
                } else if (ev.seq > next_seq) {
                    revert("BMVRevertInvalidSequence");
                }
                if (!ev.nextBmc.compareTo(_bmc)) {
                    continue;
                }
                msgs.push(ev.message);
                next_seq++;
            }
        }
        return msgs;
    }
}
