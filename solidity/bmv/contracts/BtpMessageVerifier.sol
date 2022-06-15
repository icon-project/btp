// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";

contract BtpMessageVerifier is IBtpMessageVerifier {

    using BlockUpdateLib for BlockUpdateLib.BlockUpdate;
    using MessageProofLib for MessageProofLib.MessageProof;
    using RelayMessageLib for RelayMessageLib.RelayMessage;

    address public immutable bmc;

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

    modifier onlyBmc() {
        require(
            msg.sender == bmc,
            "Function must be called through known bmc"
        );
        _;
    }

    constructor() {
        bmc = address(0);
    }

    // TODO move to constructor
    function initialize(
        bytes memory srcNetworkId,
        uint networkTypeId,
        uint networkId,
        bytes memory firstBlockUpdate
    )
    external
    {
        _srcNetworkId = srcNetworkId;
        _networkTypeId = networkTypeId;
        _networkId = networkId;

        BlockUpdateLib.BlockUpdate memory bu = BlockUpdateLib.decode(firstBlockUpdate);
        _height = bu.mainHeight;
        _nextMessageSn = bu.messageSn;
        _messageCount = _remainMessageCount = bu.messageCount;
        _messageRoot = bu.messageRoot;
        _networkSectionHash = bu.getNetworkSectionHash();
        _validators = bu.nextValidators;

    }

    function getStatus() external view returns (uint) {
        return _height;
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint _seq,
        bytes memory _msg
    ) external returns (bytes[] memory) {
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);

        bytes[] memory messages;
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
                if (_remainMessageCount != 0) {
                    continue;
                }
                BlockUpdateLib.BlockUpdate memory bu = rms[i].toBlockUpdate();
                checkBlockUpdateWithState(bu);
                checkBlockUpdateProof(bu);

                _height = bu.mainHeight;
                _networkSectionHash = bu.getNetworkSectionHash();
                if (bu.hasNextValidators) {
                    _validators = bu.nextValidators;
                }
                if (bu.messageRoot != bytes32(0)) {
                    _messageRoot = bu.messageRoot;
                    _remainMessageCount = _messageCount = bu.messageCount;
                }

            } else if (rms[i].typ == RelayMessageLib.TypeMessageProof) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();

                // compare roots of `block update` and `message proof`
                (bytes32 root, uint leafCount) = mp.calculate();
                require(root == _messageRoot, "");
                require(leafCount == _messageCount, "");

                // collect messages
                messages = Utils.append(messages, mp.mesgs);

                // update state
                _remainMessageCount -= mp.mesgs.length;
                _nextMessageSn += mp.mesgs.length;

            }
        }
        return messages;
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

    function hasQuorumOf(uint votes) private view returns (bool) {
        return votes * 3 > _validators.length * 2;
    }

    function checkBlockUpdateWithState(BlockUpdateLib.BlockUpdate memory bu) private view {
        require(_networkId == bu.networkId, "invalid network id");
        require(_networkSectionHash == bu.prevNetworkSectionHash,
                "invalid previous network section hash");
        require(_nextMessageSn == bu.messageSn, "invalid message sequence number");
    }

    function checkBlockUpdateProof(BlockUpdateLib.BlockUpdate memory bu) private view {
        uint votes = 0;
        bytes32 decision = bu.getNetworkTypeSectionDecisionHash(_srcNetworkId, _networkTypeId);
        for (uint j = 0; j < bu.signatures.length && !hasQuorumOf(votes); j++) {
            address signer = Utils.recoverSigner(decision, bu.signatures[j]);
            if (signer == _validators[j]) {
                votes++;
            }
        }
        require(hasQuorumOf(votes), "lack of quorum");
    }

}

