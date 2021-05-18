// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "./IBSH.sol";
import "./IBMC.sol";
import "../Libraries/TypesLib.sol";
import "../Libraries/RLPEncodeStructLib.sol";
import "../Libraries/RLPDecodeStructLib.sol";
import "../Libraries/ParseAddressLib.sol";
import "../Libraries/StringsLib.sol";
import "../Libraries/Owner.sol";
import "@openzeppelin/contracts/token/ERC1155/ERC1155.sol";
import "@openzeppelin/contracts/math/SafeMath.sol";
import "@openzeppelin/contracts/token/ERC1155/ERC1155Holder.sol";

/**
   @title Interface of BSH Coin transfer service
   @dev This contract use to handle coin transfer service
   Note: The coin of following interface can be:
   Native Coin : The native coin of this chain
   Wrapped Native Coin : A tokenized ERC1155 version of another native coin like ICX
*/
contract NativeCoinBSH is IBSH, ERC1155, ERC1155Holder, Owner {
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.Response;
    using RLPDecodeStruct for bytes;
    using SafeMath for uint256;
    using ParseAddress for address;
    using ParseAddress for string;
    using Address for address;
    using Strings for string;

    /**   @notice Sends a receipt to user
        The `_to` receiver.
        The `_sn` sequence number of service message.
        The `_coinName` the name of coin.
        The `_value` transfer value.    
    */
    event TransferStart(
        address indexed _from,
        string _to,
        uint256 _sn,
        Types.AssetTransferDetail[] _assetDetails
    );

    event TransferEnd(
        address indexed _from,
        uint256 _sn,
        uint256 _code,
        string _response
    );

    IBMC private bmc;
    mapping(uint256 => Types.PendingTransferCoin) private requests; // a list of transferring requests
    mapping(uint256 => Types.Asset[]) internal pendingFA;    // a list of pending transfer Aggregation Fee. MUST set back to 'private' after testing
    mapping(address => mapping(string => Types.Balance)) private balances;
    mapping(string => Types.Coin) private coins; //  a list of all supported coins
    mapping(string => uint) internal aggregationFee;   // storing Aggregation Fee in state mapping variable. MUST set back to 'private' after testing
    string[] private coinsName; // a string array stores names of supported coins
    uint256 private numOfCoins;
    string public serviceName; //    BSH Service Name

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;
    uint256 private constant FEE_DENOMINATOR = 10**6;

    uint256 private serialNo; //  a counter of request service, i.e. Transfer Coin/Token

    modifier onlyBMC {
        require(msg.sender == address(bmc), "Unauthorized");
        _;
    }

    constructor(
        address _bmc,
        string memory _serviceName,
        string memory _nativeCoinName,
        string memory _symbol,
        uint256 _decimals,
        uint256 _feeNumerator
    ) ERC1155("") Owner() {
        bmc = IBMC(_bmc);
        bmc.requestAddService(_serviceName, address(this));
        serviceName = _serviceName;
        coins[_nativeCoinName] = Types.Coin(
            0, //  native coin is assigend '_id' = 0
            _symbol,
            _decimals,
            _feeNumerator
        );
        coinsName.push(_nativeCoinName);
        numOfCoins++;
        serialNo = 0;
    }

    /***********************************************************************************
                    Coin/Wrapped Native Coin Related Functions

        - Functions to register wrapped native coins from another blockchains
        - Functions to query supported Coins/Wrapped Coins  
        - Functions to query Spendable, Locked, and Refundable of Coins/Wrapped Coins                   
    ************************************************************************************/

    /**
     @notice Registers a wrapped coin and id number of a supporting coin.
     @dev Caller must be an operator of BTP network
     _name Must be different with the native coin name.
     @dev '_id' of a wrapped coin is generated by using keccak256
     '_id' = 0 is fixed to assign to native coin
     @param _name    Coin name.
     @param _symbol    Coin symbol.     
     @param _decimals    decimals number.    
    */
    function register(
        string calldata _name,
        string calldata _symbol,
        uint256 _decimals,
        uint256 _feeNumerator
    ) external owner {
        require(bytes(coins[_name].symbol).length == 0, "Token existed");
        coins[_name] = Types.Coin(
            uint256(keccak256(abi.encodePacked(_name))),
            _symbol,
            _decimals,
            _feeNumerator
        );
        coinsName.push(_name);
        numOfCoins++;
    }

    /**
       @notice Return all supported coins names in other networks by the BSH contract
       @dev 
       @return _names   An array of strings.
    */
    function coinNames() external view returns (string[] memory _names) {
        _names = new string[](numOfCoins);
        uint256 temp = 0;
        for (uint256 i = 0; i < coinsName.length; i++) {
            if (bytes(coins[coinsName[i]].symbol).length != 0) {
                _names[temp] = coinsName[i];
                temp++;
            }
        }
        return _names;
    }

    /**
       @notice  Return an _id number and a symbol of Coin whose name is the same with given _coinName.
       @dev     Return nullempty if not found.
       @return  A Coin object.
    */
    function coinOf(string calldata _coinName)
        external
        view
        returns (Types.Coin memory)
    {
        return coins[_coinName];
    }

    /*
     @notice Return an accumulated charging fee of all supporting coins
    */
    function getAccumulatedFees()
        external
        view
        returns (Types.Asset[] memory accumulatedFees)
    {
        accumulatedFees = new Types.Asset[](coinsName.length);
        for (uint i = 0; i < coinsName.length; i++) {
            if (bytes(coins[coinsName[i]].symbol).length != 0) {
                accumulatedFees[i] = (
                    Types.Asset(coinsName[i], aggregationFee[coinsName[i]])
                );
            }
        }
        return accumulatedFees;
    }

    /*
     @notice Return a usable/locked balance of an account based on coinName.
     @dev    Locked Balance means an amount of Coins/Wrapped Coins is currently 
             at a pending state when a transfer tx is requested from this chain to another
     @dev    Return 0 if not found
     @return _lockedBalance = an amount of locked Coins/WrappedCoins
     @return _usableBalance = an amount of spendable Coins/WrappedCoins 
     @return _refundableBalance = an amount of Coins on waiting to re-claim
    */
    function getBalanceOf(address _owner, string memory _coinName) external view
        returns (uint256 _usableBalance, uint256 _lockedBalance, uint256 _refundableBalance)
    {
        //  Check whether _coinName is a nativeCoin
        if (coins[_coinName].id == 0 && bytes(coins[_coinName].symbol).length != 0) {
            return (
                _owner.balance,
                balances[_owner][_coinName].lockedBalance,
                balances[_owner][_coinName].refundableBalance
            );
        }

        return (
            this.balanceOf(_owner, coins[_coinName].id),
            balances[_owner][_coinName].lockedBalance,
            balances[_owner][_coinName].refundableBalance
        );
    }

    /*
     @notice Return a list locked/usable balance of an account.
     @dev The order of request's coinNames must be the same with the order of return balance
     @dev Return 0 if not found.
     @return _usableBalances[] An array of spendable balances 
     @return _lockedBalances[] An array of locked balances
    */
    function getBalanceOfBatch(address _owner, string[] calldata _coinNames) external view returns
    (
        uint256[] memory _usableBalances,
        uint256[] memory _lockedBalances,
        uint256[] memory _refundableBalances
    ){
        _usableBalances = new uint[](_coinNames.length);
        _lockedBalances = new uint[](_coinNames.length);
        _refundableBalances = new uint[](_coinNames.length);
        for (uint i = 0; i < _coinNames.length; i++) {
            (_usableBalances[i], _lockedBalances[i], _refundableBalances[i]) =
                this.getBalanceOf(_owner, _coinNames[i]);
        }
        return (_usableBalances, _lockedBalances, _refundableBalances);
    }

    /**
       @notice Reclaim the coin's refundable balance by an owner.
       @dev Caller must be an owner of coin
       @dev This function only applies on native coin (not wrapped coins)
       The amount to claim must be smaller than refundable balance
       @param _coinName   A given name of coin to be re-claim
       @param _value       An amount of re-claiming 
    */
    function reclaim(string calldata _coinName, uint256 _value) external {
        require(
            bytes(coins[_coinName].symbol).length != 0,
            "Invalid Token"
        );
        require(
            balances[msg.sender][_coinName].refundableBalance >= _value,
            "Insufficient balance"
        );

        balances[msg.sender][_coinName].refundableBalance = balances[
            msg.sender
        ][_coinName]
            .refundableBalance
            .sub(_value);

        if (_coinName.compareTo(coinsName[0])) {
            payable(msg.sender).transfer(_value);
        } else {
            this.safeTransferFrom(
                address(this),
                msg.sender,
                coins[_coinName].id,
                _value,
                ""
            );
        }
    }

    /***********************************************************************************
                    Coins/Wrapped Coins Transfer Related Functions

        - Function handles request transfer Coins to another chain
        - Function handles request transfer Wrapped Coins to another chain  
        - Prepare Service Message and send it to a BMC contract                   
    ************************************************************************************/

    /**
       @notice Allow users to deposit `msg.value` native coin into a BSH contract.
       @dev MUST specify msg.value
       @param _to  An address that a user expects to receive an equivalent amount of tokens.
    */
    function transfer(string calldata _to) external payable {
        // require(msg.value > 0, "Invalid amount");
        //  Aggregation Fee will be charged on BSH Contract
        //  This fee is set when coins is registered
        //  If fee = 0, revert()
        //  Otherwise, charge_amt = (_amt * fee) / 100
        uint chargeAmt = msg.value.mul(coins[coinsName[0]].feeNumerator).div(FEE_DENOMINATOR);
        require(chargeAmt > 0, "Invalid amount");
        sendServiceMessage(_to, coinsName[0], msg.value.sub(chargeAmt), chargeAmt);
    }

    /**
       @notice Allow users to deposit an amount of wrapped native coin `_coinName` from the `msg.sender` address into the BSH contract.
       @dev Caller must set to approve that the wrapped tokens can be transferred out of the `msg.sender` account by the operator.
       It MUST revert if the balance of the holder for token `_coinName` is lower than the `_value` sent.
       @param _coinName    A given name of coin that is equivalent to retrieve a wrapped Token Contract's address, i.e. list["Token A"] = 0x12345678
       @param _value       Transferring amount.
       @param _to          Target address.
    */
    function transfer(
        string calldata _coinName,
        uint256 _value,
        string calldata _to
    ) external {
        require(
            bytes(coins[_coinName].symbol).length != 0,
            "Invalid Token"
        );
        uint chargeAmt = _value.mul(coins[_coinName].feeNumerator).div(FEE_DENOMINATOR);
        require(chargeAmt > 0, "Invalid amount");
        //  Transfer and Lock Token processes:
        //  To transfer, BSH contract call safeTransferFrom()  to transfer
        //  the Token from Caller's account (msg.sender)
        //  For such thing, Caller must approve (setApproveForAll) to accept
        //  token being transfer out by an Operator
        //  If it is failed, this transaction is reverted.
        //  After transferring token, BSH contract set Caller's locked balance
        //  as a record of pending transfer tx
        //  When a tx is completed without error on another chain,
        //  Locked Token amount (bind to address of caller) will be reset/subtract
        //  If not, this locked amount will be updated (subtract or set to 0)
        //  and BSH contract will issue a Token refund to Caller
        uint256 _id = coins[_coinName].id;
        this.safeTransferFrom(msg.sender, address(this), _id, _value, "");
        sendServiceMessage(_to, _coinName, _value.sub(chargeAmt), chargeAmt);
    }

    function sendServiceMessage(
        string calldata _to,
        string memory _coinName,
        uint256 _value,
        uint256 _fee
    ) private {
        balances[msg.sender][_coinName].lockedBalance = _value.add(_fee).add(
            balances[msg.sender][_coinName].lockedBalance
        );

        //  Send Service Message to BMC
        //  If '_to' address is an invalid BTP Address format
        //  VM throws an error and revert(). Thus, it does not need 
        //  a try_catch at this point
        string memory _toNetwork;
        string memory _toAddress;
        (_toNetwork, _toAddress) = _to.splitBTPAddress();

        Types.Asset[] memory assets = new Types.Asset[](1);
        assets[0] = Types.Asset(_coinName, _value);
        bmc.sendMessage(
            _toNetwork,
            serviceName,
            serialNo,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types
                    .TransferCoin(
                    address(msg.sender).toString(),
                    _toAddress,
                    assets
                )
                    .encodeTransferCoinMsg()
            )
                .encodeServiceMessage()
        );

        //  Push pending tx into Record list
        requests[serialNo] = Types.PendingTransferCoin(
            address(msg.sender).toString(),
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
            msg.sender,
            _to,
            serialNo,
            asset
        );
        serialNo++;
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
        require(_svc.compareTo(serviceName) == true, "Invalid service");
        Types.ServiceMessage memory _sm;
        try this.tryDecodeServiceMessage(_msg) returns (Types.ServiceMessage memory ret) {
            _sm = ret;
        }catch {
            revert("Decode failed");
        }

        if (_sm.serviceType == Types.ServiceType.REQUEST_COIN_TRANSFER) {
            Types.TransferCoin memory _tc;
            try this.tryDecodeTransferCoinMsg(_sm.data) returns (Types.TransferCoin memory ret) {
                _tc = ret;
            }catch {
                revert("Decode failed");
            }
            handleRequestService(_tc.assets[0].coinName, _tc.to, _from, _tc.assets[0].value, _sn);
            return;
        } else if (
            _sm.serviceType == Types.ServiceType.REPONSE_HANDLE_SERVICE
        ) {
            //  Check whether '_sn' is pending state
            require(
                pendingFA[_sn].length != 0 ||
                bytes(requests[_sn].from).length != 0,
                "Invalid SN"
            );

            bool feeAggregationSvc;
            if (pendingFA[_sn].length != 0) {
                feeAggregationSvc = true;
            }
            Types.Response memory response;
            try this.tryDecodeResponse(_sm.data) returns (Types.Response memory ret) {
                response = ret;
            }catch {
                revert("Decode failed");
            }
             
            if (!feeAggregationSvc) {
                    address caller = requests[_sn].from.parseAddress();
                if (response.code == RC_ERR) {
                    handleErrorResponse(_sn);
                } else {
                    string memory _coinName = requests[_sn].coinName;
                    uint256 value = requests[_sn].value;
                    uint256 fee = requests[_sn].fee;
                    //  When a transfer tx is completed, locked balance amount should be update
                    //  and also BSH contract's tokens will be burn
                    balances[caller][_coinName].lockedBalance = balances[
                        caller
                    ][_coinName]
                        .lockedBalance
                        .sub(value.add(fee));
                    if (coins[_coinName].id != 0) {
                        _burn(address(this), coins[_coinName].id, value);
                    }    
                    //  Save Aggregation Fee into a mapping state
                    aggregationFee[_coinName] = aggregationFee[_coinName].add(fee);
                }
                delete requests[_sn];
                emit TransferEnd(caller, _sn, response.code, response.message);
                return;
            }
            Types.Asset[] memory _fees = pendingFA[_sn];
            if (response.code == RC_ERR) {
                for (uint i = 0; i < _fees.length; i++) {
                    aggregationFee[_fees[i].coinName] = _fees[i].value;
                }
            }
            delete pendingFA[_sn];
            emit TransferEnd(address(this), _sn, response.code, response.message);
            return;
        } else if (_sm.serviceType == Types.ServiceType.UNKNOWN_TYPE) {
            //  If receiving a RES_UNKNOWN_TYPE, ignore this message
            return;
        }
        //  If none of those types above, BSH responds a message of RES_UNKNOWN_TYPE
        sendResponseMessage(
            Types.ServiceType.UNKNOWN_TYPE,
            _from,
            _sn,
            "UNKNOWN_TYPE",
            RC_ERR
        );
    }

    /**
     @notice BSH handle BTP Error from BMC contract
     @dev Caller must be BMC contract only
     @dev Since '_sn' is provided, don't understand why it needs '_src' and '_svc'   
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
        require(_svc.compareTo(serviceName) == true, "Invalid service");
        require(bytes(requests[_sn].from).length != 0, "Invalid SN");
        handleErrorResponse(_sn);
        delete requests[_sn];
        emit TransferEnd(address(this), _sn, _code, _msg);
    }

    function handleErrorResponse(
        uint256 _sn
    ) private {
        //  When receiving BTPError, refund Coins/Wrapped Coins back to a Caller
        //  Updating locked balance of a Caller
        address caller = requests[_sn].from.parseAddress();
        string memory _coinName = requests[_sn].coinName;
        uint256 amount = requests[_sn].value.add(requests[_sn].fee);
        bool success;
        if (
            coins[_coinName].id == 0 &&
            bytes(coins[_coinName].symbol).length != 0
        ) {
            (success, ) = caller.call{gas: 2300, value: amount}("");
        } else {
            uint256 _id = coins[_coinName].id;
            (success, ) = address(this).call(
                abi.encodeWithSignature(
                    "safeTransferFrom(address,address,uint256,uint256,bytes)",
                    address(this), caller, _id, amount, ""
            ));
        }

        balances[caller][_coinName].lockedBalance = balances[caller][
            _coinName
        ]
            .lockedBalance
            .sub(amount);

        if (!success) {
            balances[caller][_coinName].refundableBalance = balances[
                caller
            ][_coinName]
                .refundableBalance
                .add(amount);
        }    
    }

    function handleRequestService(
        string memory _coinName,
        string memory _toAddress,
        string calldata _toNetwork,
        uint256 _amount,
        uint256 _sn
    ) private {
        //  If '_coinName' is not native nor supporting wrappped coin
        //  revert() immediately, then BMC can catch this error
        uint256 _id = coins[_coinName].id;
        require(
            _id != 0 || (_id == 0 && bytes(coins[_coinName].symbol).length != 0),
            "Invalid token"
        );
        //  checking receiving address whether is a valid address
        //  revert() if not a valid one
        try this.checkParseAddress(_toAddress) {} 
        catch {
            revert("Invalid address");
        }
        
        if (_id == 0 && bytes(coins[_coinName].symbol).length != 0) {
            (bool success, ) =
                _toAddress.parseAddress().call{gas: 2300, value: _amount}("");
            require(success == true, "Transfer failed");
        } else {
            try this.tryToMintToken(_toAddress.parseAddress(), _id, _amount) {} 
            catch {
                revert("Mint failed");
            }
        }
        sendResponseMessage(
            Types.ServiceType.REPONSE_HANDLE_SERVICE,
            _toNetwork,
            _sn,
            "",
            RC_OK
        );
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
            Types
                .ServiceMessage(
                _serviceType,
                Types.Response(_code, _msg).encodeResponse()
            )
                .encodeServiceMessage()
        );
    }

    //  @dev Solidity does not allow using try_catch with internal function
    //  Thus, work-around solution is the followings
    //  If there is any error throwing, i.e. invalid format,
    //  BSH can catch this error, then reply back a RC_ERR Response
    function tryDecodeResponse(bytes calldata _msg) external pure returns (Types.Response memory) {
        return _msg.decodeResponse();
    }

    function tryDecodeTransferCoinMsg(bytes calldata _msg) external pure returns (Types.TransferCoin memory) {
        return _msg.decodeTransferCoinMsg();
    }

    function tryDecodeServiceMessage(bytes calldata _msg) external pure returns (Types.ServiceMessage memory) {
        return _msg.decodeServiceMessage();
    }

    function checkParseAddress(string calldata _to) external pure {
        _to.parseAddress();
    }

    //  @dev Despite this function was set as external, it MUST be called internally
    function tryToMintToken(
        address _to,
        uint256 _id,
        uint256 _amount
    ) external {
        require(msg.sender == address(this), "Only BSH");
        _mint(_to, _id, _amount, "");
    }

    /***********************************************************************************
                                Gather Fee Handler Functions                  
    ************************************************************************************/
    /**
     @notice BSH handle Gather Fee Message request from BMC contract
     @dev Caller must be BMC contract only
     @param _fa     A BTP address of fee aggregator
    */
    function handleGatherFee(
        string calldata _fa
    ) external onlyBMC {
        //  If adress of Fee Aggregator (_fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        (string memory _net, string memory _toAddress) = _fa.splitBTPAddress();
        for (uint i = 0; i < coinsName.length; i++) {
            if (aggregationFee[coinsName[i]] != 0) {
                pendingFA[serialNo].push(
                    Types.Asset(coinsName[i], aggregationFee[coinsName[i]])
                );
                delete aggregationFee[coinsName[i]];
            }
        }
        //  If there's no charged fees, just do nothing and return
        if (pendingFA[serialNo].length == 0) return;
        
        bmc.sendMessage(
            _net,
            serviceName,
            serialNo,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types.TransferCoin(
                    address(this).toString(),
                    _toAddress,
                    pendingFA[serialNo]
                ).encodeTransferCoinMsg()
            )
                .encodeServiceMessage()
        );
        Types.AssetTransferDetail[] memory assets = new Types.AssetTransferDetail[](pendingFA[serialNo].length);
        for (uint i = 0; i < pendingFA[serialNo].length; i++) {
            assets[i] = Types.AssetTransferDetail(
                pendingFA[serialNo][i].coinName,
                pendingFA[serialNo][i].value,
                0
            );
        }
        emit TransferStart(
            address(this),
            _fa,
            serialNo,
            assets
        );
        serialNo++;
    }
}