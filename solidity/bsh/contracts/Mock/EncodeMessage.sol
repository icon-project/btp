// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Libraries/ParseAddressLib.sol";
import "../Libraries/RLPEncodeStructLib.sol";
import "../Libraries/RLPDecodeStructLib.sol";
import "../Libraries/StringsLib.sol";
import "../Libraries/TypesLib.sol";

contract EncodeMessage {
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferCoin;
    using RLPEncodeStruct for Types.Response;
    using RLPEncodeStruct for Types.GatherFeeMessage;
    using RLPEncodeStruct for Types.BMCService;
    using RLPEncodeStruct for string[];
    using ParseAddress for address;
    using ParseAddress for string;
    using Strings for string;

    function encodeTransferFeesBMCMessage(
        string memory _fromBMC,
        string memory _toBMC,
        string memory _toAddress,
        string memory _svc,
        int256 _sn,
        address _bsh,
        Types.Asset[] memory _fees
    ) external view returns (bytes memory) {
        (, string memory _to) = _toAddress.splitBTPAddress();
        return
            Types
                .BMCMessage(
                _fromBMC,
                _toBMC,
                _svc,
                _sn,
                encodeServiceMessage(_bsh.toString(), _to, _fees)
            )
                .encodeBMCMessage();
    }

    function encodeBMCService(
        string calldata _fa,
        string[] memory _svcs
    ) external view returns (bytes memory) {
        return Types.BMCService(
            "FeeGathering",
            Types.GatherFeeMessage(
                _fa,
                _svcs
            ).encodeGatherFeeMessage()
        ).encodeBMCService();
    }

    function encodeResponseBMCMessage(
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
                this.encodeResponseMsg(Types.ServiceType.REPONSE_HANDLE_SERVICE, _code, _msg)
            )
                .encodeBMCMessage();
    }

    function hashCoinName(string memory _coinName)
        external
        view
        returns (uint256)
    {
        return uint256(keccak256(abi.encodePacked(_coinName)));
    }

    function encodeResponseMsg(
        Types.ServiceType _serviceType,
        uint256 _code,
        string calldata _msg
    ) external view returns (bytes memory) {
        return Types
                .ServiceMessage(
                _serviceType,
                Types.Response(_code, _msg).encodeResponse()
            )
                .encodeServiceMessage();
    }

    function encodeBatchTransferMsgWithAddress(
        string calldata _from,
        address _to,
        Types.Asset[] memory _assets
    ) external view returns (bytes memory) {
        return encodeServiceMessage(_from, _to.toString(), _assets);
    }

    function encodeBatchTransferMsgWithStringAddress(
        string calldata _from,
        string calldata _to,
        Types.Asset[] memory _assets
    ) external view returns (bytes memory) {
        return encodeServiceMessage(_from, _to, _assets);
    }

    function encodeTransferMsgWithAddress(
        string calldata _from,
        address _to,
        string memory _coinName,
        uint256 _value
    ) external view returns (bytes memory) {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        return encodeServiceMessage(_from, _to.toString(), asset);
    }

    function encodeTransferMsgWithStringAddress(
        string calldata _from,
        string calldata _to,
        string calldata _coinName,
        uint256 _value
    ) external view returns (bytes memory) {
        Types.Asset[] memory asset = new Types.Asset[](1);
        asset[0] = Types.Asset(_coinName, _value);
        return encodeServiceMessage(_from, _to, asset);
    }

    function encodeServiceMessage(
        string memory _from,
        string memory _to,
        Types.Asset[] memory _asset
    ) private pure returns (bytes memory) {
        return Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types.TransferCoin(_from, _to, _asset).encodeTransferCoinMsg()
            )
                .encodeServiceMessage();
    }
}