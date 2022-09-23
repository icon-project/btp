// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../libraries/Types.sol";

interface IBMCPeriphery {
    /**
        @notice Get BMC BTP address
     */
    function getBmcBtpAddress() external view returns (string memory);

    /**
        @notice Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
        @dev Caller must be a registered relayer.
        @param _prev    BTP Address of the BMC generates the message
        @param _msg     serialized bytes of Relay Message refer RelayMessage structure
     */
    function handleRelayMessage(string calldata _prev, bytes calldata _msg)
        external;

    /**
        @notice Send the message to a specific network.
        @dev Caller must be an registered BSH.
        @param _to      Network Address of destination network
        @param _svc     Name of the service
        @param _sn      Serial number of the message, it should be positive
        @param _msg     Serialized bytes of Service Message
     */
    function sendMessage(
        string calldata _to,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external;

    /*
        @notice Get status of BMC.
        @param _link        BTP Address of the connected BMC
        @return _linkStats  The link status
     */
    function getStatus(string calldata _link)
        external
        view
        returns (Types.LinkStats memory _linkStats);

    /**
        @notice Send the message to a specific network.
        @dev Caller must be an BMCManagement.
        @param _next    next BMC's BTP address
        @param _seq     a sequence number to keep track of BTP messages
        @param _msg     Serialized bytes of BMCService Message
     */
    function sendInternal(
        string memory _next,
        uint256 _seq,
        bytes memory _msg
    ) external;

    /**
        @notice Drop the message of the connected BMC
        @dev Caller must be an BMCManagement.
        @param _src  String ( BTP Address of source BMC to drop )
        @param _link String ( BTP Address of previous BMC to drop )
        @param _seq  Integer ( Sequence number to drop )
        @param _svc  String ( Name of the service to drop )
        @param _sn   Integer ( Serial number of the message to drop )
        @param _txSeq  Integer ( Sequence number of the error message to send to source )
    */
    function dropMessage(
        string memory _src,
        string memory _link,
        uint256 _seq,
        string memory _svc,
        uint256 _sn,
        uint256 _txSeq
    ) external;
}
