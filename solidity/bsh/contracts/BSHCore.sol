// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "./interfaces/IBSHPeriphery.sol";
import "./interfaces/IBSHCore.sol";
import "./libraries/String.sol";
import "./libraries/Types.sol";
import "@openzeppelin/contracts-upgradeable/math/SafeMathUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC1155/ERC1155Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC1155/ERC1155HolderUpgradeable.sol";

/**
   @title BSHCore contract
   @dev This contract is used to handle coin transferring service
   Note: The coin of following contract can be:
   Native Coin : The native coin of this chain
   Wrapped Native Coin : A tokenized ERC1155 version of another native coin like ICX
*/
contract BSHCore is
    Initializable,
    IBSHCore,
    ERC1155Upgradeable,
    ERC1155HolderUpgradeable
{
    using SafeMathUpgradeable for uint256;
    using String for string;
    event SetOwnership(address indexed promoter, address indexed newOwner);
    event RemoveOwnership(address indexed remover, address indexed formerOwner);

    modifier onlyOwner {
        require(owners[msg.sender] == true, "Unauthorized");
        _;
    }

    modifier onlyBSHPeriphery {
        require(msg.sender == address(bshPeriphery), "Unauthorized");
        _;
    }

    mapping(address => bool) private owners;
    address[] private listOfOwners;

    IBSHPeriphery internal bshPeriphery;
    mapping(string => uint256) internal aggregationFee; // storing Aggregation Fee in state mapping variable.
    mapping(address => mapping(string => Types.Balance)) internal balances;
    mapping(string => uint256) private coins; //  a list of all supported coins
    string[] internal coinsName; // a string array stores names of supported coins
    string[] private chargedCoins; //   a list of coins' names have been charged so far (use this when Fee Gathering occurs)
    uint256[] private chargedAmounts; //   a list of amounts have been charged so far (use this when Fee Gathering occurs)

    uint256 private constant FEE_DENOMINATOR = 10**4;
    uint256 public feeNumerator;
    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;

    function initialize(
        string calldata _uri,
        string calldata _nativeCoinName,
        uint256 _feeNumerator
    ) public initializer {
        __ERC1155_init(_uri);
        __ERC1155Holder_init();

        owners[msg.sender] = true;
        listOfOwners.push(msg.sender);
        emit SetOwnership(address(0), msg.sender);

        coins[_nativeCoinName] = 0;
        feeNumerator = _feeNumerator;
        coinsName.push(_nativeCoinName);
    }

    /**
       @notice Adding another Onwer.
       @dev Caller must be an Onwer of BTP network
       @param _owner    Address of a new Onwer.
   */
    function addOwner(address _owner) external override onlyOwner {
        require(owners[_owner] == false, "ExistedOwner");
        owners[_owner] = true;
        listOfOwners.push(_owner);
        emit SetOwnership(msg.sender, _owner);
    }

    /**
       @notice Removing an existing Owner.
       @dev Caller must be an Owner of BTP network
       @dev If only one Owner left, unable to remove the last Owner
       @param _owner    Address of an Owner to be removed.
   */
    function removeOwner(address _owner) external override onlyOwner {
        require(listOfOwners.length > 1, "Unable to remove last Owner");
        require(owners[_owner] == true, "Removing Owner not found");
        delete owners[_owner];
        _remove(_owner);
        emit RemoveOwnership(msg.sender, _owner);
    }

    function _remove(address _addr) internal {
        for (uint256 i = 0; i < listOfOwners.length; i++)
            if (listOfOwners[i] == _addr) {
                listOfOwners[i] = listOfOwners[listOfOwners.length - 1];
                listOfOwners.pop();
                break;
            }
    }

    /**
       @notice Checking whether one specific address has Owner role.
       @dev Caller can be ANY
       @param _owner    Address needs to verify.
    */
    function isOwner(address _owner) external view override returns (bool) {
        return owners[_owner];
    }

    /**
       @notice Get a list of current Owners
       @dev Caller can be ANY
       @return      An array of addresses of current Owners
    */
    function getOwners() external view override returns (address[] memory) {
        return listOfOwners;
    }

    /**
        @notice update BSH Periphery address.
        @dev Caller must be an Owner of this contract
        _bshPeriphery Must be different with the existing one.
        @param _bshPeriphery    BSHPeriphery contract address.
    */
    function updateBSHPeriphery(address _bshPeriphery)
        external
        override
        onlyOwner
    {
        require(_bshPeriphery != address(0), "InvalidSetting");
        if (address(bshPeriphery) != address(0)) {
            require(
                bshPeriphery.hasPendingRequest() == false,
                "HasPendingRequest"
            );
        }
        bshPeriphery = IBSHPeriphery(_bshPeriphery);
    }

    /**
        @notice update base uri.
        @dev Caller must be an Owner of this contract
        the uri must be initilized in construction.
        @param _newURI    new uri
    */
    function updateUri(string calldata _newURI) external override onlyOwner {
        _setURI(_newURI);
    }

    /**
        @notice set fee ratio.
        @dev Caller must be an Owner of this contract
        The transfer fee is calculated by feeNumerator/FEE_DEMONINATOR. 
        The feeNumetator should be less than FEE_DEMONINATOR
        _feeNumerator is set to `10` in construction by default, which means the default fee ratio is 0.1%.
        @param _feeNumerator    the fee numerator
    */
    function setFeeRatio(uint256 _feeNumerator) external override onlyOwner {
        require(_feeNumerator <= FEE_DENOMINATOR, "InvalidSetting");
        feeNumerator = _feeNumerator;
    }

    /**
        @notice Registers a wrapped coin and id number of a supporting coin.
        @dev Caller must be an Owner of this contract
        _name Must be different with the native coin name.
        @dev '_id' of a wrapped coin is generated by using keccak256
          '_id' = 0 is fixed to assign to native coin
        @param _name    Coin name. 
    */
    function register(string calldata _name) external override onlyOwner {
        require(coins[_name] == 0, "ExistToken");
        coins[_name] = uint256(keccak256(abi.encodePacked(_name)));
        coinsName.push(_name);
    }

    /**
       @notice Return all supported coins names
       @dev 
       @return _names   An array of strings.
    */
    function coinNames()
        external
        view
        override
        returns (string[] memory _names)
    {
        return coinsName;
    }

    /**
       @notice  Return an _id number of Coin whose name is the same with given _coinName.
       @dev     Return nullempty if not found.
       @return  _coinId     An ID number of _coinName.
    */
    function coinId(string calldata _coinName)
        external
        view
        override
        returns (uint256 _coinId)
    {
        return coins[_coinName];
    }

    /**
       @notice  Check Validity of a _coinName
       @dev     Call by BSHPeriphery contract to validate a requested _coinName
       @return  _valid     true of false
    */
    function isValidCoin(string calldata _coinName)
        external
        view
        override
        returns (bool _valid)
    {
        return (coins[_coinName] != 0 || _coinName.compareTo(coinsName[0]));
    }

    /**
        @notice Return a usable/locked/refundable balance of an account based on coinName.
        @return _usableBalance the balance that users are holding.
        @return _lockedBalance when users transfer the coin, 
                it will be locked until getting the Service Message Response.
        @return _refundableBalance refundable balance is the balance that will be refunded to users.
    */
    function getBalanceOf(address _owner, string memory _coinName)
        external
        view
        override
        returns (
            uint256 _usableBalance,
            uint256 _lockedBalance,
            uint256 _refundableBalance
        )
    {
        if (_coinName.compareTo(coinsName[0])) {
            return (
                address(_owner).balance,
                balances[_owner][_coinName].lockedBalance,
                balances[_owner][_coinName].refundableBalance
            );
        }
        return (
            this.balanceOf(_owner, coins[_coinName]),
            balances[_owner][_coinName].lockedBalance,
            balances[_owner][_coinName].refundableBalance
        );
    }

    /**
        @notice Return a list Balance of an account.
        @dev The order of request's coinNames must be the same with the order of return balance
        Return 0 if not found.
        @return _usableBalances         An array of Usable Balances
        @return _lockedBalances         An array of Locked Balances
        @return _refundableBalances     An array of Refundable Balances
    */
    function getBalanceOfBatch(address _owner, string[] calldata _coinNames)
        external
        view
        override
        returns (
            uint256[] memory _usableBalances,
            uint256[] memory _lockedBalances,
            uint256[] memory _refundableBalances
        )
    {
        _usableBalances = new uint256[](_coinNames.length);
        _lockedBalances = new uint256[](_coinNames.length);
        _refundableBalances = new uint256[](_coinNames.length);
        for (uint256 i = 0; i < _coinNames.length; i++) {
            (
                _usableBalances[i],
                _lockedBalances[i],
                _refundableBalances[i]
            ) = this.getBalanceOf(_owner, _coinNames[i]);
        }
        return (_usableBalances, _lockedBalances, _refundableBalances);
    }

    /**
        @notice Return a list accumulated Fees.
        @dev only return the asset that has Asset's value greater than 0
        @return _accumulatedFees An array of Asset
    */
    function getAccumulatedFees()
        external
        view
        override
        returns (Types.Asset[] memory _accumulatedFees)
    {
        _accumulatedFees = new Types.Asset[](coinsName.length);
        for (uint256 i = 0; i < coinsName.length; i++) {
            _accumulatedFees[i] = (
                Types.Asset(coinsName[i], aggregationFee[coinsName[i]])
            );
        }
        return _accumulatedFees;
    }

    /**
       @notice Allow users to deposit `msg.value` native coin into a BSHCore contract.
       @dev MUST specify msg.value
       @param _to  An address that a user expects to receive an amount of tokens.
    */
    function transferNativeCoin(string calldata _to) external payable override {
        //  Aggregation Fee will be charged on BSH Contract
        //  A Fee Ratio is set when BSH contract is created
        //  If charging fee amount is zero, revert()
        //  Otherwise, charge_amt = (_amt * feeNumerator) / FEE_DENOMINATOR
        uint256 _chargeAmt = msg.value.mul(feeNumerator).div(FEE_DENOMINATOR);
        require(_chargeAmt > 0, "InvalidAmount");
        //  @dev msg.value is an amount request to transfer (include fee)
        //  Later on, it will be calculated a true amount that should be received at a destination
        _sendServiceMessage(
            msg.sender,
            _to,
            coinsName[0],
            msg.value,
            _chargeAmt
        );
    }

    /**
       @notice Allow users to deposit an amount of wrapped native coin `_coinName` from the `msg.sender` address into the BSHCore contract.
       @dev Caller must set to approve that the wrapped tokens can be transferred out of the `msg.sender` account by BSHCore contract.
       It MUST revert if the balance of the holder for token `_coinName` is lower than the `_value` sent.
       @param _coinName    A given name of a wrapped coin 
       @param _value       An amount request to transfer from a Requester (include fee)
       @param _to          Target BTP address.
    */
    function transfer(
        string calldata _coinName,
        uint256 _value,
        string calldata _to
    ) external override {
        require(coins[_coinName] != 0, "UnregisterCoin");
        uint256 _chargeAmt = _value.mul(feeNumerator).div(FEE_DENOMINATOR);
        require(_chargeAmt > 0, "InvalidAmount");
        //  Transfer and Lock Token processes:
        //  BSHCore contract calls safeTransferFrom() to transfer the Token from Caller's account (msg.sender)
        //  Before that, Caller must approve (setApproveForAll) to accept
        //  token being transfer out by an Operator
        //  If this requirement is failed, a transaction is reverted.
        //  After transferring token, BSHCore contract updates Caller's locked balance
        //  as a record of pending transfer transaction
        //  When a transaction is completed without any error on another chain,
        //  Locked Token amount (bind to an address of caller) will be reset/subtract,
        //  then emit a successful TransferEnd event as a notification
        //  Otherwise, the locked amount will also be updated
        //  but BSHCore contract will issue a refund to Caller before emitting an error TransferEnd event
        this.safeTransferFrom(
            msg.sender,
            address(this),
            coins[_coinName],
            _value,
            ""
        );
        //  @dev _value is an amount request to transfer (include fee)
        //  Later on, it will be calculated a true amount that should be received at a destination
        _sendServiceMessage(msg.sender, _to, _coinName, _value, _chargeAmt);
    }

    /**
       @notice This private function handles overlapping procedure before sending a service message to BSHPeriphery
       @param _from             An address of a Requester
       @param _to               BTP address of of Receiver on another chain
       @param _coinName         A given name of a requested coin 
       @param _value            A requested amount to transfer from a Requester (include fee)
       @param _chargeAmt        An amount being charged for this request
    */
    function _sendServiceMessage(
        address _from,
        string calldata _to,
        string memory _coinName,
        uint256 _value,
        uint256 _chargeAmt
    ) private {
        //  Lock this requested _value as a record of a pending transferring transaction
        //  @dev Note that: _value is a requested amount to transfer from a Requester including charged fee
        //  The true amount to receive at a destination receiver is calculated by
        //  _amounts[0] = _value.sub(_chargeAmt);
        lockBalance(_from, _coinName, _value);
        string[] memory _coins = new string[](1);
        _coins[0] = _coinName;
        uint256[] memory _amounts = new uint256[](1);
        _amounts[0] = _value.sub(_chargeAmt);
        uint256[] memory _fees = new uint256[](1);
        _fees[0] = _chargeAmt;
        //  @dev Note that: `_amounts` is a true amount to receive at a destination after deducting a charged fee
        bshPeriphery.sendServiceMessage(_from, _to, _coins, _amounts, _fees);
    }

    /**
       @notice Allow users to transfer multiple coins/wrapped coins to another chain
       @dev Caller must set to approve that the wrapped tokens can be transferred out of the `msg.sender` account by BSHCore contract.
       It MUST revert if the balance of the holder for token `_coinName` is lower than the `_value` sent.
       In case of transferring a native coin, it also checks `msg.value`
       The number of requested coins MUST be as the same as the number of requested values
       The requested coins and values MUST be matched respectively
       @param _coinNames    A list of requested transferring coins/wrapped coins
       @param _values       A list of requested transferring values respectively with its coin name
       @param _to          Target BTP address.
    */
    function transferBatch(
        string[] calldata _coinNames,
        uint256[] memory _values,
        string calldata _to
    ) external payable override {
        require(_coinNames.length == _values.length, "InvalidRequest");
        uint256 size = msg.value != 0 ? _coinNames.length.add(1) : _coinNames.length;
        uint256[] memory _ids = new uint256[](_coinNames.length);
        string[] memory _coins = new string[](size);
        uint256[] memory _amounts = new uint256[](size);
        uint256[] memory _chargeAmts = new uint256[](size);

        for (uint256 i = 0; i < _coinNames.length; i++) {
            _ids[i] = coins[_coinNames[i]];
            //  Does not need to check if _coinNames[i] == native_coin
            //  If _coinNames[i] is a native_coin, coins[_coinNames[i]] = 0
            require(_ids[i] != 0, "UnregisterCoin");
            _coins[i] = _coinNames[i];
            _chargeAmts[i] = _values[i].mul(feeNumerator).div(FEE_DENOMINATOR);
            require(_chargeAmts[i] > 0, "InvalidAmount");
            _amounts[i] = _values[i].sub(_chargeAmts[i]);
            //  Lock this requested _value as a record of a pending transferring transaction
            //  @dev Note that: _value is a requested amount to transfer from a Requester including charged fee
            //  The true amount to receive at a destination receiver is calculated by
            //  _amounts[i] = _values[i].sub(_chargeAmts[i]);
            lockBalance(msg.sender, _coinNames[i], _values[i]);
        }

        this.safeBatchTransferFrom(
            msg.sender,
            address(this),
            _ids,
            _values,
            ""
        );

        if (msg.value != 0) {
            _coins[size - 1] = coinsName[0];   // push native_coin at the end of request
            _chargeAmts[size - 1] = msg.value.mul(feeNumerator).div(FEE_DENOMINATOR);
            require(_chargeAmts[size - 1] > 0, "InvalidAmount");
            _amounts[size - 1] = msg.value.sub(_chargeAmts[size - 1]);
            lockBalance(msg.sender, coinsName[0], msg.value);
        }

        //  @dev Note that: `_amounts` is true amounts to receive at a destination after deducting charged fees
        bshPeriphery.sendServiceMessage(
            msg.sender,
            _to,
            _coins,
            _amounts,
            _chargeAmts
        );
    }

    /**
        @notice Reclaim the token's refundable balance by an owner.
        @dev Caller must be an owner of coin
        The amount to claim must be smaller or equal than refundable balance
        @param _coinName   A given name of coin
        @param _value       An amount of re-claiming tokens
    */
    function reclaim(string calldata _coinName, uint256 _value)
        external
        override
    {
        require(
            balances[msg.sender][_coinName].refundableBalance >= _value,
            "Imbalance"
        );

        balances[msg.sender][_coinName].refundableBalance = balances[
            msg.sender
        ][_coinName]
            .refundableBalance
            .sub(_value);

        this.refund(msg.sender, _coinName, _value);
    }

    /**
        @notice return coin for the failed transfer.
        @dev Caller must be this contract
        @param _to    account
        @param _coinName    coin name    
        @param _value    the minted amount   
    */
    function refund(
        address _to,
        string calldata _coinName,
        uint256 _value
    ) external override {
        require(msg.sender == address(this), "Unauthorized");
        uint256 _id = coins[_coinName];
        if (_id == 0) {
            payable(_to).transfer(_value);
        } else {
            this.safeTransferFrom(address(this), _to, _id, _value, "");
        }
    }

    /**
        @notice mint the wrapped coin.
        @dev Caller must be an BSHPeriphery contract
        Invalid _coinName will have an _id = 0. However, _id = 0 is also dedicated to Native Coin
        Thus, BSHPeriphery will check a validity of a requested _coinName before calling
        for the _coinName indicates with id = 0, it should send the Native Coin (Example: PRA) to user account
        @param _to    the account receive the minted coin
        @param _coinName    coin name
        @param _value    the minted amount   
    */
    function mint(
        address _to,
        string calldata _coinName,
        uint256 _value
    ) external override onlyBSHPeriphery {
        uint256 _id = coins[_coinName];
        if (_id == 0) {
            payable(_to).transfer(_value);
        } else {
            _mint(_to, _id, _value, "");
        }
    }

    /**
        @notice Handle a response of a requested service
        @dev Caller must be an BSHPeriphery contract
        @param _requester   An address of originator of a requested service
        @param _coinName    A name of requested coin
        @param _value       An amount to receive on a destination chain
        @param _fee         An amount of charged fee
    */
    function handleResponseService(
        address _requester,
        string calldata _coinName,
        uint256 _value,
        uint256 _fee,
        uint256 _rspCode
    ) external override onlyBSHPeriphery {
        //  Fee Gathering and Transfer Coin Request use the same method
        //  and both have the same response
        //  In case of Fee Gathering's response, `_requester` is this contract's address
        //  Thus, check that first
        //  -- If `_requester` is this contract's address, then check whethere response's code is RC_ERR
        //  In case of RC_ERR, adding back charged fees to `aggregationFee` state variable
        //  In case of RC_OK, ignore and return
        //  -- Otherwise, handle service's response as normal
        if (_requester == address(this)) {
            if (_rspCode == RC_ERR) {
                aggregationFee[_coinName] = aggregationFee[_coinName].add(
                    _value
                );
            }
            return;
        }
        uint256 _amount = _value.add(_fee);
        balances[_requester][_coinName].lockedBalance = balances[_requester][
            _coinName
        ]
            .lockedBalance
            .sub(_amount);
        if (_rspCode == RC_ERR) {
            try this.refund(_requester, _coinName, _amount) {} catch {
                balances[_requester][_coinName].refundableBalance = balances[
                    _requester
                ][_coinName]
                    .refundableBalance
                    .add(_amount);
            }
        } else if (_rspCode == RC_OK) {
            uint256 _id = coins[_coinName];
            if (_id != 0) {
                _burn(address(this), _id, _value);
            }
            aggregationFee[_coinName] = aggregationFee[_coinName].add(_fee);
        }
    }

    /**
        @notice Handle a request of Fee Gathering
            Usage: Copy all charged fees to an array
        @dev Caller must be an BSHPeriphery contract
    */
    function transferFees(string calldata _fa)
        external
        override
        onlyBSHPeriphery
    {
        //  @dev Due to uncertainty in identifying a size of returning memory array
        //  and Solidity does not allow to use 'push' with memory array (only storage)
        //  thus, must use 'temp' storage state variable
        for (uint256 i = 0; i < coinsName.length; i++) {
            if (aggregationFee[coinsName[i]] != 0) {
                chargedCoins.push(coinsName[i]);
                chargedAmounts.push(aggregationFee[coinsName[i]]);
                delete aggregationFee[coinsName[i]];
            }
        }
        bshPeriphery.sendServiceMessage(
            address(this),
            _fa,
            chargedCoins,
            chargedAmounts,
            new uint256[](chargedCoins.length) //  chargedFees is an array of 0 since this is a fee gathering request
        );
        delete chargedCoins;
        delete chargedAmounts;
    }

    function lockBalance(
        address _to,
        string memory _coinName,
        uint256 _value
    ) private {
        balances[_to][_coinName].lockedBalance = balances[_to][_coinName]
            .lockedBalance
            .add(_value);
    }
}
