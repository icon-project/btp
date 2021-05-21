// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Interfaces/NativeCoinBSH.sol";
import "../BMC.sol";

contract MockBMC is BMC {
    struct RelayInfo {
        address r;
        uint cb;
        uint rh;
    }

    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.GatherFeeMessage;
    using RLPEncodeStruct for Types.BMCService;
    using RLPEncodeStruct for Types.Response;
    using ParseAddress for address;

    RelayInfo private relay;

    NativeCoinBSH private bsh;
    constructor(string memory _network) BMC(_network) {}

    // function setBSH(address _bsh) external {
    //     bsh = NativeCoinBSH(_bsh);
    // }

    function sendBTPMessage(
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) internal {
        NativeCoinBSH(bshServices[_svc]).handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function gatherFee(
        string calldata _from,
        string calldata _msgType,
        string calldata _svcType,
        string calldata _fa,
        string[] memory _svcs
    ) external {
        handleMessage(
            _from,
            Types.BMCMessage(
                _from,
                bmcAddress,
                _msgType,
                0,
                Types.BMCService(
                    _svcType,
                    Types.GatherFeeMessage(
                        _fa,
                        _svcs
                    ).encodeGatherFeeMessage()
                ).encodeBMCService()
            )
        );
    }

    function transferRequestWithAddress(
        string memory _net,
        string memory _svc,
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
        string memory _net,
        string memory _svc,
        string memory _from,
        string memory _to,
        string memory _coinName,
        uint256 _value
    ) external {
        // string memory _net = "1234.iconee";
        // string memory _svc = "Token";
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

    function getLink(string memory btpAddr)
        external
        view
        returns (Types.Link memory)
    {
        return links[btpAddr];
    }


    function mineOneBlock() external {}
}