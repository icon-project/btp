// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../libraries/Types.sol";

interface ICCService {

    /**
       @notice It returns the amount of claimable reward to the target
       @param _network String ( Network address to claim )
       @param _addr    Address ( Address of the relay )
       @return _reward Integer (The claimable reward to the target )
    */
    function getReward(
        string calldata _network,
        address _addr
    ) external view returns (uint256 _reward);

    function handleFee(
        address _addr,
        bytes memory _msg
    ) external returns (
        Types.BTPMessage memory
    );

    function handleErrorFee(
        string memory _src,
        int256 _sn,
        Types.FeeInfo memory _feeInfo
    ) external returns (
        Types.FeeInfo memory
    );

    function handleDropFee(
        string memory _network,
        uint256[] memory _values
    ) external returns (
        Types.FeeInfo memory
    );

    function addReward(
        string memory _network,
        address _addr,
        uint256 _amount
    ) external;

    function clearReward(
        string calldata _network,
        address _addr
    ) external returns (
        uint256
    );

    function addRequest(
        int256 _nsn,
        string memory _dst,
        address _sender,
        uint256 _amount
    ) external;

    function removeResponse(
        string memory _to,
        string memory _svc,
        int256 _sn
    ) external returns (
        Types.Response memory
    );

    function decodeResponseMessage(
        bytes calldata _rlp
    ) external pure returns (
        Types.ResponseMessage memory
    );

}
