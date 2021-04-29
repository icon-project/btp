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

pragma solidity >=0.5.0 <=0.8.5;

import "../BEP20TokenBSH.sol";

contract BEP20Mock is BEP20TokenBSH {
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.Response;
    using RLPEncodeStruct for Types.TransferCoin;
    using ParseAddress for address;

    constructor(
        address bmc,
        string memory _serviceName,
        string memory _tokenName,
        string memory _symbol,
        uint8 _decimal
    )
        BEP20TokenBSH(
            bmc,
            _serviceName,
            _tokenName,
            _symbol,
            _decimal
        )
    {}

    function setBalance(address _tokenContract, uint256 _amt) external {
        _mint(_tokenContract, _amt);
    }

    function handleRequest(
        string memory _net,
        string memory _svc,
        string memory _from,
        address _to,
        string memory _tokenName,
        uint256 _value
    ) external {
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_TOKEN_TRANSFER,
                Types
                    .TransferCoin(_from, _to.toString(), _tokenName, _value)
                    .encodeData()
            )
                .encodeServiceMessage()
        );
    }

    function handleResponse(
        string memory _net,
        string memory _svc,
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) external {
        sendBTPMessage(
            _net,
            _svc,
            _sn,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .RESPONSE_HANDLE_SERVICE,
                Types.Response(_code, _msg).encodeResponse()
            )
                .encodeServiceMessage()
        );
    }

    function handleRequestWithStringAddress(
        string memory _net,
        string memory _svc,
        string memory _from,
        string memory _to,
        string memory _tokenName,
        uint256 _value
    ) external {
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_TOKEN_TRANSFER,
                Types.TransferCoin(_from, _to, _tokenName, _value).encodeData()
            )
                .encodeServiceMessage()
        );
    }

    function sendBTPMessage(
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) internal {
        this.handleBTPMessage(_from, _svc, _sn, _msg);
    }
}
