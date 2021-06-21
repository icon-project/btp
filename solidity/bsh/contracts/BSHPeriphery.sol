// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "./Interfaces/IBSHPeriphery.sol";
import "./Interfaces/IBSHCore.sol";
import "./Interfaces/IBMCPeriphery.sol";
import "./Libraries/TypesLib.sol";
import "./Libraries/RLPEncodeStructLib.sol";
import "./Libraries/RLPDecodeStructLib.sol";
import "./Libraries/ParseAddressLib.sol";
import "./Libraries/StringsLib.sol";
import "@openzeppelin/contracts-upgradeable/math/SafeMathUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";

/**
   @title BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
   @dev OwnerUpgradeable has been removed. This contract does not have its own Owners
        Instead, BSHCore manages ownership roles.
        Thus, BSHPeriphery should call bshCore.isOwner() and pass an address for verification
        in case of implementing restrictions, if needed, in the future. 
*/
contract BSHPeriphery is Initializable, IBSHPeriphery {
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.Response;
    using RLPDecodeStruct for bytes;
    using SafeMathUpgradeable for uint256;
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    /**   @notice Sends a receipt to user
        The `_from` sender
        The `_to` receiver.
        The `_sn` sequence number of service message.
        The `_assetDetails` a list of `_coinName` and `_value`  
    */
    event TransferStart(
        address indexed _from,
        string _to,
        uint256 _sn,
        Types.AssetTransferDetail[] _assetDetails
    );

    /**   @notice Sends a final notification to a user
        The `_from` sender
        The `_sn` sequence number of service message.
        The `_code` response code, i.e. RC_OK = 0, RC_ERR = 1
        The `_response` message of response if error  
    */
    event TransferEnd(
        address indexed _from,
        uint256 _sn,
        uint256 _code,
        string _response
    );

    /**   @notice Notify that BSH contract has received unknown response
        The `_from` sender
        The `_sn` sequence number of service message
    */
    event UnknownResponse(string _from, uint256 _sn);

    IBMCPeriphery private bmc;
    IBSHCore internal bshCore;
    mapping(uint256 => Types.PendingTransferCoin) internal requests; // a list of transferring requests
    string public serviceName; //    BSH Service Name

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;
    uint256 private serialNo; //  a counter of sequence number of service message
    uint256 private numOfPendingRequests;

    modifier onlyBMC {
        require(msg.sender == address(bmc), "Unauthorized");
        _;
    }

    function initialize(
        address _bmc,
        address _bshCore,
        string memory _serviceName
    ) public initializer {
        bmc = IBMCPeriphery(_bmc);
        bmc.requestAddService(_serviceName, address(this));
        bshCore = IBSHCore(_bshCore);
        serviceName = _serviceName;
    }

    /**
     @notice Check whether BSHPeriphery has any pending transferring requests
     @return true or false
    */
    function hasPendingRequest() external override view returns (bool) {
        return numOfPendingRequests != 0;
    }

    function sendServiceMessage(
        address _from,
        string memory _to,
        string[] memory _coinNames,
        uint256[] memory _values,
        uint256[] memory _fees
    ) external override {
        //  Send Service Message to BMC
        //  If '_to' address is an invalid BTP Address format
        //  VM throws an error and revert(). Thus, it does not need
        //  a try_catch at this point
        (string memory _toNetwork, string memory _toAddress) =
            _to.splitBTPAddress();
        Types.Asset[] memory _assets = new Types.Asset[](_coinNames.length);
        Types.AssetTransferDetail[] memory _assetDetails =
            new Types.AssetTransferDetail[](_coinNames.length);
        for (uint256 i = 0; i < _coinNames.length; i++) {
            _assets[i] = Types.Asset(_coinNames[i], _values[i]);
            _assetDetails[0] = Types.AssetTransferDetail(
                _coinNames[i],
                _values[i],
                _fees[i]
            );
        }
        //  Because `stack is too deep`, must create `_strFrom` to waive this error
        //  `_strFrom` is a string type of an address `_from`
        string memory _strFrom = _from.toString();
        bmc.sendMessage(
            _toNetwork,
            serviceName,
            serialNo,
            Types.ServiceMessage(
                Types.ServiceType.REQUEST_COIN_TRANSFER,
                Types.TransferCoin(_strFrom, _toAddress, _assets).encodeTransferCoinMsg()
            ).encodeServiceMessage()
        );
        //  Push pending tx into Record list
        requests[serialNo] = Types.PendingTransferCoin(
            _strFrom,
            _to,
            _coinNames,
            _values,
            _fees
        );
        numOfPendingRequests++;
        emit TransferStart(_from, _to, serialNo, _assetDetails);
        serialNo++;
    }

    /**
     @notice BSH handle BTP Message from BMC contract
     @dev Caller must be BMC contract only
     @param _from    An originated network address of a request
     @param _svc     A service name of BSH contract     
     @param _sn      A serial number of a service request 
     @param _msg     An RLP message of a service request/service response
    */
    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override onlyBMC {
        require(_svc.compareTo(serviceName) == true, "InvalidSvc");
        Types.ServiceMessage memory _sm = _msg.decodeServiceMessage();
        string memory errMsg;

        if (_sm.serviceType == Types.ServiceType.REQUEST_COIN_TRANSFER) {
            Types.TransferCoin memory _tc = _sm.data.decodeTransferCoinMsg();
            //  checking receiving address whether is a valid address
            //  revert() if not a valid one
            try this.checkParseAddress(_tc.to) {
                try this.handleRequestService(_tc.to, _tc.assets) {
                    sendResponseMessage(
                        Types.ServiceType.REPONSE_HANDLE_SERVICE,
                        _from,
                        _sn,
                        "",
                        RC_OK
                    );
                    return;
                } catch Error(string memory _err) {
                    errMsg = _err;
                }
            } catch {
                errMsg = "InvalidAddress";
            }
            sendResponseMessage(
                Types.ServiceType.REPONSE_HANDLE_SERVICE,
                _from,
                _sn,
                errMsg,
                RC_ERR
            );
        } else if (
            _sm.serviceType == Types.ServiceType.REPONSE_HANDLE_SERVICE
        ) {
            //  Check whether '_sn' is pending state
            require(bytes(requests[_sn].from).length != 0, "InvalidSN");
            Types.Response memory response = _sm.data.decodeResponse();
            //  @dev Not implement try_catch at this point
            //  + If RESPONSE_REQUEST_SERVICE:
            //      If RC_ERR, BSHCore proceeds a refund. If a refund is failed, BSHCore issues refundable Balance
            //      If RC_OK:
            //      - requested coin = native -> update aggregation fee (likely no issue)
            //      - requested coin = wrapped coin -> BSHCore calls itself to burn its tokens and update aggregation fee (likely no issue)
            //  The only issue, which might happen, is BSHCore's token balance lower than burning amount
            //  If so, there might be something went wrong before
            //  + If RESPONSE_FEE_GATHERING
            //      If RC_ERR, BSHCore saves charged fees back to `aggregationFee` state mapping variable
            //      If RC_OK: do nothing
            handleResponseService(_sn, response.code, response.message);
        } else if (_sm.serviceType == Types.ServiceType.UNKNOWN_TYPE) {
            emit UnknownResponse(_from, _sn);
        } else {
            //  If none of those types above, BSH responds a message of RES_UNKNOWN_TYPE
            sendResponseMessage(
                Types.ServiceType.UNKNOWN_TYPE,
                _from,
                _sn,
                "Unknown",
                RC_ERR
            );
        }
    }

    /**
     @notice BSH handle BTP Error from BMC contract
     @dev Caller must be BMC contract only 
     @param _svc     A service name of BSH contract     
     @param _sn      A serial number of a service request 
     @param _code    A response code of a message (RC_OK / RC_ERR)
     @param _msg     A response message
    */
    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override onlyBMC {
        require(_svc.compareTo(serviceName) == true, "InvalidSvc");
        require(bytes(requests[_sn].from).length != 0, "InvalidSN");
        handleResponseService(_sn, _code, _msg);
    }

    function handleResponseService(
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) private {
        address _caller = requests[_sn].from.parseAddress();
        uint256 loop = requests[_sn].coinNames.length;
        for (uint256 i = 0; i < loop; i++) {
            bshCore.handleResponseService(
                _caller,
                requests[_sn].coinNames[i],
                requests[_sn].amounts[i],
                requests[_sn].fees[i],
                _code
            );
        }
        delete requests[_sn];
        numOfPendingRequests--;
        emit TransferEnd(_caller, _sn, _code, _msg);
    }

    /**
     @notice Handle a list of minting/transferring coins/tokens
     @dev Caller must be BMC contract only 
     @param _to          An address to receive coins/tokens    
     @param _assets      A list of requested coin respectively with an amount
    */
    function handleRequestService(
        string memory _to,
        Types.Asset[] memory _assets
    ) external {
        require(msg.sender == address(this), "Unauthorized");
        for (uint256 i = 0; i < _assets.length; i++) {
            require(
                bshCore.isValidCoin(_assets[i].coinName) == true,
                "UnregisteredCoin"
            );
            //  @dev There might be many errors generating by BSHCore contract
            //  which includes also low-level error
            //  Thus, must use try_catch at this point so that it can return an expected response
            try
                bshCore.mint(
                    _to.parseAddress(),
                    _assets[i].coinName,
                    _assets[i].value
                )
            {} catch {
                revert("TransferFailed");
            }
        }
    }

    function sendResponseMessage(
        Types.ServiceType _serviceType,
        string memory _to,
        uint256 _sn,
        string memory _msg,
        uint256 _code
    ) private {
        bmc.sendMessage(
            _to,
            serviceName,
            _sn,
            Types.ServiceMessage(
                _serviceType,
                Types.Response(_code, _msg).encodeResponse()
            ).encodeServiceMessage()
        );
    }

    /**
     @notice BSH handle Gather Fee Message request from BMC contract
     @dev Caller must be BMC contract only
     @param _fa     A BTP address of fee aggregator
     @param _svc    A name of the service
    */
    function handleFeeGathering(string calldata _fa, string calldata _svc)
        external
        override
        onlyBMC
    {
        require(_svc.compareTo(serviceName) == true, "InvalidSvc");
        //  If adress of Fee Aggregator (_fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        //  @dev this part simply check whether `_fa` is splittable (`prefix` + `_net` + `dstAddr`)
        //  checking validity of `_net` and `dstAddr` does not belong to BSHPeriphery's scope
        _fa.splitBTPAddress();
        bshCore.transferFees(_fa);
    }

    //  @dev Solidity does not allow to use try_catch with internal function
    //  Thus, this is a work-around solution
    //  Since this function is basically checking whether a string address
    //  can be parsed to address type. Hence, it would not have any restrictions
    function checkParseAddress(string calldata _to) external pure {
        _to.parseAddress();
    }
}
