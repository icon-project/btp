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

import "./icondao/Interfaces/IBSH.sol";
import "./icondao/Interfaces/IBMC.sol";
import "./icondao/Libraries/TypesLib.sol";
 
import "@openzeppelin/contracts/utils/math/SafeMath.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./icondao/Libraries/RLPEncodeStructLib.sol";
import "./icondao/Libraries/RLPDecodeStructLib.sol";
import "./icondao/Libraries/StringsLib.sol";
import "./icondao/Libraries/ParseAddressLib.sol";
import "./icondao/Libraries/Owner.sol";

contract TokenBSH is IBSH, ERC20, Owner {
    using SafeMath for uint256;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.Response;
    using RLPDecodeStruct for bytes;
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    IBMC private bmc;
    mapping(address => bool) owners;
    mapping(string => address) tokenAddr;
    mapping(address => string) tokenName;
    mapping(address => mapping(string => Types.Balance)) private balances;
    event AddOwner(address indexed owner);
    event RemoveOwner(address indexed owner);
    event Register(string indexed name, address addr);
    mapping(uint256 => Types.Record) public requests;
    string[] tokens;
    uint32 noOfOwners;
    uint256 constant minReqOwner = 1;
    uint256 private serialNo;
    uint256 private numOfTokens;
    string public serviceName;

    uint256 private constant RC_OK = 0;
    uint256 private constant RC_ERR = 1;

    event HandleBTPMessageEvent(uint256 _sn, uint256 _code, string _msg);
    event TransferStart(
        address indexed _from,
        string _to,
        uint256 _sn,
        string _tokenName,
        uint256 _value
    );

    event TransferEnd(
        address indexed _from,
        uint256 _sn,
        uint256 _code,
        string _response
    );

    constructor(address _bmc, string memory _serviceName,string memory _tokenName,
        string memory _symbol)
        ERC20(_tokenName, _symbol)
        Owner()
    {
        bmc = IBMC(_bmc);
        serviceName = _serviceName;
        serialNo = 0;
    }

    modifier onlyBMC {
        require(msg.sender == address(bmc), "Only BMC");
        _;
    }

    function register(string calldata name, address addr) external owner {
        require(
            tokenAddr[name] == address(0),
            "Token with same name exists already."
        );
        tokenAddr[name] = addr;
        tokenName[addr] = name;
        tokens.push(name);
        numOfTokens++;
        emit Register(name, addr);
    }

    function tokenNames() external view returns (string[] memory _names) {
        _names = new string[](numOfTokens);
        uint256 temp = 0;
        for (uint256 i = 0; i < tokens.length; i++) {
            if (tokenAddr[tokens[i]] != address(0)) {
                _names[temp] = tokens[i];
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
            this.balanceOf(_owner),
            balances[_owner][_tokenName].lockedBalance
        );
    }

    function transfer(
        string calldata _tokenName,
        uint256 _value,
        string calldata _to
    ) external {
        address token_addr = tokenAddr[_tokenName];
        require(token_addr != address(0), "Token is not registered");
        require(_value > 0, "Invalid amount specified.");
        this.transferFrom(msg.sender, address(this), _value);
        sendServiceMessage(_to, _tokenName, _value);
    }

    function sendServiceMessage(
        string memory _to,
        string memory _tokenName,
        uint256 _value
    ) private {
        balances[msg.sender][_tokenName].lockedBalance = _value.add(
            balances[msg.sender][_tokenName].lockedBalance
        );

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
                    .TransferCoin(
                    address(msg.sender).toString(),
                    _toAddress,
                    _tokenName,
                    _value
                )
                    .encodeData()
            )
                .encodeServiceMessage();

        bmc.sendMessage(_toNetwork, serviceName, serialNo, serviceMessage);
        // Add request in the pending map
        requests[serialNo] = Types.Record(
            Types.TransferCoin(
                address(msg.sender).toString(),
                _to,
                _tokenName,
                _value
            ),
            Types.Response(0, ""),
            false
        );

        emit TransferStart(
            msg.sender,
            _to,
            balances[msg.sender][_tokenName].lockedBalance,
            _tokenName,
            _value
        );
        serialNo++;
    }

    //TODO: add onlyBMC later when integrated with BMC
    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        Types.ServiceMessage memory _sm = _msg.decodeServiceMessage();
        if (_sm.serviceType == Types.ServiceType.REQUEST_TOKEN_TRANSFER) {
            Types.TransferCoin memory _tc = _sm.data.decodeData();
            handleRequestService(_tc.coinName, _tc.to, _from, _tc.value, _sn);
            return;
        } else if (
            _sm.serviceType == Types.ServiceType.RESPONSE_HANDLE_SERVICE
        ) {
            Types.Response memory response = _sm.data.decodeResponse();
            address caller = requests[_sn].request.from.parseAddress();
            string memory _tokenName = requests[_sn].request.coinName;
            uint256 value = requests[_sn].request.value;
            if (response.code == 1) {
                handleResponseError(_sn, response.code, response.message);
            } else {
                // Update locked balance for the response OK & burn the amount transfered from BSH
                balances[caller][_tokenName].lockedBalance = balances[caller][
                    _tokenName
                ]
                    .lockedBalance
                    .sub(value);
                _burn(address(this), value);
                // Update the response message in requests and mark it resolved
                requests[_sn] = Types.Record(
                    requests[_sn].request,
                    Types.Response(response.code, response.message),
                    true
                );
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
        this.transferFrom(address(this), msg.sender, _value);
    }

    function handleRequestService(
        string memory _tokenName,
        string memory _toAddress,
        string calldata _toNetwork,
        uint256 _amount,
        uint256 _sn
    ) private {
        // Check if the _toAddress is invalid
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
        // Check if the token is registered already
        address tokenaddr = tokenAddr[_tokenName];
        if (tokenaddr == address(0)) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                "Unregistered Token",
                RC_ERR
            );
            return;
        }
        // Mint token for the _toAddress
        try this.tryToMintToken(_toAddress.parseAddress(), _amount) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                "Transfer Success",
                RC_OK
            );
        } catch Error(string memory err) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                err,
                RC_ERR
            );
        } catch (bytes memory) {
            sendResponseMessage(
                Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                _toNetwork,
                _sn,
                "Transfer failed",
                RC_ERR
            );
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
        string memory _tokenName = requests[_sn].request.coinName;
        uint256 value = requests[_sn].request.value;
        try this.tryToTransferCoin(caller, value) {} catch Error(
            string memory
        ) {
            balances[caller][_tokenName].refundableBalance = balances[caller][
                _tokenName
            ]
                .refundableBalance
                .add(value);
            emit HandleBTPMessageEvent(
                _sn,
                _code,
                "Transfer Token Failed"
            );
        } catch (bytes memory) {
            balances[caller][_tokenName].refundableBalance = balances[caller][
                _tokenName
            ]
                .refundableBalance
                .add(value);
            emit HandleBTPMessageEvent(
                _sn,
                _code,
                "Transfer Token Failed"
            );
        }
        balances[caller][_tokenName].lockedBalance = balances[caller][
            _tokenName
        ]
            .lockedBalance
            .sub(value);

        // Update the request with error message and change it to resolved
        requests[_sn] = Types.Record(
            requests[_sn].request,
            Types.Response(_code, _msg),
            true
        );
    }

    function checkParseAddress(string calldata _to) external {
        _to.parseAddress();
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

    function tryToTransferCoin(address _to, uint256 _amount) external {
        require(msg.sender == address(this), "Only BSH");
        this.approve(address(this), _amount);
        this.transferFrom(address(this), _to, _amount);
    }

    function tryToMintToken(address _to, uint256 _amount) external {
        require(msg.sender == address(this), "Only BSH");
        _mint(_to, _amount);
    }    
}
