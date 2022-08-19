// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

interface IFixedFees {
    /**
       @notice Gets the fixed fee for the given network address and type.
       @dev If there is no mapping to the network address, `default` fee is returned.
       @param _net The network address
       @param _type The fee type ("relay" or "protocol")
       @return The fee amount in loop
     */
    function fixedFee(
        string calldata _net,
        string calldata _type
    ) external view returns (uint256);

    /**
       @notice Gets the total fixed fees for the given network address.
       @dev If there is no mapping to the network address, `default` fee is returned.
       @param _net The network address
       @return The total fees amount in loop
     */
    function totalFixedFees(
        string calldata _net
    ) external view returns (uint256);

    /**
       @notice Sets the fixed fees for the given network address.
       @dev Only the admin wallet can invoke this.
       @param _net The destination network address
       @param _relay The relay fee amount in loop
       @param _protocol The protocol fee amount in loop
     */
    function setFixedFees(
        string calldata _net,
        uint256 _relay,
        uint256 _protocol
    ) external;

    /**
       @notice Gets the total accrued fees for the given type.
       @param _type The fee type ("relay" or "protocol")
       @return The total accrued fees in loop
     */
    function accruedFees(
        string calldata _type
    ) external view returns (uint256);

    /**
       @notice Notifies the user that the fees have been successfully updated.
       @param _net The destination network address
       @param _relay The relay fee amount in loop
       @param _protocol The protocol fee amount in loop
     */
    event FixedFeesUpdated(
        string indexed _net,
        uint256 _relay,
        uint256 _protocol
    );
}
