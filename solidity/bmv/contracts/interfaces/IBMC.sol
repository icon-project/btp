// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;

interface IBMC {
    /**
       @notice Registers BMV for the network. 
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
       @param _addr    Address of BMV
     */
    function addVerifier(string calldata _net, address _addr) external;
}
