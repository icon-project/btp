// SPDX-License-Identifier: MIT
pragma solidity ^0.8.12;

import "./interfaces/IBMV.sol";
import "./libraries/Errors.sol";
import "./libraries/String.sol";
import "./libraries/RLPEncode.sol";
import "./libraries/RelayMessageLib.sol";
import "./libraries/Utils.sol";

contract BtpMessageVerifier is IBMV {
    using BlockUpdateLib for Header;
    using MessageProofLib for MessageProof;
    using RelayMessageLib for RelayMessage;
    using String for string;

    address private immutable bmc;
    uint256 private immutable networkTypeId;
    uint256 private immutable networkId;
    uint256 private immutable sequenceOffset;
    string private srcNetworkId;
    StateDB private db;

    // @dev wrap state variables to avoid stack too deep error
    struct StateDB {
        bytes32 networkSectionHash;
        bytes32 messageRoot;
        uint256 height;
        uint256 messageCount;
        uint256 firstMessageSn;
        uint256 nextMessageSn;
        address[] validators;
    }

    string private constant ERR_UNAUTHORIZED = "bmv: Unauthorized";
    string private constant ERR_INVALID_ARGS = "bmv: InvalidArgs";

    modifier onlyBmc() {
        require(msg.sender == bmc, ERR_UNAUTHORIZED);
        _;
    }

    modifier onlyBtpNetwork(string memory _from) {
        (string memory network, ) = _from.splitBTPAddress();
        require(
            keccak256(abi.encodePacked(srcNetworkId)) == keccak256(abi.encodePacked(network)),
            ERR_UNAUTHORIZED
        );
        _;
    }

    constructor(
        address _bmc,
        string memory _srcNetworkId,
        uint256 _networkTypeId,
        bytes memory _firstBlockHeader,
        uint256 _sequenceOffset
    ) {
        bmc = _bmc;
        srcNetworkId = _srcNetworkId;
        networkTypeId = _networkTypeId;
        sequenceOffset = _sequenceOffset;

        Header memory header = BlockUpdateLib.decodeHeader(_firstBlockHeader);
        require(header.nextValidators.length > 0, Errors.ERR_UNKNOWN);

        networkId = header.networkId;
        db = StateDB(
            header.getNetworkSectionHash(),
            header.messageRoot,
            header.mainHeight,
            header.messageCount,
            header.messageSn,
            header.messageSn,
            header.nextValidators
        );
    }

    /// @inheritdoc IBMV
    function getStatus() external view override returns (uint256, bytes memory) {
        bytes[] memory extra = new bytes[](3);
        extra[0] = RLPEncode.encodeUint(sequenceOffset);
        extra[1] = RLPEncode.encodeUint(db.firstMessageSn);
        extra[2] = RLPEncode.encodeUint(db.messageCount);
        return (db.height, RLPEncode.encodeList(extra));
    }

    /// @inheritdoc IBMV
    function handleRelayMessage(
        string memory,
        string memory _prev,
        uint256 _sn,
        bytes memory _msg
    ) external onlyBmc onlyBtpNetwork(_prev) returns (bytes[] memory messages) {
        StateDB memory _db = db;
        require(_db.nextMessageSn == _sn + sequenceOffset, ERR_INVALID_ARGS);
        uint256 remainMessageCount = _db.messageCount - (_db.nextMessageSn - _db.firstMessageSn);
        RelayMessage[] memory rms = RelayMessageLib.decode(_msg);

        for (uint256 i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TYPE_BLOCK_UPDATE) {
                require(remainMessageCount == 0, Errors.ERR_UNKNOWN);
                (Header memory header, Proof memory proof) = rms[i].toBlockUpdate();

                require(networkId == header.networkId, Errors.ERR_UNKNOWN);
                require(_db.networkSectionHash == header.prevNetworkSectionHash, Errors.ERR_UNKNOWN);
                checkMessageSn(_db.nextMessageSn, header.messageSn);
                checkBlockProof(header, proof, _db.validators);
                _db.height = header.mainHeight;
                _db.networkSectionHash = header.getNetworkSectionHash();
                if (header.hasNextValidators) {
                    _db.validators = header.nextValidators;
                }
                if (header.messageRoot != bytes32(0)) {
                    uint256 messageSn = _db.firstMessageSn + _db.messageCount;
                    checkMessageSn(messageSn, header.messageSn);
                    _db.messageRoot = header.messageRoot;
                    _db.firstMessageSn = header.messageSn;
                    remainMessageCount = _db.messageCount = header.messageCount;
                }
            } else if (rms[i].typ == RelayMessageLib.TYPE_MESSAGE_PROOF) {
                MessageProof memory mp = rms[i].toMessageProof();
                (bytes32 root, uint256 leafCount) = mp.calculate();
                require(root == _db.messageRoot && leafCount == _db.messageCount, Errors.ERR_UNKNOWN);
                messages = Utils.append(messages, mp.mesgs);
                remainMessageCount -= mp.mesgs.length;
                _db.nextMessageSn += mp.mesgs.length;
            }
        }

        if (_db.height != 0) {
            db.height = _db.height;
        }
        db.networkSectionHash = _db.networkSectionHash;
        db.messageRoot = _db.messageRoot;
        db.messageCount = _db.messageCount;
        db.firstMessageSn = _db.firstMessageSn;
        db.nextMessageSn = _db.nextMessageSn;
        db.validators = _db.validators;

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
        return db.height;
    }

    function getNetworkSectionHash() external view returns (bytes32) {
        return db.networkSectionHash;
    }

    function getMessageRoot() external view returns (bytes32) {
        return db.messageRoot;
    }

    function getMessageCount() external view returns (uint256) {
        return db.messageCount;
    }

    function getRemainMessageCount() external view returns (uint256) {
        return db.messageCount - (db.nextMessageSn - db.firstMessageSn);
    }

    function getNextMessageSn() external view returns (uint256) {
        return db.nextMessageSn;
    }

    function getValidators(uint256 nth) external view returns (address) {
        return db.validators[nth];
    }

    function getValidatorsCount() external view returns (uint256) {
        return db.validators.length;
    }

    function hasQuorumOf(uint256 nvalidators, uint256 votes) private pure returns (bool) {
        return votes * 3 > nvalidators * 2;
    }

    function checkMessageSn(uint256 expected, uint256 actual) private pure {
        if (expected < actual) {
            revert(Errors.ERR_NOT_VERIFIABLE);
        } else if (expected > actual) {
            revert(Errors.ERR_ALREADY_VERIFIED);
        }
    }

    /// @dev Check whether valid signatures from verifiers meets quorum
    function checkBlockProof(Header memory header, Proof memory proof, address[] memory validators) private view {
        uint256 votes = 0;
        bytes32 decision = header.getNetworkTypeSectionDecisionHash(srcNetworkId, networkTypeId);
        for (uint256 i = 0; i < proof.signatures.length && !hasQuorumOf(validators.length, votes); i++) {
            address signer = Utils.recoverSigner(decision, proof.signatures[i]);
            if (signer == validators[i]) {
                votes++;
            }
        }
        require(hasQuorumOf(validators.length, votes), Errors.ERR_UNKNOWN);
    }
}
