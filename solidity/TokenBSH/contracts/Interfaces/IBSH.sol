pragma solidity >=0.5.0 <=0.8.0;

interface IBSH {
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
    ) external;

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
    ) external;

    /**
     @notice BSH handle Gather Fee Message request from BMC contract
     @dev Caller must be BMC contract only
     @param _fa     A BTP address of fee aggregator
     @param _svc    A name of the service
    */
    function handleFeeGathering(string calldata _fa, string calldata _svc)
        external;
}
