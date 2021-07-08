// SPDX-License-Identifier: MIT

pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "./IBSH.sol";
import "../../../icondao/Libraries/TypesLib.sol";

/**
   @title Interface of BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
*/
interface IBSHImpl is IBSH {
    function hasPendingRequest() external view returns (bool);

    function sendServiceMessage(
        address _from,
        string memory _to,
        Types.Asset[] memory _assets
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
    function handleFeeGathering(string calldata _fa, string calldata _svc)
        external
        override;
}
