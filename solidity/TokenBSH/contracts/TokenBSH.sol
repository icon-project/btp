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
    string[] tokenList;
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
        tokenList.push(_name);
        numOfTokens++;
        emit Register(_name, _addr);
    }

    function tokenNames() external view returns (string[] memory _names) {
        _names = new string[](numOfTokens);
        uint256 temp = 0;
        for (uint256 i = 0; i < tokenList.length; i++) {
            if (tokenAddr[tokenList[i]] != address(0)) {
                _names[temp] = tokenList[i];
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

    Types.Asset[] _feeAssets;

    function getAccumulatedFees() external view returns (Types.Asset[] memory) {
        return _feeAssets;
    }

    function handleGatherFee(string memory _toFA) external {
        sendServiceMessage(_toFA, _feeAssets);
        //delete the fees accumulated after sending to the FA
        delete _feeAssets;
        //todo: when receiving error from the handleresponse, put back the fee collected into feeCollector
    }

    Types.Asset[] public transferAssets;

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
        (_value, _fee) = _calculateTransferFee(token_addr, _value);
        balances[msg.sender][_tokenName].lockedBalance = _value.add(
            balances[msg.sender][_tokenName].lockedBalance
        );
        //to empty the transfer assets as its a public variable
        if (transferAssets.length > 0) {
            delete transferAssets;
        }
        transferAssets.push(Types.Asset(_tokenName, _value, _fee));
        sendServiceMessage(_to, transferAssets);
    }

    function sendServiceMessage(
        string memory _to,
        Types.Asset[] storage transferAssets
    ) private {
        // Send Service Message to BMC
        string memory _toNetwork;
        string memory _toAddress;
        (_toNetwork, _toAddress) = _to.splitBTPAddress();

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
                    transferAssets
                )
                    .encodeTransferAsset()
            )
                .encodeServiceMessage();

        bmc.sendMessage(_toNetwork, serviceName, serialNo, serviceMessage);

        // Add request in the pending map
        Types.TransferAssetsRecord storage record = requests[serialNo];
        record.request.asset = transferAssets;
        record.request.from = address(msg.sender).toString();
        record.request.to = _toAddress;
        record.response = Types.Response(0, "");
        record.isResolved = false;
        emit TransferStart(msg.sender, _to, serialNo, record.request.asset);
        serialNo++;
    }

    //TODO: add onlyBMC later when integrated with BMC
    function getAssets(uint256 _sn)
        public
        view
        returns (Types.Asset[] memory assets)
    {
        assets = requests[_sn].request.asset;
    }

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        Types.ServiceMessage memory _sm = _msg.decodeServiceMessage();
        if (_sm.serviceType == Types.ServiceType.REQUEST_TOKEN_TRANSFER) {
            Types.TransferAssets memory _tc = _sm.data.decodeTransferAsset();
            handleRequestService(_tc, _from, _sn);
            return;
        } else if (
            _sm.serviceType == Types.ServiceType.RESPONSE_HANDLE_SERVICE
        ) {
            Types.Response memory response = _sm.data.decodeResponse();
            address caller = requests[_sn].request.from.parseAddress();
            if (response.code == 1) {
                handleResponseError(_sn, response.code, response.message);
            } else {
                // Update locked balance for the response OK & burn the amount transfered from BSH
                Types.Asset[] memory _assets = getAssets(_sn);
                for (uint256 i = 0; i < _assets.length; i++) {
                    string memory _tokenName = _assets[i].name;
                    uint256 value = _assets[i].value;
                    uint256 fee = _assets[i].fee;
                    balances[caller][_tokenName].lockedBalance = balances[
                        caller
                    ][_tokenName]
                        .lockedBalance
                        .sub(value);
                    feeCollector[_tokenName] = feeCollector[_tokenName].add(
                        fee
                    );
                    _feeAssets.push(
                        Types.Asset(_tokenName, feeCollector[_tokenName], 0)
                    );
                    address _tokenaddr = tokenAddr[_tokenName];
                    // Update the response message in requests and mark it resolved
                    requests[_sn].response = Types.Response(
                        response.code,
                        response.message
                    );
                    requests[_sn].isResolved = true;
                }
            }

            emit TransferEnd(caller, _sn, response.code, response.message);
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
        handleResponseError(_sn, _code, _msg);
    }

    function handleGatherFee(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string memory _toFA
    ) external {
        try this.checkParseAddress(_toFA) {} catch Error(string memory err) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toFA,
                _sn,
                err,
                RC_ERR
            );
            return;
        } catch (bytes memory) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toFA,
                _sn,
                "Invalid address format",
                RC_ERR
            );
            return;
        }
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
            // Mint token for the _toAddress
            balances[_toAddress.parseAddress()][_tokenName]
                .refundableBalance = balances[_toAddress.parseAddress()][
                _tokenName
            ]
                .refundableBalance
                .add(_amount);
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
        // Update locked balance of the caller
        address caller = requests[_sn].request.from.parseAddress();

        Types.Asset[] memory _assets = requests[_sn].request.asset;
        for (uint256 i = 0; i < _assets.length; i++) {
            string memory _tokenName = _assets[i].name;
            uint256 value = _assets[i].value;
            uint256 fee = _assets[i].fee;

            address _tokenAddr = tokenAddr[_tokenName];
            balances[caller][_tokenName].refundableBalance = balances[caller][
                _tokenName
            ]
                .refundableBalance
                .add(value);
            balances[caller][_tokenName].refundableBalance = balances[caller][
                _tokenName
            ]
                .refundableBalance
                .add(fee);

            balances[caller][_tokenName].lockedBalance = balances[caller][
                _tokenName
            ]
                .lockedBalance
                .sub(value);

            // Update the request with error message and change it to resolved
            requests[_sn].response = Types.Response(_code, _msg);
            requests[_sn].isResolved = true;
        }
    }

    function checkParseAddress(string calldata _to) external {
        _to.parseAddress();
    }
}
