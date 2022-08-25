// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "../interfaces/IBMC.sol";
import "../interfaces/IBSH.sol";
import "../libraries/ParseAddress.sol";
import "../libraries/Integers.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract MockForBSH is IBMC, Initializable {
    using ParseAddress for address;
    using Integers for uint;

    string private net;
    string private btpAddress;

    function initialize(
        string memory _net
    ) public initializer {
        net = _net;
        btpAddress = string(abi.encodePacked("btp://", _net, "/", address(this).toString()));
    }

    function setNet(
        string memory _net
    ) external {
        net = _net;
        btpAddress = string(abi.encodePacked("btp://", _net, "/", address(this).toString()));
    }

    function getNet(
    ) external view returns (string memory) {
        return net;
    }

    function getBmcBtpAddress(
    ) external view override returns (string memory) {
        return btpAddress;
    }

    function sendMessage(
        string memory _to,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external override {
        emit Message(_to, _svc, _sn, _msg);
    }

    event Message(
        string _to,
        string _svc,
        uint256 _sn,
        bytes _msg
    );

    function intercallHandleBTPMessage(
        address _addr,
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external {
        try IBSH(_addr).handleBTPMessage(_from, _svc, _sn, _msg) {

        } catch Error(string memory err) {
            emit ErrorHandleBTPMessage(err);
        }
    }

    event ErrorHandleBTPMessage(
        string err
    );

    function intercallHandleBTPError(
        address _addr,
        string memory _src,
        string memory _svc,
        uint256 _sn,
        uint256 _code,
        string memory _msg
    ) external {
        try IBSH(_addr).handleBTPError(_src, _svc, _sn, _code, _msg) {

        } catch Error(string memory err) {
            emit ErrorHandleBTPError(err);
        }
    }

    event ErrorHandleBTPError(
        string err
    );
}
