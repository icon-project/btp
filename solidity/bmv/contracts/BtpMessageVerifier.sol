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
    address[] private validators;
    bytes private prevNetworkSectionHash;
    Context public context;

    struct Context {
        bytes srcNetworkId;
        uint networkTypeId;
        BlockUpdateLib.BlockUpdate blockUpdate;

        bytes networkSectionHash;
    }

    function copyFromMemory(BlockUpdateLib.BlockUpdate memory mem) private {
        context.blockUpdate.mainHeight = mem.mainHeight;
        context.blockUpdate.round = mem.round;
        context.blockUpdate.nextProofContextHash = mem.nextProofContextHash;
        for (uint i = 0; i < mem.networkSectionToRoot.length; i++) {
            context.blockUpdate.networkSectionToRoot.push(mem.networkSectionToRoot[i]);
        }
        context.blockUpdate.networkId = mem.networkId;
        context.blockUpdate.messageSerialNumber = mem.messageSerialNumber;
        context.blockUpdate.hasNewNextValidators = mem.hasNewNextValidators;
        context.blockUpdate.prevNetworkSectionHash = mem.prevNetworkSectionHash;
        context.blockUpdate.messageCount = mem.messageCount;
        context.blockUpdate.messageRoot = mem.messageRoot;
        for (uint i = 0; i < mem.signatures.length; i++) {
            context.blockUpdate.signatures.push(mem.signatures[i]);
        }
        for (uint i = 0; i < mem.nextValidators.length; i++) {
            context.blockUpdate.nextValidators.push(mem.nextValidators[i]);
        }
    }

   constructor() {
       bmc = address(0);
   }

   // TODO move to constructor
   function initialize(bytes memory srcNetworkId, uint networkTypeId, bytes memory firstBlockUpdate) external {
       context.srcNetworkId = srcNetworkId;
       context.networkTypeId = networkTypeId;
       copyFromMemory(BlockUpdateLib.decode(firstBlockUpdate));
   }

    modifier onlyBmc() {
        require(
            msg.sender == bmc,
            "Function must be called through known bmc"
        );
        _;
    }

    function getStatus() external pure returns (uint256) {
        return 1;
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    ) external returns (bytes[] memory) {
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
                BlockUpdateLib.BlockUpdate memory bu = rms[i].toBlockUpdate();

                // TODO check with previous states (network id, message sequence, ...)

                bu.verify(context.srcNetworkId, context.networkTypeId, context.blockUpdate.nextValidators);
            } else if (rms[i].typ == RelayMessageLib.TypeMessageProof) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();

                // TODO handle partial messages...

                mp.verify(context.blockUpdate.messageRoot, context.blockUpdate.messageCount);
            }
        }
    }

}

