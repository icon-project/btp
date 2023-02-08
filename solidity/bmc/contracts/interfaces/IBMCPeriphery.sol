// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/Types.sol";

interface IBMCPeriphery {
    //FIXME remove getBtpAddress
    /**
        @notice Get BMC BTP address
     */
    function getBtpAddress(
    ) external view returns (
        string memory
    );

    /**
        @notice Gets Network Address of BMC
     */
    function getNetworkAddress(
    ) external view returns (
        string memory
    );

    /**
        @notice Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
        @dev Caller must be a registered relayer.
        @param _prev    BTP Address of the BMC generates the message
        @param _msg     serialized bytes of Relay Message refer RelayMessage structure
     */
    function handleRelayMessage(
        string calldata _prev,
        bytes calldata _msg
    ) external;

    /**
        @notice Send the message to a specific network.
        @dev Caller must be an registered BSH.
        @param _to      Network Address of destination network
        @param _svc     Name of the service
        @param _sn      Serial number of the message, it should be positive
        @param _msg     Serialized bytes of Service Message
        @return nsn     Network serial number
     */
    function sendMessage(
        string calldata _to,
        string calldata _svc,
        int256 _sn,
        bytes calldata _msg
    ) external payable returns (
        int256 nsn
    );

    /**
        @notice (EventLog) Sends the message to the next BMC.
        @dev The relay monitors this event.
        @param _next String ( BTP Address of the BMC to handle the message )
        @param _seq Integer ( sequence number of the message from current BMC to the next )
        @param _msg Bytes ( serialized bytes of BTP Message )
    */
    event Message(string _next, uint256 _seq, bytes _msg);

    /*
        @notice Get status of BMC.
        @param _link        BTP Address of the connected BMC
        @return _status  The link status
     */
    function getStatus(
        string calldata _link
    ) external view returns (
        Types.LinkStatus memory _status
    );

    /**
        @notice (EventLog) Drop the message of the connected BMC
        @param _prev String ( BTP Address of the previous BMC )
        @param _seq  Integer ( sequence number of the message from connected BMC )
        @param _msg  Bytes ( serialized bytes of BTP Message )
        @param _ecode Integer ( error code )
        @param _emsg  String ( error message )
    */
    event MessageDropped(string _prev, uint256 _seq, bytes _msg, uint256 _ecode, string _emsg);

    /**
        @notice (EventLog) Logs the event that handle the message
        @dev The tracker monitors this event.
        @param _src String ( Network Address of source BMC )
        @param _nsn Integer ( Network serial number )
        @param _next String ( BTP Address of the BMC to handle the message )
        @param _event String ( Event )
     */
    event BTPEvent(string _src, int256 _nsn, string _next, string _event);

    /**
       @notice It returns the amount of claimable reward to the target
       @param _network String ( Network address to claim )
       @param _addr    Address ( Address of the relay )
       @return _reward Integer (The claimable reward to the target )
    */
    function getReward(
        string calldata _network,
        address _addr
    ) external view returns (
        uint256 _reward
    );

    /**
       @notice Sends the claim message to a given network if a claimable reward exists.
       @dev It expects a response, so it would use a positive serial number for the message.
       If _network is the current network then it transfers a reward and a sender pays nothing.
       If the <sender> is FeeHandler, then it transfers the remaining reward to the receiver.
       @param _network  String ( Network address to claim )
       @param _receiver String ( Address of the receiver of target chain )
    */
    function claimReward(
        string calldata _network,
        string calldata _receiver
    ) external payable;

    /**
       @notice (EventLog) Logs the claim.
       @dev If it claims the reward in it's own network,
            _network is current network and _nsn is zero.
       @param _sender Address ( Address of the sender )
       @param _network String ( Network address to claim )
       @param _receiver String ( Address of the receiver of target chain )
       @param _amount Integer ( Amount of reward to claim )
       @param _nsn  Integer ( Network serial number of the claim message )
    */
    event ClaimReward(address _sender, string _network, string _receiver, uint256 _amount, int256 _nsn);

    /**
       @notice (EventLog) Logs the result of claim at receiving the response or error.
       @dev _result : 0 for success, others for failure
       @param _sender Address ( Address of the sender )
       @param _network String ( Network address to claim )
       @param _nsn  Integer ( Network serial number of the claim message )
       @param _result Integer ( Result of processing )
    */
    event ClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result);

    /**
       @notice Gets the fee to the target network
       @dev _response should be true if it uses positive value for _sn of {@link #sendMessage}.
            If _to is not reachable, then it reverts.
            If _to does not exist in the fee table, then it returns zero.
       @param  _to       String ( BTP Network Address of the destination BMC )
       @param  _response Boolean ( Whether the responding fee is included )
       @return _fee      Integer (The fee of sending a message to a given destination network )
     */
    function getFee(
        string calldata _to,
        bool _response
    ) external view returns (
        uint256 _fee
    );

    function getNetworkSn(
    ) external view returns (
        int256
    );
}
