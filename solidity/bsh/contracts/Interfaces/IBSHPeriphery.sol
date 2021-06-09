// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./IBSH.sol";
/**
   @title Interface of BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
*/
interface IBSHPeriphery is IBSH {
    struct AssetTransferDetail {
        string _coinName;
        uint256 _value;
        uint256 _fee;
    }
    
    /**
     @notice Send Service Message from BSHCore contract to BMCService contract
     @dev Caller must be BSHCore only
     @param _to           A network address of destination chain
     @param _coinName     A coin name that is requested to transfer    
     @param _value        An amount to receive at destination chain
     @param _fee          An amount of charging fee 
    */
    function sendServiceMessage(
        address _from,
        string calldata _to,
        string memory _coinName,
        uint256 _value,
        uint256 _fee
    ) external;

    /**
     @notice BSH handle BTP Message from BMC contract
     @dev Caller must be BMC contract only
     @param _from    An originated network address of a request
     @param _svc     A service name of BSHPeriphery contract     
     @param _sn      A serial number of a service request 
     @param _msg     An RLP message of a service request/service response
    */
    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external override;

    /**
     @notice BSH handle BTP Error from BMC contract
     @dev Caller must be BMC contract only 
     @param _svc     A service name of BSHPeriphery contract     
     @param _sn      A serial number of a service request 
     @param _code    A response code of a message (RC_OK / RC_ERR)
     @param _msg     A response message
    */
    function handleBTPError(
        string calldata _src,
        string calldata _svc,
        uint256 _sn,
        uint256 _code,
        string calldata _msg
    ) external override;

    /**
     @notice BSH handle Gather Fee Message request from BMC contract
     @dev Caller must be BMC contract only
     @param _fa     A BTP address of fee aggregator
     @param _svc    A name of the service
    */
    function handleFeeGathering(
        string calldata _fa,
        string calldata _svc
    ) external override;
}
