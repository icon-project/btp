// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

interface ICallServiceReceiver {
    /**
       @notice Handles the call message received from the source chain.
       @dev Only called from the Call Message Service.
       @param _from The BTP address of the caller on the source chain
       @param _data The calldata delivered from the caller
     */
    function handleCallMessage(
        string calldata _from,
        bytes calldata _data
    ) external;
}
