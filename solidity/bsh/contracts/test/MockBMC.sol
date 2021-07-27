// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "./BMC.sol";

contract MockBMC is BMC {
    constructor(string memory _network) BMC(_network) {}

    function receiveRequest(
        string calldata _src,
        string memory _dst,
        string memory _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external {
        handleMessage(
            _src,
            Types.BMCMessage(_src, _dst, _svc, int256(_sn), _msg)
        );
    }

    function receiveResponse(
        string calldata _from,
        string memory _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external {
        IBSH(bshServices[_svc]).handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function getBalance(address _addr) external view returns (uint256) {
        return _addr.balance;
    }
}
