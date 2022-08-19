// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

interface ICallService {
    /*======== At the source CALL_BSH ========*/
    /**
       @notice Sends a call message to the contract on the destination chain.
       @dev Only allowed to be called from the contract.
       @param _to The BTP address of the callee on the destination chain
       @param _data The calldata specific to the target contract
       @param _rollback (Optional) The data for restoring the caller state when an error occurred
       @return The serial number of the request
     */
    function sendCallMessage(
        string calldata _to,
        bytes calldata _data,
        bytes calldata _rollback
    ) external payable returns (uint256);

    /**
       @notice Notifies the user that a rollback operation is required for the request '_sn'.
       @param _sn The serial number of the previous request
       @param _rollback The data for recovering that was given by the caller
       @param _reason The error message that caused this rollback
     */
    event RollbackMessage(
        uint256 indexed _sn,
        bytes _rollback,
        string _reason
    );

    /**
       @notice Rollbacks the caller state of the request '_sn'.
       @dev Caller should be ...
       @param _sn The serial number of the previous request
     */
    function executeRollback(
        uint256 _sn
    ) external;

    /*======== At the destination CALL_BSH ========*/
    /**
       @notice Notifies the user that a new call message has arrived.
       @param _from The BTP address of the caller on the source chain
       @param _to A string representation of the callee address
       @param _sn The serial number of the request from the source
       @param _reqId The request id of the destination chain
       @param _data The calldata
     */
    event CallMessage(
        string indexed _from,
        string indexed _to,
        uint256 indexed _sn,
        uint256 _reqId,
        bytes _data
    );

    /**
       @notice Executes the requested call.
       @dev Caller should be ...
       @param _reqId The request Id
     */
    function executeCall(
        uint256 _reqId
    ) external;
}
