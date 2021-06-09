// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Interfaces/IBSHPeriphery.sol";
import "../Interfaces/IBSHCore.sol";
import "../Interfaces/IBMCPeriphery.sol";
import "../Libraries/TypesLib.sol";
import "../Libraries/RLPEncodeStructLib.sol";
import "../Libraries/RLPDecodeStructLib.sol";
import "../Libraries/ParseAddressLib.sol";
import "../Libraries/StringsLib.sol";
import "@openzeppelin/contracts-upgradeable/math/SafeMathUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";
/**
   @title Interface of BSH Coin transfer service
   @dev This contract use to handle coin transfer service
   Note: The coin of following interface can be:
   Native Coin : The native coin of this chain
   Wrapped Native Coin : A tokenized ERC1155 version of another native coin like ICX
*/
contract BSHPeripheryV2 is Initializable, IBSHPeriphery, OwnableUpgradeable {
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
    event UnknownResponse(
        string _from,
        uint256 _sn
    );

    IBMCPeriphery private bmc;
    IBSHCore internal bshCore;      //  must be set private. Temporarily set internal for testing
    mapping(uint256 => Types.PendingTransferCoin) private requests; // a list of transferring requests
    mapping(uint256 => Types.Asset[]) private pendingFA;    // a list of pending transfer Aggregation Fee. MUST set back to 'private' after testing
    string private serviceName; //    BSH Service Name

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;
    uint256 private serialNo; //  a counter of sequence number of service message

    modifier onlyBMC {
        require(msg.sender == address(bmc), "Unauthorized");
        _;
    }

    function get(uint256 _sn) external view returns (Types.Asset[] memory) {
        return pendingFA[_sn];
    }

    function initialize(
        address _bmc,
        address _bshCore,
        string memory _serviceName
    ) public initializer {
        __Ownable_init();

        bmc = IBMCPeriphery(_bmc);
        bmc.requestAddService(_serviceName, address(this));
        bshCore = IBSHCore(_bshCore);
        serviceName = _serviceName;
        serialNo = 0;
    }
    //  @notice This is just an example of how to add more function in upgrading a contract
    function getServiceName() external view returns (string memory) {
        return serviceName;
    }
    //  @notice This is just an example of how to add more function in upgrading a contract
    function getPendingRequest(uint256 _sn) external view returns (Types.PendingTransferCoin memory) {
        return requests[_sn];
    } 
    //  @notice This is just an example of how to add more function in upgrading a contract
    function getPendingFees(uint256 _sn) external view returns (Types.Asset[] memory) {
        return pendingFA[_sn];
    }
    //  @notice This is just an example of how to add more function in upgrading a contract
    function getAggregationFeeOf(string calldata _coinName) external view returns (uint _fee) {
        Types.Asset[] memory _fees = bshCore.getAccumulatedFees();
        for (uint i = 0; i < _fees.length; i++) {
            if (_coinName.compareTo(_fees[i].coinName)) 
                return _fees[i].value;
        }
    }

    /***********************************************************************************
                                Send Service Message Function
        -   Prepare a request of service message
        -   Sends a service message to BMCService contract
        -   Save a request to a pending list
    ************************************************************************************/

    function sendServiceMessage(
        address _from,
        string calldata _to,
        string memory _coinName,
        uint256 _value,
        uint256 _fee
    ) external override {
        //  Send Service Message to BMC
        //  If '_to' address is an invalid BTP Address format
        //  VM throws an error and revert(). Thus, it does not need 
        //  a try_catch at this point
        (string memory _toNetwork, string memory _toAddress) = _to.splitBTPAddress();

        Types.Asset[] memory assets = new Types.Asset[](1);
        assets[0] = Types.Asset(_coinName, _value);
        _sendMessage(
            _toNetwork,
            serialNo,
            encodeServiceMessage(
                Types.ServiceType.REQUEST_COIN_TRANSFER,
                encodeTransferCoin(
                    address(msg.sender).toString(),
                    _toAddress,
                    assets
                )
            )
        );

        //  Push pending tx into Record list
        requests[serialNo] = Types.PendingTransferCoin(
            _from.toString(),
            _to,
            _coinName,
            _value,
            _fee
        );
        Types.AssetTransferDetail[] memory asset = new Types.AssetTransferDetail[](1);
        asset[0] = Types.AssetTransferDetail(
            _coinName, _value, _fee
        );
        emit TransferStart(
            _from,
            _to,
            serialNo,
            asset
        );
        serialNo++;
    }

    function encodeTransferCoin(string memory _from, string memory _to, Types.Asset[] memory _assets) private pure returns (bytes memory) {
        return Types.TransferCoin(_from, _to, _assets).encodeTransferCoinMsg();
    }

    function encodeServiceMessage(Types.ServiceType _serviceType, bytes memory _msg) private pure returns (bytes memory) {
        return Types.ServiceMessage(_serviceType, _msg).encodeServiceMessage();
    }

    function _sendMessage(string memory _net, uint256 _sn, bytes memory _msg) private {
        bmc.sendMessage(_net, serviceName, _sn, _msg);
    }

    /***********************************************************************************
                                BTP Message Handler Functions                  
    ************************************************************************************/
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
        bool isError;
        string memory errMsg;

        if (_sm.serviceType == Types.ServiceType.REQUEST_COIN_TRANSFER) {
            Types.TransferCoin memory _tc = _sm.data.decodeTransferCoinMsg();
            //  checking receiving address whether is a valid address
            //  revert() if not a valid one
            try this.checkParseAddress(_tc.to) {} 
            catch {
                isError = true;
                errMsg = "invalid_address";
            }
            if (!isError) {
                try this.handleRequestService(_tc.to, _tc.assets) {
                    sendResponseMessage(
                        Types.ServiceType.REPONSE_HANDLE_SERVICE,
                        _from,
                        _sn,
                        "",
                        RC_OK
                    );
                    return;
                }catch Error (string memory _err) {
                    errMsg = _err;
                }
            }
            sendResponseMessage(
                Types.ServiceType.REPONSE_HANDLE_SERVICE,
                _from,
                _sn,
                errMsg,
                RC_ERR
            );
            return;
        } else if (
            _sm.serviceType == Types.ServiceType.REPONSE_HANDLE_SERVICE
        ) {
            //  Check whether '_sn' is pending state
            require(
                pendingFA[_sn].length != 0 ||
                bytes(requests[_sn].from).length != 0,
                "InvalidSN"
            );

            bool feeAggregationSvc;
            if (pendingFA[_sn].length != 0) {
                feeAggregationSvc = true;
            }
            Types.Response memory response = _sm.data.decodeResponse();
            if (!feeAggregationSvc) {
                address _caller = requests[_sn].from.parseAddress();
                //  @dev Not implement try_catch at this point
                //  If RC_ERR, BSHCore proceeds a refund. If a refund is failed, BSHCore issues refundable Balance
                //  If RC_OK:
                //  - requested coin = native -> update aggregation fee (likely no issue)
                //  - requested coin = wrapped coin -> BSHCore calls itself to burn its tokens and update aggregation fee (likely no issue)
                //  The only issue, which might happen, is BSHCore's token balance lower than burning amount
                //  If so, there might be something went wrong before
                bshCore.handleResponseService(
                    _caller,
                    requests[_sn].coinName,
                    requests[_sn].value,
                    requests[_sn].fee,
                    response.code
                );
                delete requests[_sn];
                emit TransferEnd(_caller, _sn, response.code, response.message);
                return;
            }
            if (response.code == RC_ERR) {
                Types.Asset[] memory _fees = pendingFA[_sn];
                //  @dev Not implement try_catch at this point
                //  If any error occurs, it can be considered as UNKNOWN_ERR
                //  revert() would be invoked and BMC would catch an error
                bshCore.handleErrorFeeGathering(_fees);
            }
            delete pendingFA[_sn];
            emit TransferEnd(address(bshCore), _sn, response.code, response.message);
            return;
        } else if (_sm.serviceType == Types.ServiceType.UNKNOWN_TYPE) {
            emit UnknownResponse(_from, _sn);
            return;
        }
        //  If none of those types above, BSH responds a message of RES_UNKNOWN_TYPE
        sendResponseMessage(
            Types.ServiceType.UNKNOWN_TYPE,
            _from,
            _sn,
            "UNKNOWN",
            RC_ERR
        );
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
        address _caller = requests[_sn].from.parseAddress();
        bshCore.handleResponseService(
            _caller, 
            requests[_sn].coinName,
            requests[_sn].value,
            requests[_sn].fee,
            _code
        );
        delete requests[_sn];
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
            require(bshCore.isValidCoin(_assets[i].coinName) == true, "unregistered_coin");
            //  @dev There might be many errors generating by BSHCore contract
            //  which includes also low-level error
            //  Thus, must use try_catch at this point so that it can return an expected response
            try bshCore.mint(_to.parseAddress(), _assets[i].coinName, _assets[i].value) {}
            catch {
                revert("transfer_failed");
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
        _sendMessage(
            _to,
            _sn,
            encodeServiceMessage(
                _serviceType,
                Types.Response(_code, _msg).encodeResponse()
            )
        );
    }

    /***********************************************************************************
                                Gather Fee Handler Functions                  
    ************************************************************************************/
    /**
     @notice BSH handle Gather Fee Message request from BMC contract
     @dev Caller must be BMC contract only
     @param _fa     A BTP address of fee aggregator
     @param _svc    A name of the service
    */
    function handleFeeGathering(
        string calldata _fa,
        string calldata _svc
    ) external override onlyBMC {
        require(_svc.compareTo(serviceName) == true, "InvalidSvc");
        //  If adress of Fee Aggregator (_fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        (string memory _net, string memory _toAddress) = _fa.splitBTPAddress();
        Types.Asset[] memory _fees = bshCore.gatherFeeRequest();
        //  If there's no charged fees, just do nothing and return
        if (_fees.length == 0) return;
        _sendMessage(
            _net,
            serialNo,
            encodeServiceMessage(
                Types.ServiceType.REQUEST_COIN_TRANSFER,
                encodeTransferCoin(
                    address(bshCore).toString(),
                    _toAddress,
                    _fees
                )
            )
        );

        Types.AssetTransferDetail[] memory assets = new Types.AssetTransferDetail[](_fees.length);
        for (uint i = 0; i < _fees.length; i++) {
            assets[i] = Types.AssetTransferDetail(
                _fees[i].coinName,
                _fees[i].value,
                0
            );
            pendingFA[serialNo].push(_fees[i]);
        }
        emit TransferStart(
            address(bshCore),
            _fa,
            serialNo,
            assets
        );
        serialNo++;
    }

    //  @dev Solidity does not allow to use try_catch with internal function
    //  Thus, this is a work-around solution
    //  Since this function is basically checking whether a string address
    //  can be parsed to address type. Hence, it would not have any restrictions
    function checkParseAddress(string calldata _to) external pure {
        _to.parseAddress();
    }
}
