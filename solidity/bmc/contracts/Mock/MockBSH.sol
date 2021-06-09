// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../Interfaces/IBSH.sol";

contract MockBSH is IBSH {
    constructor() {}

    /**
       @notice Handle BTP Message from other blockchain.
       @dev Accept the message only from the BMC. 
       Every BSH must implement this function
       @param _from    Network Address of source network
       @param _svc     Name of the service
       @param _sn      Serial number of the message
       @param _msg     Serialized bytes of ServiceMessage
   */

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override {
        require(_sn != 1000, "Mocking error message on handleBTPMessage");
    }

    /**
       @notice Handle the error on delivering the message.
       @dev Accept the error only from the BMC.
       Every BSH must implement this function
       @param _src     BTP Address of BMC generates the error
       @param _svc     Name of the service
       @param _sn      Serial number of the original message
       @param _code    Code of the error
       @param _msg     Message of the error  
   */
    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override {
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
