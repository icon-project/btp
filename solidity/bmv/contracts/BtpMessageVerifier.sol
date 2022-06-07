// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./RelayMessageLib.sol";
import "./Utils.sol";

contract BtpMessageVerifier is IBtpMessageVerifier {

    using BlockUpdateLib for BlockUpdateLib.BlockUpdate;
    using MessageProofLib for MessageProofLib.MessageProof;
    using RelayMessageLib for RelayMessageLib.RelayMessage;

    address public immutable bmc;
    Context public context;

    struct Context {
        bytes srcNetworkId;
        uint networkTypeId;
        uint networkId;
        uint mainHeight;
        bytes32 networkSectionHash;
        bytes32 messageRoot;
        uint messageCount;
        uint remainMessageCount;
        uint nextMessageSn;
        address[] validators;
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
        context.srcNetworkId = srcNetworkId;
        context.networkTypeId = networkTypeId;
        context.networkId = networkId;
        BlockUpdateLib.BlockUpdate memory bu = BlockUpdateLib.decode(firstBlockUpdate);
        context.mainHeight = bu.mainHeight;
        context.nextMessageSn = 0;
        context.remainMessageCount = 0;
        context.networkSectionHash = bu.getNetworkSectionHash();
        context.validators = bu.nextValidators;
    }

    modifier onlyBmc() {
        require(
            msg.sender == bmc,
            "Function must be called through known bmc"
        );
        _;
    }

    function getStatus() external view returns (uint256) {
        return context.mainHeight;
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    ) external returns (bytes[] memory) {
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);

        // TODO optimize R/W of storage

        bytes[] memory messages;
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
                if (context.remainMessageCount != 0) {
                    // ignore block update, if there are messages to be processed
                    continue;
                }

                BlockUpdateLib.BlockUpdate memory bu = rms[i].toBlockUpdate();

                require(context.networkId == bu.networkId, "invalid network id");
                require(context.networkSectionHash == bu.prevNetworkSectionHash,
                        "invalid preivous network section hash");
                require(context.nextMessageSn == bu.messageSn, "invalid message sequence number");
                bu.verify(context.srcNetworkId, context.networkTypeId, context.validators);

                if (bu.hasNewNextValidators) {
                    context.validators = bu.nextValidators;
                }
                bytes32 prevContext = context.networkSectionHash;
                context.networkSectionHash = bu.getNetworkSectionHash();
                if (bu.messageRoot != bytes32(0)) {
                    context.messageRoot = bu.messageRoot;
                    context.remainMessageCount = context.messageCount = bu.messageCount;
                }

            } else if (rms[i].typ == RelayMessageLib.TypeMessageProof) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();

                mp.verify(context.messageRoot, context.messageCount);
                require(mp.leftLeafCount == context.messageCount - context.remainMessageCount, "??");
                context.remainMessageCount -= mp.messageCount;
                context.nextMessageSn += mp.messageCount;
                messages = Utils.append(messages, mp.messages);
            }
        }
        return messages;
    }

}

