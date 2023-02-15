// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

interface ICallService {
    /**
       @notice Get BTP address of Call Service
     */
    function getBtpAddress(
    ) external view returns (
        string memory
    );

    /*======== At the source CALL_BSH ========*/
    /**
       @notice Sends a call message to the contract on the destination chain.
       @param _to The BTP address of the callee on the destination chain
       @param _data The calldata specific to the target contract
       @param _rollback (Optional) The data for restoring the caller state when an error occurred
       @return The serial number of the request
     */
    function sendCallMessage(
        string memory _to,
        bytes memory _data,
        bytes memory _rollback
    ) external payable returns (
        uint256
    );

    /**
       @notice Notifies that the requested call message has been sent.
       @param _from The chain-specific address of the caller
       @param _to The BTP address of the callee on the destination chain
       @param _sn The serial number of the request
       @param _nsn The network serial number of the BTP message
     */
    event CallMessageSent(
        address indexed _from,
        string indexed _to,
        uint256 indexed _sn,
        int256 _nsn
    );

    /**
       @notice Notifies that a response message has arrived for the `_sn` if the request was a two-way message.
       @param _sn The serial number of the previous request
       @param _code The execution result code
                    (0: Success, -1: Unknown generic failure, >=1: User defined error code)
       @param _msg The result message if any
     */
    event ResponseMessage(
        uint256 indexed _sn,
        int _code,
        string _msg
    );

    /**
       @notice Notifies the user that a rollback operation is required for the request '_sn'.
       @param _sn The serial number of the previous request
     */
    event RollbackMessage(
        uint256 indexed _sn
    );

    /**
       @notice Rollbacks the caller state of the request '_sn'.
       @param _sn The serial number of the previous request
     */
    function executeRollback(
        uint256 _sn
    ) external;

    /**
       @notice Notifies that the rollback has been executed.
       @param _sn The serial number for the rollback
       @param _code The execution result code
                    (0: Success, -1: Unknown generic failure)
       @param _msg The result message if any
     */
    event RollbackExecuted(
        uint256 indexed _sn,
        int _code,
        string _msg
    );

    /*======== At the destination CALL_BSH ========*/
    /**
       @notice Notifies the user that a new call message has arrived.
       @param _from The BTP address of the caller on the source chain
       @param _to A string representation of the callee address
       @param _sn The serial number of the request from the source
       @param _reqId The request id of the destination chain
     */
    event CallMessage(
        string indexed _from,
        string indexed _to,
        uint256 indexed _sn,
        uint256 _reqId
    );

    /**
       @notice Executes the requested call message.
       @param _reqId The request id
     */
    function executeCall(
        uint256 _reqId
    ) external;

    /**
       @notice Notifies that the call message has been executed.
       @param _reqId The request id for the call message
       @param _code The execution result code
                    (0: Success, -1: Unknown generic failure)
       @param _msg The result message if any
     */
    event CallExecuted(
        uint256 indexed _reqId,
        int _code,
        string _msg
    );
}
