// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

interface IFeeManage {
    /**
       @notice Sets the address of FeeHandler.
               If _addr is null (default), it accrues protocol fees.
               If _addr is a valid address, it transfers accrued fees to the address and
               will also transfer the receiving fees hereafter.
       @dev Only the admin wallet can invoke this.
       @param _addr (Address) The address of FeeHandler
     */
    function setProtocolFeeHandler(
        address _addr
    ) external;

    /**
       @notice Gets the current protocol fee handler address.
       @return (Address) The protocol fee handler address
     */
    function getProtocolFeeHandler(
    ) external view returns (
        address
    );

    /**
       @notice Sets the protocol fee amount.
       @dev Only the admin wallet can invoke this.
       @param _value (Integer) The protocol fee amount in loop
     */
    function setProtocolFee(
        uint256 _value
    ) external;

    /**
       @notice Gets the current protocol fee amount.
       @return (Integer) The protocol fee amount in loop
     */
    function getProtocolFee(
    ) external view returns (
        uint256
    );

    /**
       @notice Gets the fee for delivering a message to the _net.
               If the sender is going to provide rollback data, the _rollback param should set as true.
               The returned fee is the sum of the protocol fee and the relay fee.
       @param _net (String) The destination network address
       @param _rollback (Bool) Indicates whether it provides rollback data
       @return (Integer) the sum of the protocol fee and the relay fee
     */
    function getFee(
        string memory _net,
        bool _rollback
    ) external view returns (
        uint256
    );
}
