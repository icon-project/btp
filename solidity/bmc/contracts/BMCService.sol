// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBSH.sol";
import "./interfaces/ICCService.sol";
import "./interfaces/IOwnerManager.sol";
import "./interfaces/IBMCPeriphery.sol";
import "./interfaces/ICCManagement.sol";
import "./interfaces/ICCPeriphery.sol";
import "./libraries/Types.sol";
import "./libraries/Errors.sol";
import "./libraries/BTPAddress.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/Strings.sol";
import "./libraries/RLPDecodeStruct.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/Utils.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BMCService is IBSH, ICCService, Initializable {
    using BTPAddress for string;
    using ParseAddress for string;
    using Strings for string;
    using RLPDecodeStruct for bytes;
    using RLPEncodeStruct for Types.BTPMessage;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ResponseMessage;
    using RLPEncodeStruct for Types.ClaimMessage;
    using Utils for uint256[];

    address private bmcPeriphery;
    address private bmcManagement;
    string private network;

    mapping(string => mapping(string => mapping(int256 => Types.Response))) private responseMap;
    mapping(int256 => Types.Request) private requestMap;
    mapping(address => mapping(string => uint256)) private rewardMap;

    function initialize(
        address _bmcManagementAddr
    ) public initializer {
        bmcManagement = _bmcManagementAddr;
    }

    /**
       @notice Get address of BMCManagement.
       @return address of BMCManagement
     */
    function getBMCManagement(
    ) external view returns (
        address
    ) {
        return bmcManagement;
    }

    function requireBMCManagementAccess(
    ) internal view {
        require(msg.sender == bmcManagement, Errors.BMC_REVERT_UNAUTHORIZED);
    }

    /**
       @notice Update BMCPeriphery.
       @dev Caller must be an Owner of BTP network
       @param _addr    address of BMCPeriphery.
     */
    function setBMCPeriphery(
        address _addr
    ) external {
        require(IOwnerManager(bmcManagement).isOwner(msg.sender), Errors.BMC_REVERT_UNAUTHORIZED);
        require(_addr != address(0), Errors.BMC_REVERT_INVALID_ARGUMENT);
        bmcPeriphery = _addr;
        network = IBMCPeriphery(bmcPeriphery).getNetworkAddress();
    }

    /**
       @notice Get address of BMCPeriphery.
       @return address of BMCPeriphery
     */
    function getBMCPeriphery(
    ) external view returns (
        address
    ) {
        return bmcPeriphery;
    }

    function requireBMCPeripheryAccess(
    ) internal view {
        require(msg.sender == bmcPeriphery, Errors.BMC_REVERT_UNAUTHORIZED);
    }

    function _addReward(
        string memory net,
        address addr,
        uint256 amount
    ) internal {
        if (amount > 0) {
            rewardMap[addr][net] = rewardMap[addr][net] + amount;
        }
    }

    function _collectRemainFee(
        Types.FeeInfo memory feeInfo
    ) internal returns (
        Types.FeeInfo memory
    ){
        _addReward(feeInfo.network, bmcPeriphery, feeInfo.values.sumFromUints());
        feeInfo.values = new uint256[](0);
        return feeInfo;
    }

    function getReward(
        string calldata _network,
        address _addr
    ) external view override returns (uint256) {
        return rewardMap[_addr][_network];
    }

    function clearReward(
        string calldata _network,
        address _addr
    ) external override returns (uint256) {
        requireBMCPeripheryAccess();
        uint256 reward = rewardMap[_addr][_network];
        require(reward > 0, Errors.BMC_REVERT_NOT_EXISTS_REWARD);
        rewardMap[_addr][_network] = 0;
        return reward;
    }

    function _accumulateFee(
        address addr,
        Types.FeeInfo memory feeInfo
    ) internal returns (
        Types.FeeInfo memory
    ){
        if (feeInfo.values.length > 0) {
            _addReward(feeInfo.network, addr, feeInfo.values[0]);
            //pop first
            uint256[] memory nextValues = new uint256[](feeInfo.values.length - 1);
            for (uint256 i = 0; i < nextValues.length; i++) {
                nextValues[i] = feeInfo.values[i + 1];
            }
            feeInfo.values = nextValues;
        }
        return feeInfo;
    }

    function handleFee(
        address _addr,
        bytes memory _msg
    ) external override returns (
        Types.BTPMessage memory
    ) {
        requireBMCPeripheryAccess();
        Types.BTPMessage memory btpMsg = _msg.decodeBTPMessage();
        btpMsg.feeInfo = _accumulateFee(_addr, btpMsg.feeInfo);

        if (btpMsg.dst.compareTo(network)) {
            if (btpMsg.sn > 0) {
                _collectRemainFee(responseMap[btpMsg.src][btpMsg.svc][btpMsg.sn].feeInfo);
                responseMap[btpMsg.src][btpMsg.svc][btpMsg.sn] = Types.Response(
                    btpMsg.nsn, btpMsg.feeInfo);
            } else {
                btpMsg.feeInfo = _collectRemainFee(btpMsg.feeInfo);
            }
        }
        return btpMsg;
    }

    function handleErrorFee(
        string memory _src,
        int256 _sn,
        Types.FeeInfo memory _feeInfo
    ) external override returns (
        Types.FeeInfo memory
    ) {
        requireBMCPeripheryAccess();
        if (_sn > 0) {
            uint256 hop = ICCManagement(bmcManagement).getHop(_src);
            if (hop > 0 && _feeInfo.values.length > hop) {
                uint256 remainLen = _feeInfo.values.length - hop;
                uint256[] memory nextValues = new uint256[](_feeInfo.values.length);
                for (uint256 i = 0; i < hop; i++) {
                    nextValues[i] = _feeInfo.values[remainLen+i];
                }
                for (uint256 i = 0; i < remainLen; i++) {
                    nextValues[hop+i] = _feeInfo.values[i];
                }
                _feeInfo.values = nextValues;
            }
            return _feeInfo;
        } else {
            return _collectRemainFee(_feeInfo);
        }
    }

    function handleDropFee(
        string memory _network,
        uint256[] memory _values
    ) external override returns (
        Types.FeeInfo memory
    ) {
        requireBMCManagementAccess();
        return _accumulateFee(bmcPeriphery, Types.FeeInfo(_network, _values));
    }

    function addReward(
        string memory _network,
        address _addr,
        uint256 _amount
    ) external override {
        requireBMCPeripheryAccess();
        _addReward(_network, _addr, _amount);
    }

    function addRequest(
        int256 _nsn,
        string memory _dst,
        address _sender,
        uint256 _amount
    ) external override {
        requireBMCPeripheryAccess();
        requestMap[_nsn] = Types.Request(_nsn, _dst, _sender, _amount);
    }

    function removeResponse(
        string memory _to,
        string memory _svc,
        int256 _sn
    ) external override returns (
        Types.Response memory
    ) {
        requireBMCPeripheryAccess();
        Types.Response memory response = responseMap[_to][_svc][_sn];
        require(response.nsn > 0, Errors.BMC_REVERT_NOT_EXISTS_RESPONSE);
        delete responseMap[_to][_svc][_sn];
        return response;
    }

    function _handleResponse(
        int256 nsn,
        uint256 result
    ) internal {
        Types.Request memory request = requestMap[nsn];
        require(request.nsn > 0, Errors.BMC_REVERT_NOT_EXISTS_REQUEST);
        delete requestMap[request.nsn];
        if (result != Types.ECODE_NONE) {
            _addReward(request.dst, request.caller, request.amount);
        }
        ICCPeriphery(bmcPeriphery).emitClaimRewardResult(
            request.caller, request.dst, request.nsn, result);
    }

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        requireBMCPeripheryAccess();

        Types.BMCMessage memory bmcMsg = _msg.decodeBMCMessage();
        bytes32 msgType = keccak256(abi.encodePacked(bmcMsg.msgType));
        if (msgType == keccak256(abi.encodePacked(Types.BMC_INTERNAL_CLAIM))) {
            Types.ClaimMessage memory claimMsg = bmcMsg.payload.decodeClaimMessage();
            _addReward(
                    network,
                    claimMsg.receiver.parseAddress(Errors.BMC_REVERT_INVALID_ARGUMENT),
                    claimMsg.amount);
            IBMCPeriphery(bmcPeriphery).sendMessage(
                _from,
                Types.BMC_SERVICE,
                int256(_sn) * -1,
                Types.BMCMessage(Types.BMC_INTERNAL_RESPONSE,
                    Types.ResponseMessage(0, "").encodeResponseMessage()
                ).encodeBMCMessage());
        } else if (msgType == keccak256(abi.encodePacked(Types.BMC_INTERNAL_RESPONSE))) {
            _handleResponse(int256(_sn), bmcMsg.payload.decodeResponseMessage().code);
        } else if (msgType == keccak256(abi.encodePacked(Types.BMC_INTERNAL_LINK))) {
            ICCManagement(bmcManagement).addReachable(_from,
                bmcMsg.payload.decodePropagateMessage());
        } else if (msgType == keccak256(abi.encodePacked(Types.BMC_INTERNAL_UNLINK))) {
            ICCManagement(bmcManagement).removeReachable(_from,
                bmcMsg.payload.decodePropagateMessage());
        } else if (msgType == keccak256(abi.encodePacked(Types.BMC_INTERNAL_INIT))) {
            string[] memory l = bmcMsg.payload.decodeInitMessage();
            for(uint256 i = 0; i < l.length; i++) {
                ICCManagement(bmcManagement).addReachable(_from, l[i]);
            }
        } else {
            revert(Errors.BMC_REVERT_NOT_EXISTS_INTERNAL);
        }
    }

    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override {
        requireBMCPeripheryAccess();
        _handleResponse(int256(_sn), _code);
    }

    function decodeResponseMessage(
        bytes calldata _rlp
    ) external pure override returns (
        Types.ResponseMessage memory
    ) {
        return _rlp.decodeResponseMessage();
    }

}
