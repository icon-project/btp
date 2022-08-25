// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMC.sol";
import "./interfaces/IBSH.sol";
import "./interfaces/ICallService.sol";
import "./interfaces/ICallServiceReceiver.sol";
import "./interfaces/IFixedFees.sol";
import "./libraries/Strings.sol";
import "./libraries/Integers.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/Types.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/RLPDecodeStruct.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract CallService is IBSH, ICallService, IFixedFees, Initializable {
    using Strings for string;
    using Integers for uint;
    using ParseAddress for address;
    using ParseAddress for string;
    using RLPEncodeStruct for Types.CSMessage;
    using RLPEncodeStruct for Types.CSMessageRequest;
    using RLPEncodeStruct for Types.CSMessageResponse;
    using RLPDecodeStruct for bytes;

    uint256 private constant MAX_DATA_SIZE = 2048;
    uint256 private constant MAX_ROLLBACK_SIZE = 1024;
    string private constant SERVICE = "xcall";
    address private bmc;
    string private btpAddress;
    uint256 private lastSn;
    uint256 private lastReqId;

    mapping(uint256 => Types.CallRequest) private requests;
    mapping(uint256 => Types.CSMessageRequest) private proxyReqs;

    // for fee-related operations
    uint256 private constant EXA = 1000000000000000000; //10^18
    string private constant FEE_DEFAULT = "default";
    address private owner;
    address private adminAddress;
    mapping(string => Types.FeeConfig) private feeTable;
    mapping(string => uint256) private accruedFeeTable;

    modifier onlyBMC() {
        require(msg.sender == bmc, "OnlyBMC");
        _;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "OnlyOwner");
        _;
    }

    modifier onlyAdmin() {
        require(msg.sender == this.admin(), "OnlyAdmin");
        _;
    }

    function initialize(
        address _bmc
    ) public initializer {
        owner = msg.sender;

        // set bmc address only for the first deploy
        bmc = _bmc;
        string memory bmcBtpAddress = IBMC(bmc).getBmcBtpAddress();
        (string memory _net, ) = bmcBtpAddress.splitBTPAddress();
        btpAddress = string("btp://")
            .concat(_net)
            .concat("/")
            .concat(address(this).toString());

        // set default fees to (10, 1) ICX
        if (isNullFeeConfig(feeTable[FEE_DEFAULT])) {
            feeTable[FEE_DEFAULT] = Types.FeeConfig(
                EXA * 10,
                EXA,
                true
            );
        }
    }

    function checkService(
        string calldata _svc
    ) internal pure {
        require(SERVICE.compareTo(_svc), "InvalidServiceName");
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
        Types.CallRequest memory _null;
        requests[sn] = _null;
        emit CallRequestCleared(sn);
    }

    function isNullCallRequest(
        Types.CallRequest memory req
    ) internal pure returns (bool) {
        return req.from == address(0);
    }

    function isNullCSMessageRequest(
        Types.CSMessageRequest memory req
    ) internal pure returns (bool) {
        return bytes(req.from).length == 0;
    }

    function isNullFeeConfig(
        Types.FeeConfig memory fc
    ) internal pure returns (bool) {
        return !fc.exists;
    }

    function totalFee(
        Types.FeeConfig memory _fc
    ) internal pure returns (uint256) {
        return _fc.relay + _fc.protocol;
    }

    /**
       @notice Sends a call message to the contract on the destination chain.
       @dev Only allowed to be called from the contract.
       @param _to The BTP address of the callee on the destination chain
       @param _data The calldata specific to the target contract
       @param _rollback (Optional) The data for restoring the caller state when an error occurred
       @return The serial number of the request
     */
    function sendCallMessage(
        string calldata _to,
        bytes calldata _data,
        bytes calldata _rollback
    ) external payable override returns (uint256) {
        address caller = msg.sender;
        // Note if caller is a contract in construction, will revert
        require(caller.code.length > 0, "SenderNotAContract");

        // check size of payloads to avoid abusing
        require(_data.length <= MAX_DATA_SIZE, "MaxDataSizeExceeded");
        require(_rollback.length <= MAX_ROLLBACK_SIZE, "MaxRollbackSizeExceeded");

        //TODO require BTPAddress validation
        (string memory netTo, string memory dstAccount) = _to.splitBTPAddress();
        require(bytes(dstAccount).length > 0, "invalid _to");
        Types.FeeConfig memory feeConfig = getFeeConfig(netTo);

        //TODO payable check
        require(msg.value >= totalFee(feeConfig), "InsufficientFee");

        // accumulate fees per type
        accruedFeeTable[Types.FEE_RELAY]=accruedFeeTable[Types.FEE_RELAY]+feeConfig.relay;
        accruedFeeTable[Types.FEE_PROTOCOL]=accruedFeeTable[Types.FEE_PROTOCOL]+msg.value-feeConfig.relay;

        // send message
        uint256 sn = getNextSn();
        bool rollbackEnabled = _rollback.length > 0;
        if (rollbackEnabled) {
            requests[sn] = Types.CallRequest(
                caller,
                _to,
                _rollback,
                false
            );
        }
        bytes memory payload = Types.CSMessageRequest(
            caller.toString(),
            dstAccount,
            sn,
            rollbackEnabled,
            _data
        ).encodeCSMessageRequest();
        sendBTPMessage(netTo, Types.CS_REQUEST, sn, payload);
        emit CallMessageSent(caller, _to, sn, _data);
        return sn;
    }

    /**
       @notice Executes the requested call.
       @dev Caller should be ...
       @param _reqId The request Id
     */
    function executeCall(
        uint256 _reqId
    ) external override {
        Types.CSMessageRequest memory req = proxyReqs[_reqId];
        require(!isNullCSMessageRequest(req), "InvalidRequestId");

        //TODO require BTPAddress validation
        (string memory netFrom, ) = req.from.splitBTPAddress();
        int errCode = Types.CS_RESP_SUCCESS;
        address csrAddress = req.to.parseAddress();
        string memory errMsg;
        try this.tryHandleCallMessage(
            csrAddress,
            req.from,
            req.data
        ) {
        } catch Error(string memory err) {
            errCode = Types.CS_RESP_FAILURE;
            errMsg = err;
        } catch (bytes memory err) {
            errCode = Types.CS_RESP_FAILURE;
            errMsg = string("unknownError");
//            errMsg = string("unknownError ").concat(Strings.bytesToHex(err));
        }

        // cleanup
        Types.CSMessageRequest memory _null;
        proxyReqs[_reqId] = _null;
        // send response only when there was a rollback
        if (req.rollback) {
            bytes memory payload = Types.CSMessageResponse(
                req.sn,
                errCode,
                errMsg
            ).encodeCSMessageResponse();
            uint256 sn = getNextSn();
            sendBTPMessage(netFrom, Types.CS_RESPONSE, sn, payload);
        }
    }

    //  @dev To catch for invalid address of ICallServiceReceiver
    function tryHandleCallMessage(
        address csrAddress,
        string memory from,
        bytes memory data
    ) external {
        require(msg.sender == address(this), "OnlyInternal");
        ICallServiceReceiver(csrAddress).handleCallMessage(from, data);
    }

    /**
       @notice Rollbacks the caller state of the request '_sn'.
       @dev Caller should be ...
       @param _sn The serial number of the previous request
     */
    function executeRollback(
        uint256 _sn
    ) external override {
        Types.CallRequest memory req = requests[_sn];
        require(!isNullCallRequest(req), "InvalidSerialNum");
        require(req.enabled, "RollbackNotEnabled");

        try this.tryHandleCallMessage(
            req.from,
            btpAddress,
            req.rollback
        ){
        } catch {
            //logging
        }
        cleanupCallRequest(_sn);
    }

    /* Implementation-specific eventlog */
    event CallMessageSent(
        address indexed _from,
        string indexed _to,
        uint256 indexed _sn,
        bytes _data
    );

    /* Implementation-specific eventlog */
    event CallRequestCleared(
        uint256 indexed _sn
    );

    /* ========== Interfaces with BMC ========== */
    /**
       @notice Handle BTP Message from other blockchain.
       @dev Accept the message only from the BMC.
       Every BSH must implement this function
       @param _from    Network Address of source network
       @param _svc     Name of the service
       @param _sn      Serial number of the message
       @param _msg     Serialized bytes of ServiceMessage
   */
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
            //TODO make revert message
            string memory errMsg = string("UnknownMsgType(")
                .concat(uint(csMsg.msgType).toString())
                .concat(string(")"));
            revert(errMsg);
        }
    }

    /**
       @notice Handle the error on delivering the message.
       @dev Accept the error only from the BMC.
       Every BSH must implement this function
       @param _src     BTP Address of BMC generates the error
       @param _svc     Name of the service
       @param _sn      Serial number of the original message
       @param _code    Code of the error
       @param _msg     Message of the error
   */
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
        string memory netTo,
        int msgType,
        uint256 sn,
        bytes memory msgPayload
    ) internal {
        bytes memory csMsg = Types.CSMessage(
            msgType,
            msgPayload
        ).encodeCSMessage();
        IBMC(bmc).sendMessage(
            netTo,
            SERVICE,
            sn,
            csMsg
        );
    }

    function handleRequest(
        string memory netFrom,
        uint256 sn,
        bytes memory msgPayload
    ) internal {
        Types.CSMessageRequest memory req = msgPayload.decodeCSMessageRequest();
        //TODO make btpaddress
        string memory from = string("btp://")
            .concat(netFrom)
            .concat(string("/"))
            .concat(req.from);
        uint256 reqId = getNextReqId();
        proxyReqs[reqId] = Types.CSMessageRequest(
            from,
            req.to,
            sn,
            req.rollback,
            req.data
        );
        emit CallMessage(from, req.to, sn, reqId, req.data);
    }

    function handleResponse(
        Types.CSMessageResponse memory res
    ) internal {
        Types.CallRequest memory req = requests[res.sn];
        if (!isNullCallRequest(req)) {
            if (res.code == Types.CS_RESP_SUCCESS){
                cleanupCallRequest(res.sn);
            } else {
                //emit rollback event
                require(req.rollback.length > 0, "NoRollbackData");
                req.enabled=true;
                requests[res.sn]=req;
                RollbackMessage(res.sn, req.rollback, res.msg);
            }
        }
    }

    function admin(
    ) external view returns (address) {
        if (adminAddress == address(0)) {
            return owner;
        }
        return adminAddress;
    }

    function setAdmin(
        address _address
    ) external onlyOwner {
        adminAddress = _address;
    }

    function getFeeConfig(
        string memory net
    ) internal view returns (Types.FeeConfig memory) {
        Types.FeeConfig memory feeConfig = feeTable[net];
        if (isNullFeeConfig(feeConfig)) {
            feeConfig = feeTable[FEE_DEFAULT];
        }
        return feeConfig;
    }

    /**
       @notice Gets the fixed fee for the given network address and type.
       @dev If there is no mapping to the network address, `default` fee is returned.
       @param _net The network address
       @param _type The fee type ("relay" or "protocol")
       @return The fee amount in loop
     */
    function fixedFee(
        string calldata _net,
        string calldata _type
    ) external view override returns (uint256) {
        Types.FeeConfig memory feeConfig = getFeeConfig(_net);
        string memory feeType = _type.lower();
        if (feeType.compareTo(Types.FEE_RELAY)) {
            return feeConfig.relay;
        } else if (feeType.compareTo(Types.FEE_PROTOCOL)) {
            return feeConfig.protocol;
        } else {
            return 0;
        }
    }

    /**
       @notice Gets the total fixed fees for the given network address.
       @dev If there is no mapping to the network address, `default` fee is returned.
       @param _net The network address
       @return The total fees amount in loop
     */
    function totalFixedFees(
        string calldata _net
    ) external view override returns (uint256) {
        Types.FeeConfig memory feeConfig = getFeeConfig(_net);
        return totalFee(feeConfig);
    }

    /**
       @notice Sets the fixed fees for the given network address.
       @dev Only the admin wallet can invoke this.
       @param _net The destination network address
       @param _relay The relay fee amount in loop
       @param _protocol The protocol fee amount in loop
     */
    function setFixedFees(
        string calldata _net,
        uint256 _relay,
        uint256 _protocol
    ) external override onlyAdmin {
        //TODO _net validation revert("InvalidNetworkAddress")
        feeTable[_net] = Types.FeeConfig(
            _relay,
            _protocol,
            true
        );
        FixedFeesUpdated(_net, _relay, _protocol);
    }

    /**
       @notice Gets the total accrued fees for the given type.
       @param _type The fee type ("relay" or "protocol")
       @return The total accrued fees in loop
     */
    function accruedFees(
        string calldata _type
    ) external view override returns (uint256) {
        return accruedFeeTable[_type];
    }

}
