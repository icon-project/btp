// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../interfaces/IBMCPeriphery.sol";
import "../interfaces/ICCPeriphery.sol";
import "../interfaces/IBMCManagement.sol";
import "../interfaces/IBSH.sol";
import "../interfaces/IBMV.sol";
import "../interfaces/ICCManagement.sol";
import "../interfaces/ICCService.sol";
import "../libraries/Types.sol";
import "../libraries/Errors.sol";
import "../libraries/BTPAddress.sol";
import "../libraries/ParseAddress.sol";
import "../libraries/Strings.sol";
import "../libraries/RLPEncodeStruct.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BMCPeripheryV2 is IBMCPeriphery, ICCPeriphery, Initializable {
    using BTPAddress for string;
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;
    using RLPEncodeStruct for Types.BTPMessage;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ResponseMessage;
    using RLPEncodeStruct for Types.ClaimMessage;

    string private btpAddress;
    string private network;
    address private bmcManagement;
    address private bmcService;

    mapping(string => uint256) private txSeqMap;//net of link = > txSeq
    mapping(string => uint256) private rxSeqMap;//net of link = > rxSeq
    int256 private networkSn;

    function initialize(
        string memory _network,
        address _bmcManagementAddr,
        address _bmcServiceAddr
    ) public initializer {
        network = _network;
        btpAddress = _network.btpAddress(address(this).toString());
        bmcManagement = _bmcManagementAddr;
        bmcService = _bmcServiceAddr;
    }

    function requireBMCManagementAccess(
    ) internal view {
        require(msg.sender == bmcManagement, Errors.BMC_REVERT_UNAUTHORIZED);
    }

    function requireBMCServiceAccess(
    ) internal view {
        require(msg.sender == bmcService, Errors.BMC_REVERT_UNAUTHORIZED);
    }

    //FIXME remove getBtpAddress
    function getBtpAddress(
    ) external view override returns (
        string memory
    ) {
        return btpAddress;
    }

    function getNetworkAddress(
    ) external view override returns (
        string memory
    ) {
        return network;
    }

    function handleRelayMessage(
        string calldata _prev,
        bytes calldata _msg
    ) external override {
        string memory prevNet = _prev.networkAddress();
        uint256 rxSeq = rxSeqMap[_prev];

        // decode and verify relay message
        bytes[] memory serializedMsgs = IBMV(
            ICCManagement(bmcManagement).getVerifier(prevNet)
        ).handleRelayMessage(btpAddress, _prev, rxSeq, _msg);
        require(ICCManagement(bmcManagement).isLinkRelay(_prev, msg.sender), Errors.BMC_REVERT_UNAUTHORIZED);
        // dispatch BTP Messages
        Types.BTPMessage memory btpMsg;
        for (uint256 i = 0; i < serializedMsgs.length; i++) {
            rxSeq++;
            btpMsg = ICCService(bmcService).handleFee(
                msg.sender, serializedMsgs[i]);
            if (btpMsg.dst.compareTo(network)) {
                (uint256 ecode, string memory emsg) = handleMessage(_prev, btpMsg);
                if (ecode == Types.ECODE_NONE) {
                    emitBTPEvent(btpMsg, "", Types.BTP_EVENT_RECEIVE);
                } else {
                    //rollback
                    if (btpMsg.sn > 0) {
                        _removeResponse(btpMsg.src, btpMsg.svc, btpMsg.sn);
                    }
                    _sendError(_prev, rxSeq, btpMsg, ecode, emsg);
                }
            } else {
                try ICCManagement(bmcManagement).resolveNext(btpMsg.dst) returns (
                    string memory next
                ) {
                    _sendMessage(next, btpMsg, Types.BTP_EVENT_ROUTE);
                } catch Error(string memory reason) {
                    _sendError(_prev, rxSeq, btpMsg, Types.ECODE_NO_ROUTE, reason);
                }
            }
        }
        rxSeqMap[_prev] += serializedMsgs.length;
    }

    function handleMessage(
        string memory prev,
        Types.BTPMessage memory _msg
    ) internal returns (
        uint256,
        string memory
    ) {
        int256 sn;
        address _bshAddr;
        uint256 ecode = Types.ECODE_BSH_REVERT;
        if (_msg.svc.compareTo(Types.BMC_SERVICE)) {
            _bshAddr = bmcService;
            if (_msg.sn >= 0 && _msg.nsn < 0) {
                sn = _msg.nsn * -1;
            } else {
                sn = _msg.nsn;
            }
            ecode = Types.ECODE_UNKNOWN;
        } else {
            try ICCManagement(bmcManagement).getService(_msg.svc) returns (
                address addr
            ){
                _bshAddr = addr;
            } catch Error(string memory reason) {
                return (Types.ECODE_NO_BSH, reason);
            }
            sn = _msg.sn;
        }

        string memory emsg;
        if (_msg.sn >= 0) {
            try IBSH(_bshAddr).handleBTPMessage(
                _msg.src,
                _msg.svc,
                uint256(sn),
                _msg.message
            ){
                ecode = Types.ECODE_NONE;
            } catch Error(string memory reason) {
                emsg = reason;
            } catch (bytes memory) {
            }
        } else {
            try ICCService(bmcService).decodeResponseMessage(_msg.message) returns (
                Types.ResponseMessage memory respMsg
            ) {
                try IBSH(_bshAddr).handleBTPError(
                    _msg.src,
                    _msg.svc,
                    uint256(sn * - 1),
                    respMsg.code,
                    respMsg.message
                ){
                    ecode = Types.ECODE_NONE;
                } catch Error(string memory reason) {
                    emsg = reason;
                } catch (bytes memory) {
                }
            } catch {
                ecode = Types.ECODE_UNKNOWN;
                emsg = Errors.BMC_REVERT_PARSE_FAILURE;
            }
        }
        return (ecode, emsg);
    }

    function _sendMessage(
        string memory next,
        Types.BTPMessage memory btpMsg,
        string memory evt
    ) internal {
        txSeqMap[next]++;
        emit Message(next, txSeqMap[next], btpMsg.encodeBTPMessage());
        emitBTPEvent(btpMsg, next, evt);
    }

    function _sendError(
        string calldata _prev,
        uint256 _seq,
        Types.BTPMessage memory btpMsg,
        uint256 _errCode,
        string memory _errMsg
    ) internal {
        btpMsg.feeInfo = ICCService(bmcService).handleErrorFee(btpMsg.src, btpMsg.sn, btpMsg.feeInfo);
        if (btpMsg.sn > 0) {
            _sendMessage(_prev, Types.BTPMessage(
                    network,
                    btpMsg.src,
                    btpMsg.svc,
                    btpMsg.sn * - 1,
                    Types.ResponseMessage(_errCode, _errMsg).encodeResponseMessage(),
                    btpMsg.nsn * - 1,
                    btpMsg.feeInfo
                ), Types.BTP_EVENT_ERROR);
        } else {
            emitBTPEvent(btpMsg, "", Types.BTP_EVENT_DROP);
            emit MessageDropped(
                _prev,
                _seq,
                btpMsg.encodeBTPMessage(),
                _errCode,
                _errMsg
            );
        }
    }

    function sendMessage(
        string memory _to,
        string memory _svc,
        int256 _sn,
        bytes memory _msg
    ) external override payable returns (
        int256
    ) {
        if (msg.sender != bmcService) {
            require(ICCManagement(bmcManagement).getService(_svc) == msg.sender, Errors.BMC_REVERT_UNAUTHORIZED);
        }

        bool isResponse = false;
        if (_sn < 0) {
            _sn = _sn * - 1;
            isResponse = true;
        }
        //FIXME only for upgrade test
        revert("Upgrade successfully");
        return sendMessageWithFee(_to, _svc, _sn, _msg, isResponse, false);
    }

    function _collectOverFee(
        string memory net,
        uint256 amount
    ) internal {
        if (amount > 0) {
            ICCService(bmcService).addReward(net, address(this), amount);
        }
    }

    function nextNetworkSn(
    ) internal returns (
        int256
    ) {
        return ++networkSn;
    }

    function getNetworkSn(
    ) external view override returns (
        int256
    ) {
        return networkSn;
    }

    function sendMessageWithFee(
        string memory _to,
        string memory _svc,
        int256 _sn,
        bytes memory _msg,
        bool isResponse,
        bool fillSnByNsn
    ) internal returns (
        int256
    ) {
        string memory next = ICCManagement(bmcManagement).resolveNext(_to);
        Types.BTPMessage memory btpMsg = Types.BTPMessage(
            network,
            _to,
            _svc,
            _sn,
            _msg,
            0,
            Types.FeeInfo(network, new uint256[](0))
        );
        string memory evt;
        if (isResponse) {
            Types.Response memory response = _removeResponse(_to, _svc, _sn);
            _collectOverFee(network, msg.value);
            btpMsg.sn = 0;
            btpMsg.nsn = response.nsn * - 1;
            btpMsg.feeInfo = response.feeInfo;
            evt = Types.BTP_EVENT_REPLY;
        } else {
            btpMsg.nsn = nextNetworkSn();
            if (fillSnByNsn) {
                btpMsg.sn = btpMsg.nsn;
            }
            if (msg.sender != bmcService) {
                (uint256 sum, uint256[] memory values) = ICCManagement(bmcManagement).getFee(_to, _sn > 0);
                btpMsg.feeInfo.values = values;
                require(msg.value >= sum, Errors.BMC_REVERT_NOT_ENOUGH_FEE);
                _collectOverFee(network, msg.value - sum);
            }
            evt = Types.BTP_EVENT_SEND;
        }
        _sendMessage(next, btpMsg, evt);
        return btpMsg.nsn;
    }

    function getFee(
        string calldata _to,
        bool _response
    ) external view override returns (
        uint256
    ) {
        (uint256 fee,) = ICCManagement(bmcManagement).getFee(_to, _response);
        return fee;
    }

    function emitBTPEvent(
        Types.BTPMessage memory _msg, //CANNOT_REDUCE
        string memory next,
        string memory evt
    ) internal {
        if (_msg.nsn < 0) {
            emit BTPEvent(_msg.dst, _msg.nsn * - 1, next, evt);
        } else {
            emit BTPEvent(_msg.src, _msg.nsn, next, evt);
        }
    }

    function getStatus(
        string calldata _link
    ) external view override returns (
        Types.LinkStatus memory
    ) {
        (uint256 height, bytes memory extra) = IBMV(
            ICCManagement(bmcManagement).getVerifier(_link.networkAddress())
        ).getStatus();
        return Types.LinkStatus(
            rxSeqMap[_link],
            txSeqMap[_link],
            Types.VerifierStats(height, extra),
            block.number
        );
    }

    function sendInternal(
        string memory _next,
        bytes memory _msg
    ) external override {
        requireBMCManagementAccess();
        _sendMessage(_next,
            Types.BTPMessage(
                btpAddress,
                _next.networkAddress(),
                Types.BMC_SERVICE,
                0,
                _msg,
                nextNetworkSn(),
                Types.FeeInfo(network, new uint256[](0))
            ),
            Types.BTP_EVENT_SEND);
    }

    function dropMessage(
        string calldata _prev,
        uint256 _seq,
        Types.BTPMessage memory _msg
    ) external override {
        requireBMCManagementAccess();
        require(_seq == (rxSeqMap[_prev] + 1), Errors.BMC_REVERT_INVALID_SEQ);
        rxSeqMap[_prev]++;

        _sendError(
            _prev,
            _seq,
            _msg,
            Types.ECODE_UNKNOWN,
            Errors.BMC_REVERT_DROP
        );
    }

    function clearSeq(
        string memory _link
    ) external override {
        requireBMCManagementAccess();
        delete rxSeqMap[_link];
        delete txSeqMap[_link];
    }

    function getReward(
        string calldata _network,
        address _addr
    ) external view override returns (
        uint256
    ) {
        return ICCService(bmcService).getReward(_network, _addr);
    }

    function emitClaimRewardResult(
        address _sender,
        string memory _network,
        int256 _nsn,
        uint256 _result
    ) external override {
        requireBMCServiceAccess();
        emit ClaimRewardResult(_sender, _network, _nsn, _result);
    }

    receive() external payable {}

    function claimReward(
        string calldata _network,
        string calldata _receiver
    ) external payable override {
        address sender = msg.sender;
        if (sender == IBMCManagement(bmcManagement).getFeeHandler()) {
            sender = address(this);
        }
        uint256 reward = ICCService(bmcService).clearReward(_network, sender);
        int256 nsn = 0;
        if (network.compareTo(_network)) {
            payable(_receiver.parseAddress(Errors.BMC_REVERT_INVALID_ARGUMENT)).transfer(reward);
        } else {
            nsn = sendMessageWithFee(
                _network,
                Types.BMC_SERVICE,
                0,
                Types.BMCMessage(
                    Types.BMC_INTERNAL_CLAIM,
                    Types.ClaimMessage(
                        reward,
                        _receiver
                    ).encodeClaimMessage()
                ).encodeBMCMessage(),
                false,
                true);
            ICCService(bmcService).addRequest(nsn, _network, sender, reward);
        }
        emit ClaimReward(sender, _network, _receiver, reward, nsn);
    }

    function _removeResponse(
        string memory _to,
        string memory _svc,
        int256 _sn
    ) internal returns (Types.Response memory) {
        return ICCService(bmcService).removeResponse(_to, _svc, _sn);
    }

}
