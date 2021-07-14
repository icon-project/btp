// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "../interfaces/IBSH.sol";
import "../interfaces/IBMCPeriphery.sol";
import "../interfaces/IBMCManagement.sol";
import "../interfaces/IBMV.sol";
import "../libraries/ParseAddressLib.sol";
import "../libraries/RLPDecodeStructLib.sol";
import "../libraries/RLPEncodeStructLib.sol";
import "../libraries/StringsLib.sol";
import "../libraries/TypesLib.sol";
import "../libraries/Utils.sol";

import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";

contract BMCPeripheryV2 is IBMCPeriphery, Initializable {
    using Strings for string;
    using ParseAddress for address;
    using RLPDecodeStruct for bytes;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.Response;
    using Utils for uint256;

    uint256 internal constant UNKNOWN_ERR = 0;
    uint256 internal constant BMC_ERR = 10;
    uint256 internal constant BMV_ERR = 25;
    uint256 internal constant BSH_ERR = 40;

    string private bmcBtpAddress; // a network address BMV, i.e. btp://1234.pra/0xabcd
    address private bmcManagement;

    function initialize(string memory _network, address _bmcManagementAddr)
        public
        initializer
    {
        bmcBtpAddress = string("btp://").concat(_network).concat("/").concat(
            address(this).toString()
        );
        bmcManagement = _bmcManagementAddr;
    }

    event Message(
        string _next, //  an address of the next BMC (it could be a destination BMC)
        uint256 _seq, //  a sequence number of BMC (NOT sequence number of BSH)
        bytes _msg
    );

    // emit errors in BTP messages processing
    event ErrorOnBTPError(
        string _svc,
        int256 _sn,
        uint256 _code,
        string _errMsg,
        uint256 _svcErrCode,
        string _svcErrMsg
    );

    function getBmcBtpAddress() external view override returns (string memory) {
        return bmcBtpAddress;
    }

    /**
       @notice Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
       @dev Caller must be a registered relayer.     
       @param _prev    BTP Address of the BMC generates the message
       @param _msg     base64 encoded string of serialized bytes of Relay Message refer RelayMessage structure
     */
    function handleRelayMessage(string calldata _prev, string calldata _msg)
        external
        override
    {
        bytes[] memory serializedMsgs = decodeMsgAndValidateRelay(_prev, _msg);
        string memory _net;

        // dispatch BTP Messages
        Types.BMCMessage memory _message;
        for (uint256 i = 0; i < serializedMsgs.length; i++) {
            try this.decodeBTPMessage(serializedMsgs[i]) returns (
                Types.BMCMessage memory _decoded
            ) {
                _message = _decoded;
            } catch {
                // ignore BTPMessage parse failure?
                continue;
            }

            if (_message.dst.compareTo(bmcBtpAddress)) {
                handleMessage(_prev, _message);
            } else {
                (_net, ) = _message.dst.splitBTPAddress();
                try IBMCManagement(bmcManagement).resolveRoute(_net) returns (
                    string memory _nextLink,
                    string memory
                ) {
                    _sendMessage(_nextLink, serializedMsgs[i]);
                } catch Error(string memory _error) {
                    _sendError(_prev, _message, BMC_ERR, _error);
                }
            }
        }
        IBMCManagement(bmcManagement).updateLinkRxSeq(
            _prev,
            serializedMsgs.length
        );
    }

    function decodeMsgAndValidateRelay(
        string calldata _prev,
        string calldata _msg
    ) internal returns (bytes[] memory) {
        (string memory _net, ) = _prev.splitBTPAddress();
        address _bmvAddr =
            IBMCManagement(bmcManagement).getBmvServiceByNet(_net);

        require(_bmvAddr != address(0), "BMCRevertNotExistsBMV");
        (uint256 _prevHeight, , ) = IBMV(_bmvAddr).getStatus();

        // decode and verify relay message
        bytes[] memory serializedMsgs =
            IBMV(_bmvAddr).handleRelayMessage(
                bmcBtpAddress,
                _prev,
                IBMCManagement(bmcManagement).getLinkRxSeq(_prev),
                _msg
            );

        // rotate and check valid relay
        (uint256 _height, uint256 _lastHeight, ) = IBMV(_bmvAddr).getStatus();
        address relay =
            IBMCManagement(bmcManagement).rotateRelay(
                _prev,
                block.number,
                _lastHeight,
                serializedMsgs.length > 0
            );

        if (relay == address(0)) {
            address[] memory relays =
                IBMCManagement(bmcManagement).getLinkRelays(_prev);
            bool check;
            for (uint256 i = 0; i < relays.length; i++)
                if (msg.sender == relays[i]) {
                    check = true;
                    break;
                }
            require(check, "BMCRevertUnauthorized: not registered relay");
            relay = msg.sender;
        } else if (relay != msg.sender)
            revert("BMCRevertUnauthorized: invalid relay");

        IBMCManagement(bmcManagement).updateRelayStats(
            relay,
            _height - _prevHeight,
            serializedMsgs.length
        );
        return serializedMsgs;
    }

    //  @dev Despite this function was set as external, it should be called internally
    //  since Solidity does not allow using try_catch with internal function
    //  this solution can solve the issue
    function decodeBTPMessage(bytes memory _rlp)
        external
        pure
        returns (Types.BMCMessage memory)
    {
        return _rlp.decodeBMCMessage();
    }

    function handleMessage(string calldata _prev, Types.BMCMessage memory _msg)
        internal
    {
        address _bshAddr;
        if (_msg.svc.compareTo("_event")) {
            Types.EventMessage memory _eventMsg =
                _msg.message.decodeEventMessage();
            Types.Link memory link =
                IBMCManagement(bmcManagement).getLink(_eventMsg.conn.from);
            if (_eventMsg.eventType.compareTo("Link")) {
                bool check;
                if (link.isConnected) {
                    for (uint256 i = 0; i < link.reachable.length; i++)
                        if (_eventMsg.conn.to.compareTo(link.reachable[i])) {
                            check = true;
                            break;
                        }
                    if (!check)
                        IBMCManagement(bmcManagement).updateLinkReachable(
                            _eventMsg.conn.from,
                            _eventMsg.conn.to
                        );
                }
            } else if (_eventMsg.eventType.compareTo("Unlink")) {
                if (link.isConnected) {
                    for (uint256 i = 0; i < link.reachable.length; i++) {
                        if (_eventMsg.conn.to.compareTo(link.reachable[i]))
                            IBMCManagement(bmcManagement).deleteLinkReachable(
                                _eventMsg.conn.from,
                                i
                            );
                    }
                }
            } else revert("BMCRevert: not exists event handler");
        } else if (_msg.svc.compareTo("FeeGathering")) {
            Types.BMCService memory _sm;
            try this.tryDecodeBMCService(_msg.message) returns (
                Types.BMCService memory res
            ) {
                _sm = res;
            } catch {
                _sendError(_prev, _msg, BMC_ERR, "BMCRevertParseFailure");
            }
            if (_sm.serviceType.compareTo("FeeGathering")) {
                Types.GatherFeeMessage memory _gatherFee;
                try this.tryDecodeGatherFeeMessage(_sm.payload) returns (
                    Types.GatherFeeMessage memory res
                ) {
                    _gatherFee = res;
                } catch {
                    _sendError(_prev, _msg, BMC_ERR, "BMCRevertParseFailure");
                }

                for (uint256 i = 0; i < _gatherFee.svcs.length; i++) {
                    _bshAddr = IBMCManagement(bmcManagement)
                        .getBshServiceByName(_gatherFee.svcs[i]);
                    //  If 'svc' not found, ignore
                    if (_bshAddr != address(0)) {
                        try
                            IBSH(_bshAddr).handleFeeGathering(
                                _gatherFee.fa,
                                _gatherFee.svcs[i]
                            )
                        {} catch {
                            //  If BSH contract throws a revert error, ignore and continue
                        }
                    }
                }
            }
        } else {
            _bshAddr = IBMCManagement(bmcManagement).getBshServiceByName(
                _msg.svc
            );
            if (_bshAddr == address(0)) {
                _sendError(_prev, _msg, BMC_ERR, "BMCRevertNotExistsBSH");
                return;
            }

            if (_msg.sn >= 0) {
                (string memory _net, ) = _msg.src.splitBTPAddress();
                try
                    IBSH(_bshAddr).handleBTPMessage(
                        _net,
                        _msg.svc,
                        uint256(_msg.sn),
                        _msg.message
                    )
                {} catch Error(string memory _error) {
                    _sendError(_prev, _msg, BSH_ERR, _error);
                }
            } else {
                Types.Response memory _errMsg = _msg.message.decodeResponse();
                try
                    IBSH(_bshAddr).handleBTPError(
                        _msg.src,
                        _msg.svc,
                        uint256(_msg.sn * -1),
                        _errMsg.code,
                        _errMsg.message
                    )
                {} catch Error(string memory _error) {
                    emit ErrorOnBTPError(
                        _msg.svc,
                        _msg.sn * -1,
                        _errMsg.code,
                        _errMsg.message,
                        BSH_ERR,
                        _error
                    );
                } catch (bytes memory _error) {
                    emit ErrorOnBTPError(
                        _msg.svc,
                        _msg.sn * -1,
                        _errMsg.code,
                        _errMsg.message,
                        UNKNOWN_ERR,
                        string(_error)
                    );
                }
            }
        }
    }

    //  @dev Solidity does not allow using try_catch with internal function
    //  Thus, work-around solution is the followings
    //  If there is any error throwing, BMC contract can catch it, then reply back a RC_ERR Response
    function tryDecodeBMCService(bytes calldata _msg)
        external
        pure
        returns (Types.BMCService memory)
    {
        return _msg.decodeBMCService();
    }

    function tryDecodeGatherFeeMessage(bytes calldata _msg)
        external
        pure
        returns (Types.GatherFeeMessage memory)
    {
        return _msg.decodeGatherFeeMessage();
    }

    function _sendMessage(string memory _to, bytes memory _serializedMsg)
        internal
    {
        IBMCManagement(bmcManagement).updateLinkTxSeq(_to);
        emit Message(
            _to,
            IBMCManagement(bmcManagement).getLinkTxSeq(_to),
            _serializedMsg
        );
    }

    function _sendError(
        string calldata _prev,
        Types.BMCMessage memory _message,
        uint256 _errCode,
        string memory _errMsg
    ) internal {
        if (_message.sn > 0) {
            bytes memory _serializedMsg =
                Types
                    .BMCMessage(
                    bmcBtpAddress,
                    _message
                        .src,
                    _message
                        .svc,
                    _message.sn * -1,
                    Types.Response(_errCode, _errMsg).encodeResponse()
                )
                    .encodeBMCMessage();
            _sendMessage(_prev, _serializedMsg);
        }
    }

    function sendMessage(
        string memory,
        string memory,
        uint256,
        bytes memory
    ) external pure override {
        revert("Upgrade successfully");
    }

    /*
       @notice Get status of BMC.
       @param _link        BTP Address of the connected BMC.
       @return tx_seq       Next sequence number of the next sending message.
       @return rx_seq       Next sequence number of the message to receive.
       @return verifier     VerifierStatus Object contains status information of the BMV.
    */
    function getStatus(string calldata _link)
        public
        view
        override
        returns (Types.LinkStats memory _linkStats)
    {
        Types.Link memory link = IBMCManagement(bmcManagement).getLink(_link);
        require(link.isConnected == true, "BMCRevertNotExistsLink");
        Types.RelayStats[] memory _relays =
            IBMCManagement(bmcManagement).getRelayStatusByLink(_link);
        (string memory _net, ) = _link.splitBTPAddress();
        uint256 _height;
        uint256 _offset;
        uint256 _lastHeight;
        (_height, _offset, _lastHeight) = IBMV(
            IBMCManagement(bmcManagement).getBmvServiceByNet(_net)
        )
            .getStatus();
        uint256 _rotateTerm =
            link.maxAggregation.getRotateTerm(
                link.blockIntervalSrc.getScale(link.blockIntervalDst)
            );
        return
            Types.LinkStats(
                link.rxSeq,
                link.txSeq,
                Types.VerifierStats(_height, _offset, _lastHeight, ""),
                _relays,
                link.relayIdx,
                link.rotateHeight,
                _rotateTerm,
                link.delayLimit,
                link.maxAggregation,
                link.rxHeightSrc,
                link.rxHeight,
                link.blockIntervalSrc,
                link.blockIntervalDst,
                block.number
            );
    }
}
