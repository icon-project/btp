// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../xcall/interfaces/ICallService.sol";
import "../xcall/interfaces/ICallServiceReceiver.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract DAppProxySample is ICallServiceReceiver, Initializable {
    address private callSvc;
    string private callSvcBtpAddr;
    uint256 private lastId;

    struct RollbackData {
        uint256 id;
        bytes rollback;
        uint256 ssn;
    }
    mapping(uint256 => RollbackData) private rollbacks;

    modifier onlyCallService() {
        require(msg.sender == callSvc, "OnlyCallService");
        _;
    }

    function initialize(
        address _callService
    ) public initializer {
        callSvc = _callService;
        callSvcBtpAddr = ICallService(callSvc).getBtpAddress();
    }

    function compareTo(
        string memory _base,
        string memory _value
    ) internal pure returns (bool) {
        if (
            keccak256(abi.encodePacked(_base)) ==
            keccak256(abi.encodePacked(_value))
        ) {
            return true;
        }
        return false;
    }

    function sendMessage(
        string calldata _to,
        bytes calldata _data,
        bytes calldata _rollback
    ) external payable {
        if (_rollback.length > 0) {
            uint256 id = ++lastId;
            bytes memory encodedRd = abi.encode(id, _rollback);
            uint256 sn = ICallService(callSvc).sendCallMessage{value:msg.value}(
                _to,
                _data,
                encodedRd
            );
            rollbacks[id] = RollbackData(id, _rollback, sn);
        } else {
            ICallService(callSvc).sendCallMessage{value:msg.value}(
                _to,
                _data,
                _rollback
            );
        }
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
    ) external override onlyCallService {
        if (compareTo(_from, callSvcBtpAddr)) {
            // handle rollback data here
            // In this example, just compare it with the stored one.
            (uint256 id, bytes memory received) = abi.decode(_data, (uint256, bytes));
            RollbackData memory stored = rollbacks[id];
            require(compareTo(string(received), string(stored.rollback)), "rollbackData mismatch");
            delete rollbacks[id]; // cleanup
            emit RollbackDataReceived(_from, stored.ssn, received);
        } else {
            // normal message delivery
            string memory msgData = string(_data);
            if (compareTo("revertMessage", msgData)) {
                revert("revertFromDApp");
            }
            emit MessageReceived(_from, _data);
        }
    }

    event MessageReceived(
        string _from,
        bytes _data
    );

    event RollbackDataReceived(
        string _from,
        uint256 _ssn,
        bytes _rollback
    );
}
