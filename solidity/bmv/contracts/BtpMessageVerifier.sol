// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "./interfaces/IBMV.sol";
import "./libraries/String.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BtpMessageVerifier is IBMV, Initializable {
    using BlockUpdateLib for BlockUpdateLib.Header;
    using MessageProofLib for MessageProofLib.MessageProof;
    using RelayMessageLib for RelayMessageLib.RelayMessage;
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
    string private constant ERR_NOT_VERIFIABLE = "bmv: NotVerifiable";
    string private constant ERR_ALREADY_VERIFIED = "bmv: AlreadyVerified";

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

        BlockUpdateLib.Header memory header = BlockUpdateLib.decodeHeader(_firstBlockHeader);
        networkId = header.networkId;
        height = header.mainHeight;
        nextMessageSn = header.messageSn;
        firstMessageSn = header.messageSn;
        messageCount = header.messageCount;
        messageRoot = header.messageRoot;
        networkSectionHash = header.getNetworkSectionHash();
        require(header.nextValidators.length > 0, ERR_NOT_VERIFIABLE);
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
        require(_sn >= sequenceOffset, ERR_NOT_VERIFIABLE);
        checkNextMessageSn(_sn - sequenceOffset);
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);
        bytes[] memory messages;
        uint256 remainMessageCount = messageCount - (nextMessageSn - firstMessageSn);
        for (uint256 i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TYPE_BLOCK_UPDATE) {
                require(remainMessageCount == 0, ERR_NOT_VERIFIABLE);
                (BlockUpdateLib.Header memory header, BlockUpdateLib.Proof memory proof) = rms[i].toBlockUpdate();
                checkHeaderWithState(header);
                checkBlockUpdateProof(header, proof);

                // update state
                height = header.mainHeight;
                networkSectionHash = header.getNetworkSectionHash();
                if (header.hasNextValidators) {
                    validators = header.nextValidators;
                }
                if (header.messageRoot != bytes32(0)) {
                    uint256 messageSn = firstMessageSn + messageCount;
                    if (messageSn < header.messageSn) {
                        revert(ERR_NOT_VERIFIABLE);
                    } else if (messageSn > header.messageSn) {
                        revert(ERR_ALREADY_VERIFIED);
                    }
                    messageRoot = header.messageRoot;
                    firstMessageSn = header.messageSn;
                    remainMessageCount = messageCount = header.messageCount;
                }
            } else if (rms[i].typ == RelayMessageLib.TYPE_MESSAGE_PROOF) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();

                // compare roots of `block update` and `message proof`
                (bytes32 root, uint256 leafCount) = mp.calculate();
                if (root != messageRoot || leafCount != messageCount) {
                    revert(ERR_NOT_VERIFIABLE);
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

    function getSrcNetworkId() public view returns (string memory) {
        // TODO only support {network address}.{account identifier} format
        string memory scheme = "btp://";
        if (Utils.substring(srcNetworkId, 0, scheme.length()).compareTo(scheme)) {
            return Utils.substring(srcNetworkId, scheme.length(), srcNetworkId.length());
        } else {
            return srcNetworkId;
        }
    }

    function getNetworkTypeId() public view returns (uint256) {
        return networkTypeId;
    }

    function getNetworkId() public view returns (uint256) {
        return networkId;
    }

    function getHeight() public view returns (uint256) {
        return height;
    }

    function getNetworkSectionHash() public view returns (bytes32) {
        return networkSectionHash;
    }

    function getMessageRoot() public view returns (bytes32) {
        return messageRoot;
    }

    function getMessageCount() public view returns (uint256) {
        return messageCount;
    }

    function getRemainMessageCount() public view returns (uint256) {
        return messageCount - (nextMessageSn - firstMessageSn);
    }

    function getNextMessageSn() public view returns (uint256) {
        return nextMessageSn;
    }

    function getValidators(uint256 nth) public view returns (address) {
        return validators[nth];
    }

    function getValidatorsCount() public view returns (uint256) {
        return validators.length;
    }

    function hasQuorumOf(uint256 votes) private view returns (bool) {
        return votes * 3 > validators.length * 2;
    }

    function checkAccessible(string memory _from) private view {
        (string memory net, ) = _from.splitBTPAddress();
        require(getSrcNetworkId().compareTo(net), ERR_NOT_VERIFIABLE);
    }

    function checkNextMessageSn(uint256 sn) private view {
        if (nextMessageSn < sn) {
            revert(ERR_NOT_VERIFIABLE);
        } else if (nextMessageSn > sn) {
            revert(ERR_ALREADY_VERIFIED);
        }
    }

    function checkHeaderWithState(BlockUpdateLib.Header memory header) private view {
        require(networkId == header.networkId, ERR_NOT_VERIFIABLE);
        require(networkSectionHash == header.prevNetworkSectionHash, ERR_NOT_VERIFIABLE);
        if (nextMessageSn < header.messageSn) {
            revert(ERR_NOT_VERIFIABLE);
        } else if (nextMessageSn > header.messageSn) {
            revert(ERR_ALREADY_VERIFIED);
        }
    }

    function checkBlockUpdateProof(BlockUpdateLib.Header memory header, BlockUpdateLib.Proof memory proof)
        private
        view
    {
        uint256 votes = 0;
        bytes32 decision = header.getNetworkTypeSectionDecisionHash(srcNetworkId, networkTypeId);
        for (uint256 j = 0; j < proof.signatures.length && !hasQuorumOf(votes); j++) {
            address signer = Utils.recoverSigner(decision, proof.signatures[j]);
            if (signer == validators[j]) {
                votes++;
            }
        }
        require(hasQuorumOf(votes), ERR_NOT_VERIFIABLE);
    }
}
