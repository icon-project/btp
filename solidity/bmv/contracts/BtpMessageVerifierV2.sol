// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

// TODO Support upgradable bmv
contract BtpMessageVerifierV2 is IBtpMessageVerifier, Initializable {

    using BlockUpdateLib for BlockUpdateLib.Header;
    using MessageProofLib for MessageProofLib.MessageProof;
    using RelayMessageLib for RelayMessageLib.RelayMessage;

    address public _bmc;
    bytes private _srcNetworkId;
    uint private _networkTypeId;
    uint private _networkId;
    uint private _height;
    bytes32 private _networkSectionHash;
    bytes32 private _messageRoot;
    uint private _messageCount;
    uint private _remainMessageCount;
    uint private _nextMessageSn;
    address[] private _validators;
    uint _sequenceOffset;

    modifier onlyBmc() {
        require(
            msg.sender == _bmc,
            "BtpMessageVerifier: Function must be called through known bmc"
        );
        _;
    }

    function initialize(
        address bmc,
        bytes memory srcNetworkId_,
        uint networkTypeId_,
        uint networkId_,
        bytes memory firstBlockUpdate
    )
    external
    {
        _bmc = bmc;
        _srcNetworkId = srcNetworkId_;
        _networkTypeId = networkTypeId_;
        _networkId = networkId_;

        BlockUpdateLib.Header memory bu = BlockUpdateLib.decodeHeader(firstBlockUpdate);
        _height = bu.mainHeight;
        _nextMessageSn = bu.messageSn;
        _messageCount = _remainMessageCount = bu.messageCount;
        _messageRoot = bu.messageRoot;
        _networkSectionHash = bu.getNetworkSectionHash();
        _validators = bu.nextValidators;
    }

    function getStatus() external view returns (uint, uint, uint, uint) {
        return (_height, 0, 0, 0);
    }

    // NOTE: Using bytes message instead of base64url during development
    function handleRelayMessage(
        string memory bmc_,
        string memory prev_,
        uint seq,
        bytes memory _msg
    ) external returns (bytes[] memory) {
    }

    function srcNetworkId() public view returns (bytes memory) {
        return _srcNetworkId;
    }

    function networkTypeId() public view returns (uint) {
        return _networkTypeId;
    }

    function networkId() public view returns (uint) {
        return _networkId;
    }

    function height() public view returns (uint) {
        return _height;
    }

    function networkSectionHash() public view returns (bytes32) {
        return _networkSectionHash;
    }

    function messageRoot() public view returns (bytes32) {
        return _messageRoot;
    }

    function messageCount() public view returns (uint) {
        return _messageCount;
    }

    function remainMessageCount() public view returns (uint) {
        return _remainMessageCount;
    }

    function nextMessageSn() public view returns (uint) {
        return _nextMessageSn;
    }

    function validators(uint nth) public view returns (address) {
        return _validators[nth];
    }

    function validatorsCount() public view returns (uint) {
        return _validators.length;
    }

}

