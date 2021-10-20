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
pragma experimental ABIEncoderV2;
// import "../TokenBSH.sol";
import "../Interfaces/IBSHImpl.sol";
import "./BMC.sol";

contract BMCMock is BMC {
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.Response;
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.TransferAssets;
    using RLPEncodeStruct for Types.BMCMessage;
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    IBSHImpl private bsh;

    constructor(string memory _network) BMC(_network) {}

    function setBSH(address _bsh) external {
        bsh = IBSHImpl(_bsh);
    }

    function sendBTPMessage(
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) internal {
        bsh.handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function handleFeeGathering(string calldata _fa, string memory _svc)
        external
    {
        bsh.handleFeeGathering(_fa, _svc);
    }

    Types.Asset[] public assetsMock;

    function handleTransferReq(
        string memory _from,
        address _to,
        string memory _net,
        string memory _svc,
        string memory _tokenName,
        uint256 _value
    ) external {
        assetsMock.push(Types.Asset(_tokenName, _value, 0));
        bytes memory _ta = Types
            .TransferAssets(_from, _to.toString(), assetsMock)
            .encodeTransferAsset();
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(Types.ServiceType.REQUEST_TOKEN_TRANSFER, _ta)
                .encodeServiceMessage()
        );
        delete assetsMock;
    }

    Types.Asset[] public tokensMock;

    function handleTransferReqStrAddr(
        string memory _from,
        string memory _to,
        string memory _net,
        string memory _svc,
        string memory _tokenName,
        uint256 _value
    ) external {
        tokensMock.push(Types.Asset(_tokenName, _value, 0));
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                    Types.ServiceType.REQUEST_TOKEN_TRANSFER,
                    Types
                        .TransferAssets(_from, _to, tokensMock)
                        .encodeTransferAsset()
                )
                .encodeServiceMessage()
        );
        delete tokensMock;
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
                    Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                    Types.Response(_code, _msg).encodeResponse()
                )
                .encodeServiceMessage()
        );
    }

    function handleUnknownBTPResponse(
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
                    Types.ServiceType.RESPONSE_UNKNOWN,
                    Types.Response(_code, _msg).encodeResponse()
                )
                .encodeServiceMessage()
        );
    }

    function handleInvalidBTPResponse(
        string memory _net,
        string memory _svc,
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) external {
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                    Types.ServiceType.RESPONSE_INVALID,
                    Types.Response(_code, _msg).encodeResponse()
                )
                .encodeServiceMessage()
        );
    }

    function response(
        Types.ServiceType _serviceType,
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external {
        sendBTPMessage(
            _from,
            _svc,
            _sn,
            Types
                .ServiceMessage(
                    _serviceType,
                    Types.Response(_code, _msg).encodeResponse()
                )
                .encodeServiceMessage()
        );
    }

    function buildBTPRespMessage(
        string memory _from,
        string memory _to,
        string memory _svc,
        int256 _sn,
        uint256 _code,
        string memory _msg
    ) external view returns (bytes memory) {
        return
            Types
                .BMCMessage(
                    _from,
                    _to,
                    _svc,
                    _sn,
                    Types
                        .ServiceMessage(
                            Types.ServiceType.RESPONSE_HANDLE_SERVICE,
                            Types.Response(_code, _msg).encodeResponse()
                        )
                        .encodeServiceMessage()
                )
                .encodeBMCMessage();
    }

    function buildBTPInvalidRespMessage(
        string memory _from,
        string memory _to,
        string memory _svc,
        int256 _sn,
        uint256 _code,
        string memory _msg
    ) external view returns (bytes memory) {
        return
            Types
                .BMCMessage(
                    _from,
                    _to,
                    _svc,
                    int256(0),
                    Types
                        .ServiceMessage(
                            Types.ServiceType.RESPONSE_INVALID,
                            Types.Response(_code, _msg).encodeResponse()
                        )
                        .encodeServiceMessage()
                )
                .encodeBMCMessage();
    }
}
