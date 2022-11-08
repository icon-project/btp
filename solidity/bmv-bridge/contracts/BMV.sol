// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMV.sol";

import "./libraries/Strings.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/BTPAddress.sol";
import "./libraries/Types.sol";
import "./libraries/Errors.sol";
import "./libraries/RLPDecodeStruct.sol";
import "./libraries/RLPEncode.sol";

contract BMV is IBMV {
    using BTPAddress for string;
    using Strings for string;
    using ParseAddress for string;
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
    function getConnectedBMC(
    ) external view returns (
        address
    ) {
        return bmcAddr;
    }

    function getStatus(
    ) external view override returns (
        IBMV.VerifierStatus memory
    ) {
        return IBMV.VerifierStatus(height, bytes(""));
    }

    function checkAccessible(
        string memory _currentAddr,
        string memory _fromAddr
    ) internal view {
        require(netAddr.compareTo(_fromAddr.networkAddress()), Errors.BMV_REVERT_INVALID_PREV_ADDR);
        require(msg.sender == bmcAddr, Errors.BMV_REVERT_UNAUTHORIZED);
        (, string memory _contractAddr) = _currentAddr.parseBTPAddress();

        require(
            _contractAddr.parseAddress(Errors.BMV_REVERT_INVALID_BMC_ADDR) == bmcAddr,
            Errors.BMV_REVERT_INVALID_BMC_ADDR
        );
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes calldata _msg
    ) external override returns (
        bytes[] memory
    ) {
        checkAccessible(_bmc, _prev);
        Types.RelayMessage memory relayMsg = _msg.decodeRelayMessage();
        Types.ReceiptProof memory rp;
        Types.MessageEvent memory ev;
        uint256 next_seq = _seq + 1;
        if (msgs.length > 0) delete msgs;
        for (uint256 i = 0; i < relayMsg.receiptProofs.length; i++) {
            rp = relayMsg.receiptProofs[i];
            if (rp.height < height) {
                continue;
                // ignore lower block height
            }
            height = rp.height;
            for (uint256 j = 0; j < rp.events.length; j++) {
                ev = rp.events[j];
                if (ev.seq < next_seq) {
                    continue;
                    // ignore lower sequence number
                } else if (ev.seq > next_seq) {
                    revert(Errors.BMV_REVERT_NOT_VERIFIABLE);
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
