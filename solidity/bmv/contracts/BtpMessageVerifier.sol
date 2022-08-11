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
    uint private networkTypeId;
    uint private networkId;
    uint private height;
    bytes32 private networkSectionHash;
    bytes32 private messageRoot;
    uint private messageCount;
    uint private nextMessageSn;
    address[] private validators;
    uint private sequenceOffset;
    uint private firstMessageSn;

    string constant private ERR_UNAUTHORIZED = "bmv: Unauthorized";
    string constant private ERR_NOT_VERIFIABLE = "bmv: NotVerifiable";
    string constant private ERR_ALREADY_VERIFIED = "bmv: AlreadyVerified";

    modifier onlyBmc() {
        require(msg.sender == bmc, ERR_UNAUTHORIZED);
        _;
    }

    function initialize(
        address _bmc,
        string memory _srcNetworkId,
        uint _networkTypeId,
        bytes memory _firstBlockHeader,
        uint _sequenceOffset
    )
    initializer
    external
    {
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

    function getStatus() external view returns (uint, bytes memory) {
        bytes[] memory extra = new bytes[](3);
        extra[0] = RLPEncode.encodeUint(sequenceOffset);
        extra[1] = RLPEncode.encodeUint(firstMessageSn);
        extra[2] = RLPEncode.encodeUint(messageCount);
        return (
            height,
            RLPEncode.encodeList(extra)
        );
    }

    function handleRelayMessage(
        string memory,
        string memory _prev,
        uint _sn,
        bytes memory _msg
    )
    external
    onlyBmc
    returns (bytes[] memory) {
        checkAccessible(_prev);
        checkNextMessageSn(_sn);
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);
        bytes[] memory messages;
        uint remainMessageCount = messageCount - (nextMessageSn - firstMessageSn);
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
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
                    uint messageSn = firstMessageSn + messageCount - sequenceOffset;
                    if (messageSn < header.messageSn) {
                        revert(ERR_NOT_VERIFIABLE);
                    } else if (messageSn > header.messageSn) {
                        revert(ERR_ALREADY_VERIFIED);
                    }
                    messageRoot = header.messageRoot;
                    firstMessageSn = header.messageSn;
                    remainMessageCount = messageCount = header.messageCount;
                }

            } else if (rms[i].typ == RelayMessageLib.TypeMessageProof) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();

                // compare roots of `block update` and `message proof`
                (bytes32 root, uint leafCount) = mp.calculate();
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
        string[] memory tokens = srcNetworkId.split("/");

        // {network address}.{account identifier}
        if (tokens.length == 2) {
            return srcNetworkId;

        // btp://{netowkr address}.{account identifier}
        } else {
            return tokens[2];
        }
    }

    function getNetworkTypeId() public view returns (uint) {
        return networkTypeId;
    }

    function getNetworkId() public view returns (uint) {
        return networkId;
    }

    function getHeight() public view returns (uint) {
        return height;
    }

    function getNetworkSectionHash() public view returns (bytes32) {
        return networkSectionHash;
    }

    function getMessageRoot() public view returns (bytes32) {
        return messageRoot;
    }

    function getMessageCount() public view returns (uint) {
        return messageCount;
    }

    function getRemainMessageCount() public view returns (uint) {
        return messageCount - (nextMessageSn - firstMessageSn);
    }

    function getNextMessageSn() public view returns (uint) {
        return nextMessageSn;
    }

    function getValidators(uint nth) public view returns (address) {
        return validators[nth];
    }

    function getValidatorsCount() public view returns (uint) {
        return validators.length;
    }

    function hasQuorumOf(uint votes) private view returns (bool) {
        return votes * 3 > validators.length * 2;
    }

    function checkAccessible(string memory _from) private view {
        (string memory net, ) = _from.splitBTPAddress();
        require(getSrcNetworkId().compareTo(net), ERR_NOT_VERIFIABLE);
    }

    function checkNextMessageSn(uint sn) private view {
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
            revert(ERR_ALREADY_VERIFIED);
        } else if (nextMessageSn > header.messageSn) {
            revert(ERR_NOT_VERIFIABLE);
        }
    }

    function checkBlockUpdateProof(BlockUpdateLib.Header memory header, BlockUpdateLib.Proof memory proof) private view {
        uint votes = 0;
        bytes32 decision = header.getNetworkTypeSectionDecisionHash(srcNetworkId, networkTypeId);
        for (uint j = 0; j < proof.signatures.length && !hasQuorumOf(votes); j++) {
            address signer = Utils.recoverSigner(decision, proof.signatures[j]);
            if (signer == validators[j]) {
                votes++;
            }
        }
        require(hasQuorumOf(votes), ERR_NOT_VERIFIABLE);
    }

}

