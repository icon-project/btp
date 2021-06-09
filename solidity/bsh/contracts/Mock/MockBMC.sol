// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../BSHPeriphery.sol";
import "./BMC.sol";

contract MockBMC is BMC {
    struct RelayInfo {
        address r;
        uint cb;
        uint rh;
    }

    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.Response;
    using RLPEncodeStruct for string[];
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    RelayInfo private relay;

    BSHPeriphery private bsh;
    constructor(string memory _network) BMC(_network) {}

    function setBSH(address _bsh) external {
        bsh = BSHPeriphery(_bsh);
    }

    function sendBTPMessage(
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) internal {
        bsh.handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function gatherFee(
        string calldata _fa,
        string calldata _svc
    ) external {
        bsh.handleFeeGathering(_fa, _svc);
    }

    function transferBatchStringAddress(
        string calldata _src,
        string memory _dst,
        string memory _svc,
        uint256 _sn,
        string memory _from,
        string memory _to,
        Types.Asset[] memory assets
    ) external {
        handleMessage(
            _src,
            Types
                .BMCMessage(
                _src,
                _dst,
                _svc,
                int256(_sn),
                Types
                    .ServiceMessage(
                    Types
                        .ServiceType
                        .REQUEST_COIN_TRANSFER,
                    Types
                        .TransferCoin(_from, _to, assets)
                        .encodeTransferCoinMsg()
                    )
                    .encodeServiceMessage()
                ) 
        );
    }

    function transferBatchAddress(
        string calldata _src,
        string memory _dst,
        string memory _svc,
        uint256 _sn,
        string memory _from,
        address _to,
        Types.Asset[] memory assets
    ) external {
        handleMessage(
            _src,
            Types
                .BMCMessage(
                _src,
                _dst,
                _svc,
                int256(_sn),
                Types
                    .ServiceMessage(
                    Types
                        .ServiceType
                        .REQUEST_COIN_TRANSFER,
                    Types
                        .TransferCoin(_from, _to.toString(), assets)
                        .encodeTransferCoinMsg()
                    )
                    .encodeServiceMessage()
                ) 
        );
    }

    function transferRequestAddress(
        string calldata _src,
        string memory _dst,
        string memory _svc,
        uint256 _sn,
        string memory _from,
        address _to,
        string memory _coinName,
        uint256 _value
    ) external {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        handleMessage(
            _src,
            Types
                .BMCMessage(
                _src,
                _dst,
                _svc,
                int256(_sn),
                Types
                    .ServiceMessage(
                    Types
                        .ServiceType
                        .REQUEST_COIN_TRANSFER,
                    Types
                        .TransferCoin(_from, _to.toString(), asset)
                        .encodeTransferCoinMsg()
                    )
                    .encodeServiceMessage()
                ) 
        );
    }

    function transferRequestStringAddress(
        string calldata _src,
        string memory _dst,
        string memory _svc,
        uint256 _sn,
        string memory _from,
        string memory _to,
        string memory _coinName,
        uint256 _value
    ) external {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        handleMessage(
            _src,
            Types
                .BMCMessage(
                _src,
                _dst,
                _svc,
                int256(_sn),
                Types
                    .ServiceMessage(
                    Types
                        .ServiceType
                        .REQUEST_COIN_TRANSFER,
                    Types
                        .TransferCoin(_from, _to, asset)
                        .encodeTransferCoinMsg()
                    )
                    .encodeServiceMessage()
                ) 
        );
    }

    function transferRequestWithAddress(
        string calldata _net,
        string calldata _svc,
        string memory _from,
        address _to,
        string memory _coinName,
        uint256 _value
    ) external {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types
                    .TransferCoin(_from, _to.toString(), asset)
                    .encodeTransferCoinMsg()
            )
                .encodeServiceMessage()
        );
    }

    function transferRequestWithStringAddress(
        string calldata _net,
        string calldata _svc,
        string memory _from,
        string memory _to,
        string memory _coinName,
        uint256 _value
    ) external {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types.TransferCoin(_from, _to, asset).encodeTransferCoinMsg()
            )
                .encodeServiceMessage()
        );
    }

    function response(
        Types.ServiceType _serviceType,
        string calldata _from,
        string calldata _svc,
        uint _sn,
        uint _code,
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

    function encodeBMCMessage(
        string memory _from,
        string memory _to,
        string memory _svc,
        int256 _sn,
        uint256 _code,
        string memory _msg
    ) 
        external
        view
        returns (bytes memory) 
    {
        return Types.BMCMessage(
            _from,
            _to,
            _svc,
            _sn,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REPONSE_HANDLE_SERVICE,
                Types
                    .Response(_code, _msg)
                    .encodeResponse()
                )
                .encodeServiceMessage()
        ).encodeBMCMessage();
    }

    function encodeTransferCoin(
        string memory _from,
        string memory _toNetwork,
        string memory _toAddress,
        string memory _svc,
        int256 _sn,
        address _bsh,
        Types.Asset[] memory _fees
    ) 
        external
        view
        returns (bytes memory) 
    { 
        ( , string memory _to) = _toAddress.splitBTPAddress();
        return Types.BMCMessage(
            _from,
            _toNetwork,
            _svc,
            _sn,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types
                    .TransferCoin(
                        _bsh.toString(),
                        _to,
                        _fees
                    ).encodeTransferCoinMsg()
                )
                .encodeServiceMessage()
        ).encodeBMCMessage();
    }

    function hashCoinName(string memory _coinName)
        external
        view
        returns (uint256)
    {
        return uint256(keccak256(abi.encodePacked(_coinName)));
    }

    function getBalance(address _addr) external view returns (uint) {
        return _addr.balance;
    }

    function relayRotation(
        string memory _link,
        uint _relayMsgHeight,
        bool hasMsg
    ) external
    {
        address r = rotateRelay(
                _link, 
                block.number,
                _relayMsgHeight,
                hasMsg
        );
        Types.Link memory link = links[_link];
        relay = RelayInfo(
            r,
            block.number,
            link.rotateHeight
        );    
    }

    function getRelay() external view returns (RelayInfo memory) {
        return relay;
    }

    function mineOneBlock() external {}
}