// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBSH.sol";
import "./interfaces/IBMV.sol";
import "./interfaces/IBMCPeriphery.sol";
import "./interfaces/IBMCManagement.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/RLPDecodeStruct.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/Strings.sol";
import "./libraries/Types.sol";
import "./libraries/Utils.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BMCPeriphery is IBMCPeriphery, Initializable {
    using Strings for string;
    using ParseAddress for address;
    using RLPDecodeStruct for bytes;
    using RLPEncodeStruct for Types.BTPMessage;
    using RLPEncodeStruct for Types.ErrorMessage;
    using Utils for uint256[];

    uint256 internal constant UNKNOWN_ERR = 0;
    uint256 internal constant BMC_ERR = 10;
    uint256 internal constant BMC_ERR_CODE_DROP = 22;
    string internal constant BMC_ERR_NAME_DROP = "Drop";
    uint256 internal constant BMV_ERR = 25;
    uint256 internal constant BSH_ERR = 40;

    string internal constant BMC_INTERNAL_SERVICE = "bmc";
    string internal constant BMCRevertUnauthorized = "11:Unauthorized";
    string internal constant BMCRevertParseFailure = "ParseFailure";
    string internal constant BMCRevertNotExistsBSH = "16:NotExistsBSH";
    string internal constant BMCRevertNotExistsLink = "18:NotExistsLink";
    string internal constant BMCRevertInvalidSn = "12:InvalidSn";
    string internal constant BMCRevertNotExistsInternalHandler =
        "NotExistsInternalHandler";
    string internal constant BMCRevertUnknownHandleBTPError =
        "UnknownHandleBTPError";
    string internal constant BMCRevertUnknownHandleBTPMessage =
        "UnknownHandleBTPMessage";

    string private bmcBtpAddress; // a network address, i.e. btp://1234.pra/0xabcd
    address private bmcManagement;

    uint256[] private drops;

    modifier onlyBMCManagement() {
        require(msg.sender == bmcManagement, BMCRevertUnauthorized);
        _;
    }

    function initialize(string memory _network, address _bmcManagementAddr)
        public
        initializer
    {
        bmcBtpAddress = string("btp://").concat(_network).concat("/").concat(
            address(this).toString()
        );
        bmcManagement = _bmcManagementAddr;
    }

    /**
        @param _next next BMC's BTP address
        @param _seq a sequence number to keep track of BTP messages
        @param _msg message from BSH
    */
    event Message(string _next, uint256 _seq, bytes _msg);

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
    function handleRelayMessage(string calldata _prev, bytes calldata _msg)
        external
        override
    {
        (string memory prevNet, ) = _prev.splitBTPAddress();
        address _bmvAddr = IBMCManagement(bmcManagement).getBmvServiceByNet(
            prevNet
        );

        require(_bmvAddr != address(0), "NotExistsBMV");
        (uint256 _prevHeight,) = IBMV(_bmvAddr).getStatus();

        uint256 rxSeq = IBMCManagement(bmcManagement).getLink(_prev).rxSeq;
        // decode and verify relay message
        bytes[] memory serializedMsgs = IBMV(_bmvAddr).handleRelayMessage(
            bmcBtpAddress, _prev, rxSeq, _msg);

        require(IBMCManagement(bmcManagement).isLinkRelay(_prev, msg.sender), BMCRevertUnauthorized);

        (uint256 _height,) = IBMV(_bmvAddr).getStatus();
        IBMCManagement(bmcManagement).updateRelayStats(
            _prev,
            msg.sender,
            _height - _prevHeight,
            serializedMsgs.length
        );

        bool dropped = false;
        drops = IBMCManagement(bmcManagement).getScheduledDropMessages(_prev);

        // dispatch BTP Messages
        Types.BTPMessage memory btpMsg;
        for (uint256 i = 0; i < serializedMsgs.length; i++) {
            try this.tryDecodeBTPMessage(serializedMsgs[i]) returns (
                Types.BTPMessage memory _decoded
            ) {
                btpMsg = _decoded;
            } catch {
                // ignore BTPMessage parse failure
                continue;
            }
            rxSeq++;
            if (drops.length > 0 && drops.removeFromUints(rxSeq)) {
                if (btpMsg.sn > 0) {
                    _sendError(_prev, prevNet, btpMsg, BMC_ERR_CODE_DROP, BMC_ERR_NAME_DROP);
                }
                emit MessageDropped(_prev, rxSeq, serializedMsgs[i]);
                dropped = true;
            } else {
                if (btpMsg.dst.compareTo(bmcBtpAddress)) {
                    if (btpMsg.svc.compareTo(BMC_INTERNAL_SERVICE)) {
                        handleInternal(_prev, prevNet, btpMsg);
                    } else {
                        handleService(_prev, prevNet, btpMsg);
                    }
                } else {
                    (string memory dstNet, ) = btpMsg.dst.splitBTPAddress();
                    try IBMCManagement(bmcManagement).resolveRoute(dstNet) returns (
                        string memory nextLink,
                        string memory
                    ) {
                        (string memory nextNet, ) = nextLink.splitBTPAddress();
                        _sendMessage(nextLink, nextNet, serializedMsgs[i]);
                    } catch Error(string memory _error) {
                        _sendError(_prev, prevNet, btpMsg, BMC_ERR, _error);
                    }
                }
            }
        }
        if (dropped) {
            IBMCManagement(bmcManagement).setScheduledDropMessages(_prev, drops);
        }
        IBMCManagement(bmcManagement).updateLinkRxSeq(
            prevNet,
            serializedMsgs.length
        );

    }

    function handleInternal(string calldata _prev, string memory prevNet, Types.BTPMessage memory _msg)
    internal
    {
        Types.BMCService memory _sm;
        try this.tryDecodeBMCService(_msg.message) returns (
            Types.BMCService memory res
        ) {
            _sm = res;
        } catch {
            _sendError(_prev, prevNet, _msg, BMC_ERR, BMCRevertParseFailure);
            return;
        }

        if (_sm.serviceType.compareTo("Link")) {
            IBMCManagement(bmcManagement).updateLinkReachable(
                prevNet, _sm.payload.decodePropagateMessage(), false
            );
        } else if (_sm.serviceType.compareTo("Unlink")) {
            IBMCManagement(bmcManagement).updateLinkReachable(
                prevNet, _sm.payload.decodePropagateMessage(), true
            );
        } else if (_sm.serviceType.compareTo("Init")) {
            IBMCManagement(bmcManagement).setLinkReachable(
                prevNet, _sm.payload.decodeInitMessage()
            );
        } else if (_sm.serviceType.compareTo("Sack")) {
            // skip this case since it has been removed from internal services

        } else revert(BMCRevertNotExistsInternalHandler);
    }

    function handleService(string calldata _prev, string memory prevNet, Types.BTPMessage memory _msg)
        internal
    {
        address _bshAddr = IBMCManagement(bmcManagement).getBshServiceByName(
            _msg.svc
        );
        if (_bshAddr == address(0)) {
            _sendError(_prev, prevNet, _msg, BMC_ERR, BMCRevertNotExistsBSH);
            return;
        }

        if (_msg.sn >= 0) {
            (string memory _net, ) = _msg.src.splitBTPAddress();
            try IBSH(_bshAddr).handleBTPMessage(
                _net,
                _msg.svc,
                uint256(_msg.sn),
                _msg.message
            ){
            } catch Error(string memory reason) {
                _sendError(_prev, prevNet, _msg, BSH_ERR, reason);
            } catch (bytes memory) {
                _sendError(_prev, prevNet, _msg, BSH_ERR, BMCRevertUnknownHandleBTPMessage);
            }
        } else {
            Types.ErrorMessage memory _res = _msg.message.decodeErrorMessage();
            uint256 _errCode;
            bytes memory _errMsg;
            try IBSH(_bshAddr).handleBTPError(
                _msg.src,
                _msg.svc,
                uint256(_msg.sn * -1),
                _res.code,
                _res.message
            ){
            } catch Error(string memory reason) {
                _errCode = BSH_ERR;
                _errMsg = bytes(reason);
            } catch (bytes memory) {
                _errCode = UNKNOWN_ERR;
                _errMsg = bytes(BMCRevertUnknownHandleBTPError);
            }
            if (_errMsg.length > 0) {
                emit ErrorOnBTPError(
                    _msg.svc,
                    _msg.sn * -1,
                    _res.code,
                    _res.message,
                    _errCode,
                    string(_errMsg)
                );
            }
        }
    }

    //  @dev Despite this function was set as external, it should be called internally
    //  since Solidity does not allow using try_catch with internal function
    //  this solution can solve the issue
    function tryDecodeBTPMessage(bytes memory _rlp)
        external
        pure
        returns (Types.BTPMessage memory)
    {
        return _rlp.decodeBTPMessage();
    }

    //  @dev Solidity does not allow using try_catch with internal function
    //  Thus, work-around solution is the followings
    //  If there is any error throwing, BMC contract can catch it, then reply back a RC_ERR ErrorMessage
    function tryDecodeBMCService(bytes calldata _msg)
        external
        pure
        returns (Types.BMCService memory)
    {
        return _msg.decodeBMCService();
    }

    function _sendMessage(string memory next, string memory net, bytes memory _serializedMsg)
        internal
    {
        emit Message(
            next,
            IBMCManagement(bmcManagement).updateLinkTxSeq(net),
            _serializedMsg
        );
    }

    function _sendError(
        string calldata _prev,
        string memory prevNet,
        Types.BTPMessage memory _message,
        uint256 _errCode,
        string memory _errMsg
    ) internal {
        if (_message.sn > 0) {
            bytes memory _serializedMsg = Types.BTPMessage(
                    bmcBtpAddress,
                    _message.src,
                    _message.svc,
                    _message.sn * -1,
                    Types.ErrorMessage(_errCode, _errMsg).encodeErrorMessage()
                )
                .encodeBTPMessage();
            _sendMessage(_prev, prevNet, _serializedMsg);
        }
    }

    /**
       @notice Send the message to a specific network.
       @dev Caller must be an registered BSH.
       @param _to      Network Address of destination network
       @param _svc     Name of the service
       @param _sn      Serial number of the message, it should be positive
       @param _msg     Serialized bytes of Service Message
    */
    function sendMessage(
        string memory _to,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external override {
        address bshAddress = IBMCManagement(bmcManagement).getBshServiceByName(_svc);
        require(bshAddress != address(0), BMCRevertNotExistsBSH);
        require(bshAddress == msg.sender, BMCRevertUnauthorized);
        require(_sn >= 0, BMCRevertInvalidSn);

        (string memory next, string memory dst) = IBMCManagement(
            bmcManagement
        ).resolveRoute(_to);
        bytes memory _rlp = Types.BTPMessage(bmcBtpAddress, dst, _svc, int256(_sn), _msg)
            .encodeBTPMessage();
        (string memory nextNet, ) = next.splitBTPAddress();
        _sendMessage(next, nextNet, _rlp);
    }

    function sendInternal(
        string memory _next,
        uint256 _seq,
        bytes memory _msg
    ) external override onlyBMCManagement {
        bytes memory _rlp = Types.BTPMessage(bmcBtpAddress, _next, BMC_INTERNAL_SERVICE, 0, _msg)
            .encodeBTPMessage();
        emit Message(_next, _seq, _rlp);
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
        require(link.isConnected == true, BMCRevertNotExistsLink);
        (string memory net, ) = _link.splitBTPAddress();
        (uint256 height, bytes memory extra) = IBMV(
            IBMCManagement(bmcManagement).getBmvServiceByNet(net)
        ).getStatus();
        return
            Types.LinkStats(
                link.rxSeq,
                link.txSeq,
                Types.VerifierStats(height, extra),
                block.number
            );
    }

    /**
        @notice (EventLog) Drop the message of the connected BMC
        @param _link String ( BTP Address of connected BMC )
        @param _seq  Integer ( sequence number of the message from connected BMC )
        @param _msg  Bytes ( serialized bytes of BTP Message )
    */
    event MessageDropped(string _link, uint256 _seq, bytes _msg);

    /**
        @notice Drop the message of the connected BMC
        @dev Caller must be an BMCManagement.
        @param _src  String ( BTP Address of source BMC to drop )
        @param _link String ( BTP Address of previous BMC to drop )
        @param _seq  Integer ( Sequence number to drop )
        @param _svc  String ( Name of the service to drop )
        @param _sn   Integer ( Serial number of the message to drop )
        @param _txSeq  Integer ( Sequence number of the error message to send to source )
    */
    function dropMessage(
        string memory _src,
        string memory _link,
        uint256 _seq,
        string memory _svc,
        uint256 _sn,
        uint256 _txSeq
    ) external override onlyBMCManagement {
        //instead _sendError
        emit Message(_link, _seq,
            Types.BTPMessage(
                bmcBtpAddress,
                _src,
                _svc,
                int256(_sn) * -1,
                Types.ErrorMessage(BMC_ERR_CODE_DROP, BMC_ERR_NAME_DROP).encodeErrorMessage())
            .encodeBTPMessage());
        emit MessageDropped(_link, _txSeq,
            Types.BTPMessage(_src, "", _svc, int256(_sn), bytes("")).encodeBTPMessage());
    }
}
