// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "./interfaces/IBMC.sol";
import "./interfaces/IMockBMC.sol";
import "./interfaces/IBMV.sol";
import "./interfaces/IBSH.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/BTPAddress.sol";
import "./libraries/Integers.sol";
import "./libraries/Strings.sol";
import "./libraries/Errors.sol";

contract MockBMC is IBMC, IMockBMC {
    using ParseAddress for address;
    using Integers for uint;
    using BTPAddress for string;
    using Strings for string;

    string private net;
    string private btpAddress;
    int256 private networkSn;
    uint256 private forward;
    uint256 private backward;
    string[] private responseList;

    constructor(
        string memory _net
    ) {
        net = _net;
        btpAddress = _net.btpAddress(address(this).toString());
    }

    function getBtpAddress(
    ) external view override returns (
        string memory
    ) {
        return btpAddress;
    }

    function getNetworkAddress(
    ) external view override returns (string memory) {
        return net;
    }

    function sendMessage(
        string memory _to,
        string memory _svc,
        int256 _sn,
        bytes memory _msg
    ) external override payable returns (
        int256
    ) {
        uint256 fee = _getFee(_to, _sn > 0);
        if (_sn < 0) {
            require(_removeResponse(toResponse(_to, _svc, uint256(_sn * -1))),
                Errors.BMC_REVERT_NOT_EXISTS_RESPONSE);
            fee = 0;
            _sn = 0;
        }
        require(msg.value >= fee, Errors.BMC_REVERT_NOT_ENOUGH_FEE);
        networkSn++;
        emit SendMessage(networkSn, _to, _svc, _sn, _msg);
        return networkSn;
    }

    function _getFee(
        string memory _to,
        bool _response
    ) internal view returns (
        uint256
    ) {
        if (_response) {
            return forward + backward;
        } else {
            return forward;
        }
    }

    function getFee(
        string memory _to,
        bool _response
    ) external view override returns (
        uint256
    ) {
        return _getFee(_to, _response);
    }

    function getNetworkSn(
    ) external view override returns (
        int256
    ) {
        return networkSn;
    }

    function setNetworkAddress(
        string memory _net
    ) external override {
        net = _net;
        btpAddress = _net.btpAddress(address(this).toString());
    }

    function setFee(
        uint256 _forward,
        uint256 _backward
    ) external override {
        forward = _forward;
        backward = _backward;
    }

    function handleRelayMessage(
        address _addr,
        string calldata _prev,
        uint256 _seq,
        bytes calldata _msg
    ) external override {
        bytes[] memory _ret = IBMV(_addr).handleRelayMessage(btpAddress, _prev, _seq, _msg);
        emit HandleRelayMessage(_ret);
    }

    event ErrorHandleRelayMessage(
        string err
    );

    function toResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) internal pure returns (
        string memory
    ) {
        return string(abi.encodePacked(_to, _svc, uint(_sn).toString()));
    }

    function getResponseIndex(
        string memory response
    ) internal view returns (
        uint256, bool
    ) {
        for (uint256 i = 0; i < responseList.length; i++) {
            if (responseList[i].compareTo(response)) {
                return (i, true);
            }
        }
        return (0, false);
    }

    function _addResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) internal {
        require(_sn > 0, "_sn should be positive");
        string memory response = toResponse(_to, _svc, _sn);
        (, bool ok) = getResponseIndex(response);
        if (!ok) {
            responseList.push(response);
        }
    }

    function addResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) external override {
        _addResponse(_to, _svc, _sn);
    }

    function hasResponse(
        string memory _to,
        string memory _svc,
        uint256 _sn
    ) external view override returns (
        bool
    ) {
        (, bool ok) = getResponseIndex(toResponse(_to, _svc, _sn));
        return ok;
    }

    function _removeResponse(
        string memory response
    ) internal returns (
        bool
    ) {
        (uint256 idx, bool ok) = getResponseIndex(response);
        if (ok) {
            string memory last = responseList[responseList.length - 1];
            responseList.pop();
            if (idx < responseList.length) {
                responseList[idx] = last;
            }
        }
        return ok;
    }

    function clearResponse(
    ) external override {
        responseList = new string[](0);
    }

    function handleBTPMessage(
        address _addr,
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external override {
        if (_sn > 0) {
            _addResponse(_from, _svc, _sn);
        }
        IBSH(_addr).handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function handleBTPError(
        address _addr,
        string memory _src,
        string memory _svc,
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) external override {
        IBSH(_addr).handleBTPError(_src, _svc, _sn, _code, _msg);
    }

}
