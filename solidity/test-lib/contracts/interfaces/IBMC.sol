// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

interface IBMC {
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
        @notice Send the message to a specific network.
        @dev Caller must be an registered BSH.
        @param _to      Network Address of destination network
        @param _svc     Name of the service
        @param _sn      Serial number of the message, it should be positive
        @param _msg     Serialized bytes of Service Message
        @return nsn     Network serial number
     */
    function sendMessage(
        string memory _to,
        string memory _svc,
        int256 _sn,
        bytes memory _msg
    ) external payable returns (
        int256
    );

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
        string memory _to,
        bool _response
    ) external view returns (
        uint256 _fee
    );

    function getNetworkSn(
    ) external view returns (
        int256
    );

}
