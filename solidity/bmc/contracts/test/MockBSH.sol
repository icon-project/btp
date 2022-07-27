// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../interfaces/IBSH.sol";

contract MockBSH is IBSH {
    constructor() {}

    function handleBTPMessage(
        string calldata,
        string calldata,
        uint256 _sn,
        bytes calldata
    ) external pure override {
        require(_sn != 1000, "Mocking error message on handleBTPMessage");
    }

    function handleBTPError(
        string calldata,
        string calldata,
        uint256 _sn,
        uint256,
        string calldata
    ) external pure override {
        require(_sn != 1000, "Mocking error message on handleBTPError");
        assert(_sn != 100); // mocking invalid opcode
    }

    /**
       @notice Handle Gather Fee Request from ICON.
       @dev Every BSH must implement this function
       @param _fa    BTP Address of Fee Aggregator in ICON
       @param _svc   Name of the service
   */
    function handleFeeGathering(string calldata _fa, string calldata _svc)
        external
        override
    {}
}
