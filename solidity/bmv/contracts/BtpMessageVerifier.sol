// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "./interfaces/IBMV.sol";
import "./libraries/Errors.sol";
import "./libraries/String.sol";
import "./libraries/RLPEncode.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BtpMessageVerifier is IBMV, Initializable {
    using BlockUpdateLib for Header;
    using MessageProofLib for MessageProof;
    using RelayMessageLib for RelayMessage;
    using String for string;

    address private bmc;
    string private srcNetworkId;
    uint256 private networkTypeId;
    uint256 private networkId;
    uint256 private height;
    bytes32 private networkSectionHash;
    bytes32 private messageRoot;
    uint256 private messageCount;
    uint256 private nextMessageSn;
    address[] private validators;
    uint256 private sequenceOffset;
    uint256 private firstMessageSn;

    string private constant ERR_UNAUTHORIZED = "bmv: Unauthorized";
    string private constant ERR_INVALID_ARGS = "bmv: InvalidArgs";

    modifier onlyBmc() {
        require(msg.sender == bmc, ERR_UNAUTHORIZED);
        _;
    }

    function initialize(
        address _bmc,
        string memory _srcNetworkId,
        uint256 _networkTypeId,
        bytes memory _firstBlockHeader,
        uint256 _sequenceOffset
    ) external initializer {
        bmc = _bmc;
        srcNetworkId = _srcNetworkId;
        networkTypeId = _networkTypeId;
        sequenceOffset = _sequenceOffset;

        Header memory header = BlockUpdateLib.decodeHeader(_firstBlockHeader);
        networkId = header.networkId;
        height = header.mainHeight;
        nextMessageSn = header.messageSn;
        firstMessageSn = header.messageSn;
        messageCount = header.messageCount;
        messageRoot = header.messageRoot;
        networkSectionHash = header.getNetworkSectionHash();
        require(header.nextValidators.length > 0, Errors.ERR_UNKNOWN);
        validators = header.nextValidators;
    }

    function getStatus() external view returns (uint256, bytes memory) {
        bytes[] memory extra = new bytes[](3);
        extra[0] = RLPEncode.encodeUint(sequenceOffset);
        extra[1] = RLPEncode.encodeUint(firstMessageSn);
        extra[2] = RLPEncode.encodeUint(messageCount);
        return (height, RLPEncode.encodeList(extra));
    }

    function handleRelayMessage(
        string memory,
        string memory _prev,
        uint256 _sn,
        bytes memory _msg
    ) external onlyBmc returns (bytes[] memory) {
        checkAccessible(_prev);
        uint256 _sequenceOffset = sequenceOffset;
        require(_sn >= _sequenceOffset, ERR_INVALID_ARGS);
        checkNextMessageSn(_sn - _sequenceOffset);
        RelayMessage[] memory rms = RelayMessageLib.decode(_msg);
        bytes[] memory messages;
        uint256 _messageCount = messageCount;
        uint256 _firstMessageSn = firstMessageSn;
        uint256 remainMessageCount = _messageCount - (nextMessageSn - _firstMessageSn);
        for (uint256 i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TYPE_BLOCK_UPDATE) {
                require(remainMessageCount == 0, Errors.ERR_UNKNOWN);
                (Header memory header, Proof memory proof) = rms[i].toBlockUpdate();
                checkHeaderWithState(header);
                checkBlockUpdateProof(header, proof);

                // update state
                height = header.mainHeight;
                networkSectionHash = header.getNetworkSectionHash();
                if (header.hasNextValidators) {
                    validators = header.nextValidators;
                }
                if (header.messageRoot != bytes32(0)) {
                    uint256 messageSn = _firstMessageSn + _messageCount;
                    if (messageSn < header.messageSn) {
                        revert(Errors.ERR_NOT_VERIFIABLE);
                    } else if (messageSn > header.messageSn) {
                        revert(Errors.ERR_ALREADY_VERIFIED);
                    }
                    messageRoot = header.messageRoot;
                    firstMessageSn = header.messageSn;
                    remainMessageCount = messageCount = header.messageCount;
                }
            } else if (rms[i].typ == RelayMessageLib.TYPE_MESSAGE_PROOF) {
                MessageProof memory mp = rms[i].toMessageProof();

                // compare roots of `block update` and `message proof`
                (bytes32 root, uint256 leafCount) = mp.calculate();
                if (root != messageRoot || leafCount != messageCount) {
                    revert(Errors.ERR_UNKNOWN);
                }

                // collect messages
                messages = Utils.append(messages, mp.mesgs);

                // update state
                remainMessageCount -= mp.mesgs.length;
                nextMessageSn += mp.mesgs.length;
            }
        }
        return messages;
    }

    function getSrcNetworkId() external view returns (string memory) {
        return srcNetworkId;
    }

    function getNetworkTypeId() external view returns (uint256) {
        return networkTypeId;
    }

    function getNetworkId() external view returns (uint256) {
        return networkId;
    }

    function getHeight() external view returns (uint256) {
        return height;
    }

    function getNetworkSectionHash() external view returns (bytes32) {
        return networkSectionHash;
    }

    function getMessageRoot() external view returns (bytes32) {
        return messageRoot;
    }

    function getMessageCount() external view returns (uint256) {
        return messageCount;
    }

    function getRemainMessageCount() external view returns (uint256) {
        return messageCount - (nextMessageSn - firstMessageSn);
    }

    function getNextMessageSn() external view returns (uint256) {
        return nextMessageSn;
    }

    function getValidators(uint256 nth) external view returns (address) {
        return validators[nth];
    }

    function getValidatorsCount() external view returns (uint256) {
        return validators.length;
    }

    function hasQuorumOf(uint256 votes) private view returns (bool) {
        return votes * 3 > validators.length * 2;
    }

    function checkAccessible(string memory _from) private view {
        (string memory net, ) = _from.splitBTPAddress();
        require(keccak256(abi.encodePacked(srcNetworkId)) == keccak256(abi.encodePacked(net)), ERR_UNAUTHORIZED);
    }

    function checkNextMessageSn(uint256 sn) private view {
        uint256 _nextMessageSn = nextMessageSn;
        if (_nextMessageSn < sn) {
            revert(Errors.ERR_NOT_VERIFIABLE);
        } else if (_nextMessageSn > sn) {
            revert(Errors.ERR_ALREADY_VERIFIED);
        }
    }

    function checkHeaderWithState(Header memory header) private view {
        require(networkId == header.networkId, Errors.ERR_UNKNOWN);
        require(networkSectionHash == header.prevNetworkSectionHash, Errors.ERR_UNKNOWN);

        uint256 _nextMessageSn = nextMessageSn;
        if (_nextMessageSn < header.messageSn) {
            revert(Errors.ERR_NOT_VERIFIABLE);
        } else if (_nextMessageSn > header.messageSn) {
            revert(Errors.ERR_ALREADY_VERIFIED);
        }
    }

    function checkBlockUpdateProof(Header memory header, Proof memory proof) private view {
        uint256 votes = 0;
        bytes32 decision = header.getNetworkTypeSectionDecisionHash(srcNetworkId, networkTypeId);
        for (uint256 j = 0; j < proof.signatures.length && !hasQuorumOf(votes); j++) {
            address signer = Utils.recoverSigner(decision, proof.signatures[j]);
            if (signer == validators[j]) {
                votes++;
            }
        }
        require(hasQuorumOf(votes), Errors.ERR_UNKNOWN);
    }
}
