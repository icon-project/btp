// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../xcall/interfaces/ICallService.sol";
import "../xcall/interfaces/ICallServiceReceiver.sol";
import "../xcall/libraries/Types.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract DAppProxySample is ICallServiceReceiver, Initializable {
    address private callSvc;
    mapping(uint256 => Types.CallRequest) private requests;

    function initialize(
        address _callService
    ) public initializer {
        callSvc = _callService;
    }

    function sendMessage(
        string calldata _to,
        bytes calldata _data,
        bytes calldata _rollback
    ) external payable {
        uint256 sn =  ICallService(callSvc).sendCallMessage{value:msg.value}(
            _to,
            _data,
            _rollback
        );
        requests[sn] = Types.CallRequest(
            msg.sender,
            _to,
            _rollback,
            false
        );
    }

    /**
       @notice Handles the call message received from the source chain.
       @dev Only called from the Call Message Service.
       @param _from The BTP address of the caller on the source chain
       @param _data The calldata delivered from the caller
     */
    function handleCallMessage(
        string calldata _from,
        bytes calldata _data
    ) external override {
        emit MessageReceived(_from, _data);
    }

    event MessageReceived(
        string _from,
        bytes _data
    );
}
