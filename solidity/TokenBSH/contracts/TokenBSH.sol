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

import "../../icondao/Interfaces/IBSH.sol";
import "../../icondao/Interfaces/IBMC.sol";
import "../../icondao/Libraries/TypesLib.sol";

import "@openzeppelin/contracts/utils/math/SafeMath.sol";
import "../../icondao/Libraries/RLPEncodeStructLib.sol";
import "../../icondao/Libraries/RLPDecodeStructLib.sol";
import "../../icondao/Libraries/StringsLib.sol";
import "../../icondao/Libraries/ParseAddressLib.sol";
import "../../icondao/Libraries/Owner.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract TokenBSH is IBSH, Owner {
    using SafeMath for uint256;
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

    IBMC private bmc;
    mapping(string => address) tokenAddr;
    mapping(address => Token) tokens;
    mapping(address => mapping(string => Types.Balance)) private balances;
    event Register(string indexed name, address addr);
    mapping(uint256 => Types.TransferAssetsRecord) public requests;
    mapping(string => uint256) public feeCollector;
    mapping(uint256 => Types.Asset[]) internal pendingFA;
    string[] tokenNamesList;
    uint256 private serialNo;
    uint256 private numOfTokens;
    string public serviceName;

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;
    uint256 constant FEE_DENOMINATOR = 100; //TODO: keeping it simple for now

    event HandleBTPMessageEvent(uint256 _sn, uint256 _code, string _msg);
    event TransferStart(
        address indexed _from,
        string _to,
        uint256 _sn,
        Types.Asset[] _assets
    );

    event TransferEnd(
        address indexed _from,
        uint256 _sn,
        uint256 _code,
        string _response
    );

    constructor(address _bmc, string memory _serviceName) Owner() {
        bmc = IBMC(_bmc);
        serviceName = _serviceName;
        serialNo = 0;
    }

    modifier onlyBMC {
        require(msg.sender == address(bmc), "Only BMC");
        _;
    }

    function register(
        string calldata _name,
        string calldata _symbol,
        uint256 _decimals,
        uint256 _feeNumerator,
        address _addr
    ) external owner {
        require(
            tokenAddr[_name] == address(0),
            "Token with same name exists already."
        );
        tokenAddr[_name] = _addr;
        tokens[_addr] = Token(_name, _symbol, _decimals, _feeNumerator);
        tokenNamesList.push(_name);
        numOfTokens++;
        emit Register(_name, _addr);
    }

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
        returns (uint256 _usableBalance, uint256 _lockedBalance)
    {
        return (
            //this.balanceOf(_owner),
            balances[_owner][_tokenName].refundableBalance,
            balances[_owner][_tokenName].lockedBalance
        );
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

    function calculateTransferFee(address token_addr, uint256 _value)
        external
        view
        returns (uint256 value, uint256 fee)
    {
        return _calculateTransferFee(token_addr, _value);
    }

    //TODO: check for the decimals calulation && require for amount less than FEE_DENOMINATOR
    function _calculateTransferFee(address token_addr, uint256 _value)
        private
        view
        returns (uint256 value, uint256 fee)
    {
        Token memory _token = tokens[token_addr];
        fee = (_value * _token.feeNumerator) / FEE_DENOMINATOR;
        value = _value.sub(fee);
        return (value, fee);
    }

    function getAccumulatedFees()
        external
        view
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

    function handleFeeGathering(string memory _toFA, string memory _svc)
        external
    {
        string memory _toNetwork;
        string memory _toAddress;
        (_toNetwork, _toAddress) = _toFA.splitBTPAddress();

        for (uint256 i = 0; i < tokenNamesList.length; i++) {
            if (feeCollector[tokenNamesList[i]] != 0) {
                pendingFA[serialNo].push(
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
        if (pendingFA[serialNo].length == 0) return;

        bytes memory serviceMessage =
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_TOKEN_TRANSFER,
                Types
                    .TransferAssets(
                    address(this).toString(),
                    _toAddress,
                    pendingFA[serialNo]
                )
                    .encodeTransferAsset()
            )
                .encodeServiceMessage();

        bmc.sendMessage(_toNetwork, serviceName, serialNo, serviceMessage);

        emit TransferStart(address(this), _toFA, serialNo, pendingFA[serialNo]);
        serialNo++;
    }

    function transfer(
        string calldata _tokenName,
        uint256 _value,
        string calldata _to
    ) external {
        address token_addr = tokenAddr[_tokenName];
        require(token_addr != address(0), "Token is not registered");
        require(_value > 0, "Invalid amount specified.");
        ERC20(token_addr).transferFrom(msg.sender, address(this), _value);
        uint256 _fee;
        //todo check if the locked balance should hold with fee or without fee
        (_value, _fee) = _calculateTransferFee(token_addr, _value);
        balances[msg.sender][_tokenName].lockedBalance = _value.add(
            balances[msg.sender][_tokenName].lockedBalance
        );
        sendServiceMessage(_to, _tokenName, _value, _fee);
    }

    function sendServiceMessage(
        string memory _to,
        string memory _tokenName,
        uint256 _value,
        uint256 _fee
    ) private {
        // Send Service Message to BMC
        string memory _toNetwork;
        string memory _toAddress;
        (_toNetwork, _toAddress) = _to.splitBTPAddress();
        Types.Asset[] memory _assets = new Types.Asset[](1);
        _assets[0] = Types.Asset(_tokenName, _value, _fee);
        bytes memory serviceMessage =
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_TOKEN_TRANSFER,
                Types
                    .TransferAssets(
                    address(msg.sender).toString(),
                    _toAddress,
                    _assets
                )
                    .encodeTransferAsset()
            )
                .encodeServiceMessage();

        bmc.sendMessage(_toNetwork, serviceName, serialNo, serviceMessage);

        requests[serialNo] = Types.TransferAssetsRecord(
            address(msg.sender).toString(),
            _toAddress,
            _tokenName,
            _value,
            _fee
        );
        emit TransferStart(msg.sender, _to, serialNo, _assets);
        serialNo++;
    }

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        Types.ServiceMessage memory _sm = _msg.decodeServiceMessage();
        if (_sm.serviceType == Types.ServiceType.REQUEST_TOKEN_TRANSFER) {
            Types.TransferAssets memory _ta = _sm.data.decodeTransferAsset();
            handleRequestService(_ta, _from, _sn);
            return;
        } else if (
            _sm.serviceType == Types.ServiceType.RESPONSE_HANDLE_SERVICE
        ) {
            require(
                pendingFA[_sn].length != 0 ||
                    bytes(requests[_sn].from).length != 0,
                "Invalid SN"
            );
            bool feeAggregationSvc;
            if (pendingFA[_sn].length != 0) {
                feeAggregationSvc = true;
            }
            Types.Response memory response = _sm.data.decodeResponse();
            if (!feeAggregationSvc) {
                address caller = requests[_sn].from.parseAddress();
                if (response.code == 1) {
                    handleResponseError(_sn, response.code, response.message);
                } else {
                    string memory _tokenName = requests[_sn].name;
                    uint256 value = requests[_sn].value;
                    uint256 fee = requests[_sn].fee;
                    balances[caller][_tokenName].lockedBalance = balances[
                        caller
                    ][_tokenName]
                        .lockedBalance
                        .sub(value);
                    //TODO: to burn the tokens
                    feeCollector[_tokenName] = feeCollector[_tokenName].add(
                        fee
                    );
                }
                delete requests[_sn];
                emit TransferEnd(
                    address(this),
                    _sn,
                    response.code,
                    response.message
                );
            }

            Types.Asset[] memory _fees = pendingFA[_sn];
            if (response.code == RC_ERR) {
                for (uint256 i = 0; i < _fees.length; i++) {
                    feeCollector[_fees[i].name] = _fees[i].value;
                }
            }
            delete pendingFA[_sn];
            emit TransferEnd(
                address(this),
                _sn,
                response.code,
                response.message
            );
            return;
        } else if (_sm.serviceType == Types.ServiceType.RESPONSE_UNKNOWN) {
            // Ignore if UNknown type received
            return;
        }
        sendResponseMessage(
            Types.ServiceType.RESPONSE_UNKNOWN,
            _from,
            _sn,
            "UNKNOWN_TYPE",
            RC_ERR
        );
    }

    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override {
        require(_svc.compareTo(serviceName) == true, "Invalid service");
        require(bytes(requests[_sn].from).length != 0, "Invalid SN");
        handleResponseError(_sn, _code, _msg);
    }

    function handleRequestService(
        Types.TransferAssets memory transferAssets,
        string calldata _toNetwork,
        uint256 _sn
    ) private {
        string memory _toAddress = transferAssets.to;
        try this.checkParseAddress(_toAddress) {} catch Error(
            string memory err
        ) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                err,
                RC_ERR
            );
            return;
        } catch (bytes memory) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                "Invalid address format",
                RC_ERR
            );
            return;
        }

        Types.Asset[] memory _asset = transferAssets.asset;
        //TODO:what happens if a token asset during FA transfer is not registered??
        for (uint256 i = 0; i < _asset.length; i++) {
            // Check if the _toAddress is invalid
            uint256 _amount = _asset[i].value;
            string memory _tokenName = _asset[i].name;
            // Check if the token is registered already
            address _tokenaddr = tokenAddr[_tokenName];
            if (_tokenaddr == address(0)) {
                sendResponseMessage(
                    Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                    _toNetwork,
                    _sn,
                    "Unregistered Token",
                    RC_ERR
                );
                continue;
            }

            //TODO: send money to the user , try catch?
            //TODO: if there is no inital balance with the tokenBSH, this transfer will fail
            address token_addr = tokenAddr[_tokenName];
            ERC20(token_addr).approve(address(this), _amount);
            ERC20(token_addr).transferFrom(
                address(this),
                _toAddress.parseAddress(),
                _amount
            );
            /* balances[_toAddress.parseAddress()][_tokenName]
                .refundableBalance = balances[_toAddress.parseAddress()][
                _tokenName
            ]
                .refundableBalance
                .add(_amount);*/
        }

        sendResponseMessage(
            Types.ServiceType.RESPONSE_HANDLE_SERVICE,
            _toNetwork,
            _sn,
            "Transfer Success",
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
        emit HandleBTPMessageEvent(_sn, _code, _msg);
    }

    function handleResponseError(
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) private {
        uint256 sn = _sn;
        // Update locked balance of the caller
        address caller = requests[_sn].from.parseAddress();
        string memory _tokenName = requests[_sn].name;
        uint256 value = requests[_sn].value;
        uint256 fee = requests[_sn].fee;
        //todo: send money back to the user directly?
        address _tokenAddr = tokenAddr[_tokenName];
        balances[caller][_tokenName].refundableBalance = balances[caller][
            _tokenName
        ]
            .refundableBalance
            .add(value)
            .add(fee);

        balances[caller][_tokenName].lockedBalance = balances[caller][
            _tokenName
        ]
            .lockedBalance
            .sub(value);

        delete requests[_sn];
        emit TransferEnd(address(this), _sn, _code, _msg);
    }

    function checkParseAddress(string calldata _to) external {
        _to.parseAddress();
    }
}
