// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/Types.sol";

interface ICCManagement {
    /**
        @notice Get address of BSH.
        @param _svc (String) Name of the service
        @return address of BSH
     */
    function getService(
        string memory _svc
    ) external view returns (
        address
    );

    /**
        @notice Get address of BMV.
        @param _net (String) Network Address of the blockchain
        @return address of BMV
     */
    function getVerifier(
        string memory _net
    ) external view returns (
        address
    );

    /**
       @notice Gets the fee to the target network
       @dev _response should be true if it uses positive value for _sn of {@link #sendMessage}.
            If _to is not reachable, then it reverts.
            If _to does not exist in the fee table, then it returns zero.
       @param  _to       String ( BTP Network Address of the destination BMC )
       @param  _response Boolean ( Whether the responding fee is included )
       @return _fee      Integer (The fee of sending a message to a given destination network )
       @return _values   []Integer (The fee of sending a message to a given destination network )
     */
    function getFee(
        string calldata _to,
        bool _response
    ) external view returns (
        uint256 _fee,
        uint256[] memory _values
    );

    /**
       @notice Checking whether one specific address is registered relay.
       @dev Caller can be ANY
       @param _link BTP Address of the connected BMC
       @param _addr Address needs to verify.
       @return whether one specific address is registered relay
     */
    function isLinkRelay(
        string calldata _link,
        address _addr
    ) external view returns (
        bool
    );

    /**
        @notice resolve next BMC.
        @param _dst   network address of destination
        @return _next BTP address of next BMC
     */
    function resolveNext(
        string memory _dst
    ) external view returns (
        string memory _next
    );

    function addReachable(
        string memory _from,
        string memory _reachable
    ) external;

    function removeReachable(
        string memory _from,
        string memory _reachable
    ) external;

    function getHop(
        string memory _dst
    ) external view returns (
        uint256
    );
}
