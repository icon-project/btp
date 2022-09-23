// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "./interfaces/IBMC.sol";
import "./interfaces/IBMV.sol";
import "./interfaces/IBSH.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/Integers.sol";

contract MockBMC is IBMC {
    using ParseAddress for address;
    using Integers for uint;

    string private net;
    string private btpAddress;

    constructor(
        string memory _net
    ) {
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

    function handleRelayMessage(
        address _addr,
        string calldata _prev,
        uint256 _seq,
        bytes calldata _msg
    ) external {
        try IBMV(_addr).handleRelayMessage(btpAddress, _prev, _seq, _msg) returns (
            bytes[] memory _ret
        ){
            emit HandleRelayMessage(_ret);
        } catch Error(string memory err) {
            emit ErrorHandleRelayMessage(err);
        } catch (bytes memory _err) {
            emit ErrorHandleRelayMessage("Unknown");
        }
    }

    event HandleRelayMessage(
        bytes[] _ret
    );

    event ErrorHandleRelayMessage(
        string err
    );

    function sendMessage(
        string memory _to,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external override {
        emit SendMessage(_to, _svc, _sn, _msg);
    }

    event SendMessage(
        string _to,
        string _svc,
        uint256 _sn,
        bytes _msg
    );

    function handleBTPMessage(
        address _addr,
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external {
        try IBSH(_addr).handleBTPMessage(_from, _svc, _sn, _msg) {

        } catch Error(string memory err) {
            emit ErrorHandleBTPMessage(err);
        } catch (bytes memory _err) {
            emit ErrorHandleBTPMessage("Unknown");
        }
    }

    event ErrorHandleBTPMessage(
        string err
    );

    function handleBTPError(
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
        } catch (bytes memory _err) {
            emit ErrorHandleBTPError("Unknown");
        }
    }

    event ErrorHandleBTPError(
        string err
    );
}
