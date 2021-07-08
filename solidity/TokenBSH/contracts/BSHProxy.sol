// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./Interfaces/IBSHProxy.sol";
import "./Interfaces/IBSHImpl.sol";
import "../../icondao/Interfaces/IBMC.sol";
import "../../icondao/Libraries/TypesLib.sol";

import "@openzeppelin/contracts-upgradeable/utils/math/SafeMathUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

import "../../icondao/Libraries/RLPEncodeStructLib.sol";
import "../../icondao/Libraries/RLPDecodeStructLib.sol";
import "../../icondao/Libraries/StringsLib.sol";
import "../../icondao/Libraries/ParseAddressLib.sol";
import "../../icondao/Libraries/Owner.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract BSHProxy is IBSHProxy, Initializable {
    using SafeMathUpgradeable for uint256;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferAssets;
    using RLPEncodeStruct for Types.Response;
    using RLPDecodeStruct for bytes;
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    struct Token {
        string name;
        string symbol;
        uint256 decimals;
        uint256 feeNumerator;
    }

    mapping(address => bool) private owners;
    address[] private listOfOwners;

    IBSHImpl internal bshImpl;
    IBMC private bmc;
    mapping(string => address) tokenAddr;
    mapping(address => Token) tokens;
    mapping(address => mapping(string => Types.Balance)) private balances;
    mapping(string => uint256) public feeCollector;
    Types.Asset[] internal pendingFA;
    string[] tokenNamesList;
    uint256 private numOfTokens;

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;
    uint256 constant FEE_DENOMINATOR = 100; //TODO: keeping it simple for now
    uint256 private feeNumerator;

    event Register(string indexed name, address addr);
    event SetOwnership(address indexed promoter, address indexed newOwner);
    event RemoveOwnership(address indexed remover, address indexed formerOwner);

    modifier onlyOwner {
        require(owners[msg.sender] == true, "Unauthorized");
        _;
    }

    function initialize(uint256 _feeNumerator) public initializer {
        owners[msg.sender] = true;
        listOfOwners.push(msg.sender);
        emit SetOwnership(address(0), msg.sender);
        feeNumerator = _feeNumerator;
    }

    /**
       @notice Adding another Onwer.
       @dev Caller must be an Onwer of BTP network
       @param _owner    Address of a new Onwer.
   */
    function addOwner(address _owner) external override onlyOwner {
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
        require(listOfOwners.length > 1, "LastOwner");
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

    function updateBSHImplementation(address _bshImpl)
        external
        override
        onlyOwner
    {
        require(_bshImpl != address(0), "InvalidAddress");
        if (address(bshImpl) != address(0)) {
            require(bshImpl.hasPendingRequest() == false, "HasPendingRequest");
        }
        bshImpl = IBSHImpl(_bshImpl);
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

    function register(
        string calldata _name,
        string calldata _symbol,
        uint256 _decimals,
        uint256 _feeNumerator,
        address _addr
    ) external override onlyOwner {
        require(tokenAddr[_name] == address(0), "TokenExists");
        tokenAddr[_name] = _addr;
        tokens[_addr] = Token(_name, _symbol, _decimals, _feeNumerator);
        tokenNamesList.push(_name);
        numOfTokens++;
        emit Register(_name, _addr);
    }

    //todo: check to optimize
    function tokenNames() external view returns (string[] memory _names) {
        _names = new string[](numOfTokens);
        uint256 temp = 0;
        for (uint256 i = 0; i < tokenNamesList.length; i++) {
            if (tokenAddr[tokenNamesList[i]] != address(0)) {
                _names[temp] = tokenNamesList[i];
                temp++;
            }
        }
        return _names;
    }

    function getBalanceOf(address _owner, string memory _tokenName)
        external
        view
        override
        returns (
            uint256 _usableBalance,
            uint256 _lockedBalance,
            uint256 _refundableBalance
        )
    {
        return (
            0,
            balances[_owner][_tokenName].lockedBalance,
            balances[_owner][_tokenName].refundableBalance
        );
    }

    function getAccumulatedFees()
        external
        view
        override
        returns (Types.Asset[] memory collectedFees)
    {
        collectedFees = new Types.Asset[](tokenNamesList.length);
        for (uint256 i = 0; i < tokenNamesList.length; i++) {
            if (tokenAddr[tokenNamesList[i]] != address(0)) {
                collectedFees[i] = (
                    Types.Asset(
                        tokenNamesList[i],
                        feeCollector[tokenNamesList[i]],
                        0
                    )
                );
            }
        }
        return collectedFees;
    }

    function withdraw(string calldata _tokenName, uint256 _value) external {
        require(tokenAddr[_tokenName] != address(0), "Token not supported");
        require(
            balances[msg.sender][_tokenName].refundableBalance >= _value,
            "Insufficient balance"
        );

        balances[msg.sender][_tokenName].refundableBalance = balances[
            msg.sender
        ][_tokenName]
        .refundableBalance
        .sub(_value);
        address token_addr = tokenAddr[_tokenName];
        ERC20(token_addr).approve(address(this), _value);
        ERC20(token_addr).transferFrom(address(this), msg.sender, _value);
    }

    event debug(address add, string name, uint256 val);

    function transfer(
        string calldata _tokenName,
        uint256 _value,
        string calldata _to
    ) external override {
        address token_addr = tokenAddr[_tokenName];
        require(token_addr != address(0), "UnRegisteredToken");
        require(_value > 0, "InvalidAmount");
        ERC20(token_addr).transferFrom(msg.sender, address(this), _value);
        uint256 _fee;
        //todo check if the locked balance should hold with fee or without fee
        (_value, _fee) = this.calculateTransferFee(token_addr, _value);
        balances[msg.sender][_tokenName].lockedBalance = _value.add(
            balances[msg.sender][_tokenName].lockedBalance
        );
        _sendServiceMessage(msg.sender, _to, _tokenName, _value, _fee);
    }

    function _sendServiceMessage(
        address _from,
        string memory _to,
        string memory _tokenName,
        uint256 _value,
        uint256 _fee
    ) private {
        // Send Service Message to BMC

        Types.Asset[] memory _assets = new Types.Asset[](1);
        _assets[0] = Types.Asset(_tokenName, _value, _fee);
        bshImpl.sendServiceMessage(_from, _to, _assets);
    }

    //TODO: check for the decimals calulation && require for amount less than FEE_DENOMINATOR
    function calculateTransferFee(address token_addr, uint256 _value)
        public
        view
        returns (uint256 value, uint256 fee)
    {
        Token memory _token = tokens[token_addr];
        fee = _value.mul(_token.feeNumerator).div(FEE_DENOMINATOR);
        value = _value.sub(fee);
        return (value, fee);
    }

    function handleFeeTransfer(string memory _toFA)
        external
        override
        onlyBSHImpl
    {
        string memory _toNetwork;
        string memory _toAddress;
        (_toNetwork, _toAddress) = _toFA.splitBTPAddress();

        for (uint256 i = 0; i < tokenNamesList.length; i++) {
            if (feeCollector[tokenNamesList[i]] != 0) {
                pendingFA.push(
                    Types.Asset(
                        tokenNamesList[i],
                        feeCollector[tokenNamesList[i]],
                        0
                    )
                );
            }
            delete feeCollector[tokenNamesList[i]];
        }

        //  If there's no charged fees, just do nothing and return
        if (pendingFA.length == 0) return;

        bshImpl.sendServiceMessage(address(this), _toFA, pendingFA);
        delete pendingFA;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    function handleResponse(
        address _caller,
        Types.Asset[] memory _assets,
        uint256 _code
    ) external override onlyBSHImpl {
        //  Fee Gathering and Transfer Coin Request use the same method
        //  and both have the same response
        //  In case of Fee Gathering's response, `_from` is this contract's address
        //  Thus, check that first
        //  -- If `_from` is this contract's address, then check whethere response's code is RC_ERR
        //  In case of RC_ERR, adding back charged fees to `aggregationFee` state variable
        //  In case of RC_OK, ignore and return
        //  -- Otherwise, handle service's response as normal
        if (_caller == address(this)) {
            if (_code == RC_ERR) {
                for (uint256 i = 0; i < _assets.length; i++) {
                    string memory tokenName = _assets[i].name;
                    feeCollector[tokenName] = feeCollector[tokenName].add(
                        _assets[i].value
                    );
                }
            }
            return;
        }
        for (uint256 i = 0; i < _assets.length; i++) {
            string memory _tokenName = _assets[i].name;
            uint256 value = _assets[i].value;
            uint256 fee = _assets[i].fee;
            balances[_caller][_tokenName].lockedBalance = balances[_caller][
                _tokenName
            ]
            .lockedBalance
            .sub(value);
            if (_code == RC_ERR) {
                balances[_caller][_tokenName].refundableBalance = balances[
                    _caller
                ][_tokenName]
                .refundableBalance
                .add(value)
                .add(fee);
            } else if (_code == RC_OK) {
                feeCollector[_tokenName] = feeCollector[_tokenName].add(fee);
            }
        }
    }

    function handleTransferRequest(
        address _toAddress,
        string calldata _tokenName,
        uint256 _amount
    ) external override onlyBSHImpl {
        //TODO: if there is no inital balance with the tokenBSH, this transfer will fail
        address token_addr = tokenAddr[_tokenName];
        ERC20(token_addr).approve(address(this), _amount);
        ERC20(token_addr).transferFrom(address(this), _toAddress, _amount);
    }

    /**
       @notice  Check if a _tokenName is registered
       @dev     used by BSHProxy contract to validate a requested _tokenName
       @return  _registered     true of false
    */
    function isTokenRegisterd(string calldata _tokenName)
        external
        view
        override
        returns (bool _registered)
    {
        address _tokenaddr = tokenAddr[_tokenName];
        return (_tokenaddr != address(0));
    }

    modifier onlyBSHImpl {
        require(msg.sender == address(bshImpl), "Unauthorized");
        _;
    }
}
