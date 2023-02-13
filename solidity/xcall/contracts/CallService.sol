// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "./interfaces/IBMC.sol";
import "./interfaces/IBSH.sol";
import "./interfaces/ICallService.sol";
import "./interfaces/ICallServiceReceiver.sol";
import "./interfaces/IFeeManage.sol";
import "./libraries/BTPAddress.sol";
import "./libraries/Strings.sol";
import "./libraries/Integers.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/Types.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/RLPDecodeStruct.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract CallService is IBSH, ICallService, IFeeManage, Initializable {
    using Strings for string;
    using Integers for uint;
    using ParseAddress for address;
    using ParseAddress for string;
    using BTPAddress for string;
    using RLPEncodeStruct for Types.CSMessage;
    using RLPEncodeStruct for Types.CSMessageRequest;
    using RLPEncodeStruct for Types.CSMessageResponse;
    using RLPDecodeStruct for bytes;

    uint256 private constant MAX_DATA_SIZE = 2048;
    uint256 private constant MAX_ROLLBACK_SIZE = 1024;
    address private bmc;
    string private btpAddress;
    uint256 private lastSn;
    uint256 private lastReqId;
    uint256 private protocolFee;

    mapping(uint256 => Types.CallRequest) private requests;
    mapping(uint256 => Types.CSMessageRequest) private proxyReqs;

    address private owner;
    address private adminAddress;
    address payable private feeHandler;

    modifier onlyBMC() {
        require(msg.sender == bmc, "OnlyBMC");
        _;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "OnlyOwner");
        _;
    }

    modifier onlyAdmin() {
        require(msg.sender == _admin(), "OnlyAdmin");
        _;
    }

    function initialize(
        address _bmc
    ) public initializer {
        owner = msg.sender;

        // set bmc address only for the first deploy
        bmc = _bmc;
        string memory _net = IBMC(bmc).getNetworkAddress();
        btpAddress = _net.btpAddress(address(this).toString());
    }

    /* Implementation-specific external */
    function getBtpAddress(
    ) external view override returns (
        string memory
    ) {
        return btpAddress;
    }

    function checkService(
        string calldata _svc
    ) internal pure {
        require(Types.NAME.compareTo(_svc), "InvalidServiceName");
    }

    function getNextSn(
    ) internal returns (uint256) {
        lastSn = lastSn + 1;
        return lastSn;
    }

    function getNextReqId(
    ) internal returns (uint256) {
        lastReqId = lastReqId + 1;
        return lastReqId;
    }

    function cleanupCallRequest(
        uint256 sn
    ) internal {
        delete requests[sn];
        emit CallRequestCleared(sn);
    }

    function isNullCallRequest(
        Types.CallRequest memory req
    ) internal pure returns (bool) {
        return req.from == address(0);
    }

    function sendCallMessage(
        string memory _to,
        bytes memory _data,
        bytes memory _rollback
    ) external payable override returns (
        uint256
    ) {
        // check if caller is a contract or rollback data is null in case of EOA
        require(msg.sender.code.length > 0 || _rollback.length == 0, "RollbackNotPossible");

        // check size of payloads to avoid abusing
        require(_data.length <= MAX_DATA_SIZE, "MaxDataSizeExceeded");
        require(_rollback.length <= MAX_ROLLBACK_SIZE, "MaxRollbackSizeExceeded");

        bool needResponse = _rollback.length > 0;
        (string memory netTo, string memory dstAccount) = _to.parseBTPAddress();
        uint256 requiredFee = _getFee(netTo, needResponse);
        require(msg.value >= requiredFee, "InsufficientFee");

        // handle protocol fee
        if (feeHandler != address(0) && protocolFee > 0) {
            // we trust fee handler, it should just accept the protocol fee and return
            // assume that no reentrant cases occur here
            feeHandler.transfer(protocolFee);
        }

        uint256 sn = getNextSn();
        int256 msgSn = 0;
        if (needResponse) {
            requests[sn] = Types.CallRequest(msg.sender, _to, _rollback, false);
            msgSn = int256(sn);
        }
        Types.CSMessageRequest memory reqMsg = Types.CSMessageRequest(
            msg.sender.toString(), dstAccount, sn, needResponse, _data);

        int256 nsn = sendBTPMessage(msg.value - protocolFee, netTo, Types.CS_REQUEST, msgSn, reqMsg.encodeCSMessageRequest());
        emit CallMessageSent(msg.sender, _to, sn, nsn, _data);
        return sn;
    }

    function executeCall(
        uint256 _reqId
    ) external override {
        Types.CSMessageRequest memory msgReq = proxyReqs[_reqId];
        require(bytes(msgReq.from).length > 0, "InvalidRequestId");
        // cleanup
        delete proxyReqs[_reqId];

        string memory netFrom = msgReq.from.networkAddress();
        Types.CSMessageResponse memory msgRes;

        try this.tryHandleCallMessage(
            address(0),
            msgReq.to,
            msgReq.from,
            msgReq.data
        ) {
            msgRes = Types.CSMessageResponse(msgReq.sn, Types.CS_RESP_SUCCESS, "");
        } catch Error(string memory reason) {
            msgRes = Types.CSMessageResponse(msgReq.sn, Types.CS_RESP_FAILURE, reason);
        } catch (bytes memory) {
            msgRes = Types.CSMessageResponse(msgReq.sn, Types.CS_RESP_FAILURE, "unknownError");
        }
        emit CallExecuted(_reqId, msgRes.code, msgRes.msg);

        // send response only when there was a rollback
        if (msgReq.rollback) {
            sendBTPMessage(0, netFrom, Types.CS_RESPONSE, int256(msgReq.sn) * -1, msgRes.encodeCSMessageResponse());
        }
    }

    //  @dev To catch error
    function tryHandleCallMessage(
        address toAddr,
        string memory to,
        string memory from,
        bytes memory data
    ) external {
        require(msg.sender == address(this), "OnlyInternal");
        if (toAddr == address(0)) {
            toAddr = to.parseAddress("IllegalArgument");
        }
        ICallServiceReceiver(toAddr).handleCallMessage(from, data);
    }

    function executeRollback(
        uint256 _sn
    ) external override {
        Types.CallRequest memory req = requests[_sn];
        require(req.from != address(0), "InvalidSerialNum");
        require(req.enabled, "RollbackNotEnabled");
        cleanupCallRequest(_sn);

        try this.tryHandleCallMessage(
            req.from,
            "",
            btpAddress,
            req.rollback
        ){
        } catch {
            //ignore failure
        }
    }

    /* Implementation-specific eventlog */
    event CallMessageSent(
        address indexed _from,
        string indexed _to,
        uint256 indexed _sn,
        int256 _nsn,
        bytes _data
    );

    /* Implementation-specific eventlog */
    event CallRequestCleared(
        uint256 indexed _sn
    );

    /* ========== Interfaces with BMC ========== */
    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override onlyBMC {
        checkService(_svc);

        Types.CSMessage memory csMsg = _msg.decodeCSMessage();
        if (csMsg.msgType == Types.CS_REQUEST) {
            handleRequest(_from, _sn, csMsg.payload);
        } else if (csMsg.msgType == Types.CS_RESPONSE) {
            handleResponse(csMsg.payload.decodeCSMessageResponse());
        } else {
            string memory errMsg = string("UnknownMsgType(")
                .concat(uint(csMsg.msgType).toString())
                .concat(string(")"));
            revert(errMsg);
        }
    }

    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override onlyBMC {
        checkService(_svc);
        string memory errMsg = string("BTPError{code=")
            .concat(uint(_code).toString())
            .concat(string(", msg="))
            .concat(_msg)
            .concat(string("}"));
        handleResponse(Types.CSMessageResponse(
            _sn,
            Types.CS_RESP_BTP_ERROR,
            errMsg
        ));
    }
    /* ========================================= */

    function sendBTPMessage(
        uint256 value,
        string memory netTo,
        int msgType,
        int256 sn,
        bytes memory msgPayload
    ) internal returns (
        int256
    ){
        return IBMC(bmc).sendMessage{value: value}(
            netTo,
            Types.NAME,
            sn,
            Types.CSMessage(
                msgType,
                msgPayload
            ).encodeCSMessage()
        );
    }

    function handleRequest(
        string memory netFrom,
        uint256 sn,
        bytes memory msgPayload
    ) internal {
        Types.CSMessageRequest memory req = msgPayload.decodeCSMessageRequest();
        string memory from = netFrom.btpAddress(req.from);

        uint256 reqId = getNextReqId();
        proxyReqs[reqId] = Types.CSMessageRequest(
            from,
            req.to,
            req.sn,
            req.rollback,
            req.data
        );
        emit CallMessage(from, req.to, req.sn, reqId, req.data);
    }

    function handleResponse(
        Types.CSMessageResponse memory res
    ) internal {
        Types.CallRequest memory req = requests[res.sn];
        if (req.from != address(0)) {
            if (res.code == Types.CS_RESP_SUCCESS){
                cleanupCallRequest(res.sn);
            } else {
                //emit rollback event
                require(req.rollback.length > 0, "NoRollbackData");
                req.enabled=true;
                requests[res.sn]=req;
                emit RollbackMessage(res.sn, req.rollback, res.msg);
            }
        }
    }

    function _admin(
    ) internal view returns (
        address
    ) {
        if (adminAddress == address(0)) {
            return owner;
        }
        return adminAddress;
    }

    /**
       @notice Gets the address of admin
       @return (Address) the address of admin
    */
    function admin(
    ) external view returns (
        address
    ) {
        return _admin();
    }

    /**
       @notice Sets the address of admin
       @dev Only the owner wallet can invoke this.
       @param _address (Address) The address of admin
    */
    function setAdmin(
        address _address
    ) external onlyOwner {
        adminAddress = _address;
    }

    function setProtocolFeeHandler(
        address _addr
    ) external override onlyAdmin {
        feeHandler = payable(_addr);
        if (feeHandler != address(0)) {
            uint256 accruedFees = address(this).balance;
            if (accruedFees > 0) {
                feeHandler.transfer(accruedFees);
            }
        }
    }

    function getProtocolFeeHandler(
    ) external view override returns (
        address
    ) {
        return feeHandler;
    }

    function setProtocolFee(
        uint256 _value
    ) external override onlyAdmin {
        require(_value >= 0, "ValueShouldBePositive");
        protocolFee = _value;
    }

    function getProtocolFee(
    ) external view override returns (
        uint256
    ) {
        return protocolFee;
    }

    function _getFee(
        string memory _net,
        bool _rollback
    ) internal view returns (
        uint256
    ) {
        return protocolFee + IBMC(bmc).getFee(_net, _rollback);
    }

    function getFee(
        string memory _net,
        bool _rollback
    ) external view override returns (
        uint256
    ) {
        return _getFee(_net, _rollback);
    }
}
