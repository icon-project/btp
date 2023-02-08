// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/Types.sol";

interface IBMCManagement {
    /**
       @notice Add the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Service's contract address
     */
    function addService(
        string memory _svc,
        address _addr
    ) external;

    /**
       @notice De-registers the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
     */
    function removeService(
        string calldata _svc
    ) external;

    /**
       @notice Get registered services.
       @return _servicers   An array of Service.
     */
    function getServices(
    ) external view returns (
        Types.Service[] memory _servicers
    );

    /**
       @notice Registers BMV for the network.
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
       @param _addr    Address of BMV
     */
    function addVerifier(
        string calldata _net,
        address _addr
    ) external;

    /**
       @notice De-registers BMV for the network.
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
     */
    function removeVerifier(
        string calldata _net
    ) external;

    /**
       @notice Get registered verifiers.
       @return _verifiers   An array of Verifier.
     */
    function getVerifiers(
    ) external view returns (
        Types.Verifier[] memory _verifiers
    );

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
     */
    function addLink(
        string calldata _link
    ) external;

    /**
       @notice Removes the link and status information.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
     */
    function removeLink(
        string calldata _link
    ) external;

    /**
       @notice Get registered links.
       @return _links   An array of links ( BTP Addresses of the BMCs ).
     */
    function getLinks(
    ) external view returns (
        string[] memory _links
    );

    /**
       @notice Registers relay for the network.
       @dev Caller must be an operator of BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     The address of relay
     */
    function addRelay(
        string calldata _link,
        address _addr
    ) external;

    /**
       @notice Unregisters Relay for the network.
       @dev Caller must be an operator of BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     The address of relay
     */
    function removeRelay(
        string calldata _link,
        address _addr
    ) external;

    /**
       @notice Get relays status by link.
       @param _link        BTP Address of the connected BMC.
       @return _relays list of address of relay
     */
    function getRelays(
        string calldata _link
    ) external view returns (
        address[] memory _relays
    );

    /**
       @notice Add route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     Network Address of the destination BMC
       @param _link    Network Address of the next BMC for the destination
     */
    function addRoute(
        string calldata _dst,
        string calldata _link
    ) external;

    /**
       @notice Remove route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     Network Address of the destination BMC
     */
    function removeRoute(
        string calldata _dst
    ) external;

    /**
       @notice Get routing information.
       @return _routes An array of Route.
     */
    function getRoutes(
    ) external view returns (
        Types.Route[] memory _routes
    );

    /**
       @notice Sets the fee table
       @dev Caller must be an operator of BTP network.
       @param _dst   String[] ( List of BTP Network Address of the destination BMC )
       @param _value Integer[][] ( List of lists of relay fees in the path including return path.
                     If it provides an empty relay fee list, then it removes the entry from the table. )
    */
    function setFeeTable(
        string[] memory _dst,
        uint256[][] memory _value
    ) external;

    /**
       @notice Gets the fee table
       @dev It reverts if the one of destination networks is not reachable.
            If there is no corresponding fee table, then it returns an empty list.
       @param  _dst      String[] ( List of BTP Network Address of the destination BMC )
       @return _feeTable Integer[][] ( List of lists of relay fees in the path including return path )
     */
    function getFeeTable(
        string[] calldata _dst
    ) external view returns (
        uint256[][] memory _feeTable
    );

    /**
       @notice Sets the address to handle the remaining reward fee.
       @dev Caller must be an operator of BTP network.
       @param _addr Address ( the address to handle the remaining reward fee )
    */
    function setFeeHandler(
        address _addr
    ) external;

    /**
       @notice Gets the address to handle the remaining reward fee.
       @return _addr Address ( the address to handle the remaining reward fee )
    */
    function getFeeHandler(
    ) external view returns (
        address _addr
    );

    /**
        @notice Drop the next message that to be relayed from a specific network
        @dev Called by the operator to manage the BTP network.
        @param _src  String ( Network Address of source BMC )
        @param _seq  Integer ( number of the message from connected BMC )
        @param _svc  String ( number of the message from connected BMC )
        @param _sn   Integer ( serial number of the message, must be positive )
        @param _nsn        Integer ( network serial number of the message )
        @param _feeNetwork String ( Network Address of the relay fee of the message )
        @param _feeValues  Integer[] ( list of relay fees of the message )
     */
    function dropMessage(
        string calldata _src,
        uint256 _seq,
        string calldata _svc,
        int256 _sn,
        int256 _nsn,
        string calldata  _feeNetwork,
        uint256[] memory _feeValues
    ) external;
}
