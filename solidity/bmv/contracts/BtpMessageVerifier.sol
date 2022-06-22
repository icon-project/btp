// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BtpMessageVerifier is IBtpMessageVerifier, Initializable {

    using BlockUpdateLib for BlockUpdateLib.BlockUpdate;
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

    modifier onlyBmc() {
        require(
            msg.sender == _bmc,
            "BtpMessageVerifier: Unauthorized bmc sender"
        );
        _;
    }

    function initialize(
        address bmc,
        bytes memory srcNetworkId,
        uint networkTypeId,
        uint networkId,
        bytes memory firstBlockUpdate
    )
    initializer
    external
    {
        _bmc = bmc;
        _srcNetworkId = srcNetworkId;
        _networkTypeId = networkTypeId;
        _networkId = networkId;

        BlockUpdateLib.BlockUpdate memory bu = BlockUpdateLib.decode(firstBlockUpdate);
        _height = bu.mainHeight;
        _nextMessageSn = bu.messageSn;
        _messageCount = _remainMessageCount = bu.messageCount;
        _messageRoot = bu.messageRoot;
        _networkSectionHash = bu.getNetworkSectionHash();
        require(bu.nextValidators.length > 0, "BtpMessageVerifier: No validator(s)");
        _validators = bu.nextValidators;
    }

    function getStatus() external view returns (uint) {
        return _height;
    }

    // NOTE: Using bytes message instead of base64url during development
    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint _seq,
        //string memory _msg
        bytes memory _msg
    )
    external
    onlyBmc
    returns (bytes[] memory) {
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);

        checkAllowedNetwork(_prev);

        bytes[] memory messages;
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
                require(_remainMessageCount == 0, "BtpMessageVerifier: has messages to be handled");

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
                require(root == _messageRoot, "BtpMessageVerifier: Invalid merkle root of messages");
                require(leafCount == _messageCount, "BtpMessageVerifier: Invalid message count");

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

    function checkAllowedNetwork(string memory srcAddr) private view {
        require(
            keccak256(abi.encodePacked(_srcNetworkId)) == keccak256(abi.encodePacked(bytes(srcAddr))),
            "BtpMessageVerifier: Not allowed source network"
        );
    }

    function checkBlockUpdateWithState(BlockUpdateLib.BlockUpdate memory bu) private view {
        require(_networkId == bu.networkId, "BtpMessageVerifier: BlockUpdate for unknown network");
        require(_networkSectionHash == bu.prevNetworkSectionHash,
                "BtpMessageVerifier: Invalid previous network section hash");
        require(_nextMessageSn == bu.messageSn, "BtpMessageVerifier: Invalid message sequence number");
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
        require(hasQuorumOf(votes), "BtpMessageVerifier: Lack of quorum");
    }

}

