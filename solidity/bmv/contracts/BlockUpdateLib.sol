// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./RLPEncode.sol";
import "./RLPReader.sol";
import "./MerkleTreeLib.sol";
import "./Utils.sol";

library BlockUpdateLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;
    using BlockUpdateLib for BlockUpdateLib.BlockUpdate;

    struct BlockUpdate {
        uint mainHeight;
        uint round;
        bytes32 nextProofContextHash;
        MerkleTreeLib.Path[] networkSectionToRoot;
        uint networkId;
        uint messageSerialNumber;
        bool hasNewNextValidators;
        bytes prevNetworkSectionHash;
        uint messageCount;
        bytes32 messageRoot;
        bytes[] signatures;
        address[] nextValidators;
    }

    function getNetworkTypeSectionHash(BlockUpdate memory bu) internal returns (bytes32) {
        bytes[] memory nts = new bytes[](2);
        nts[0] = RLPEncode.encodeBytes(abi.encodePacked(bu.nextProofContextHash));
        nts[1] = RLPEncode.encodeBytes(abi.encodePacked(bu.getNetworkSectionRoot()));
        return keccak256(RLPEncode.encodeList(nts));
    }

    function verify(
        BlockUpdate memory bu,
        bytes storage srcNetworkId,
        uint networkTypId,
        address[] memory validators
    )
    internal
    {
        bytes32 ntsdh = bu.getNetworkTypeSectionDecisionHash(srcNetworkId, networkTypId);
        bu.verifySignature(ntsdh, validators);
    }

    function getNetworkTypeSectionDecisionHash(
        BlockUpdate memory bu,
        bytes storage srcNetworkId,
        uint networkType
    )
    internal
    returns (bytes32)
    {
        bytes[] memory ntsd = new bytes[](5);
        ntsd[0] = RLPEncode.encodeBytes(srcNetworkId);
        ntsd[1] = RLPEncode.encodeUint(networkType);
        ntsd[2] = RLPEncode.encodeUint(bu.mainHeight);
        ntsd[3] = RLPEncode.encodeUint(bu.round);
        ntsd[4] = RLPEncode.encodeBytes(abi.encodePacked(bu.getNetworkTypeSectionHash()));
        return keccak256(RLPEncode.encodeList(ntsd));
    }

    function encodeNetworkSection(BlockUpdate memory bu) internal returns (bytes memory) {
        bytes[] memory ns = new bytes[](5);
        ns[0] = RLPEncode.encodeUint(bu.networkId);
        ns[1] = RLPEncode.encodeUint((bu.messageSerialNumber << 1) | (bu.hasNewNextValidators ? 1 : 0));
        ns[2] = RLPEncode.encodeBytes(bu.prevNetworkSectionHash);
        ns[3] = RLPEncode.encodeUint(bu.messageCount);
        ns[4] = bu.messageRoot != bytes32(0)
            ? RLPEncode.encodeBytes(abi.encodePacked(bu.messageRoot))
            : RLPEncode.encodeNil();
        return RLPEncode.encodeList(ns);
    }

    function getNetworkSectionRoot(BlockUpdate memory bu) internal returns (bytes32) {
        bytes32 nsh = keccak256(encodeNetworkSection(bu));
        return MerkleTreeLib.calculate(nsh, bu.networkSectionToRoot);
    }

    function verifySignature(BlockUpdate memory bu, bytes32 message, address[] memory validators) internal {
        require(bu.signatures.length == validators.length,
                "the number of validators and signatures must be same");

        uint nverified = 0;
        for (uint i = 0; i < validators.length; i++) {
            address recovered = Utils.recoverSigner(message, bu.signatures[i]);
            if (validators[i] == recovered) {
                nverified++;
            }
        }
        require(validators.length * 2 <= nverified * 3, "verification of block falls short of a quorum");
    }

    function decode(bytes memory enc) internal returns (BlockUpdate memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();

        BlockUpdate memory bu = BlockUpdate(
            tl[0].toUint()
            , tl[1].toUint()
            , bytes32(tl[2].toBytes())
            , tl[3].payloadLen() > 0 ? decodeNSToRoot(tl[3].toRlpBytes()) : new MerkleTreeLib.Path[](0)
            , tl[4].toUint()
            , tl[5].toUint() >> 1
            , tl[5].toUint() & 1 == 1
            , tl[6].payloadLen() > 0 ? tl[6].toBytes() : new bytes(0)
            , tl[7].toUint()
            , tl[8].payloadLen() > 0 ? bytes32(tl[8].toBytes()) : bytes32(0)
            , tl[9].payloadLen() > 0 ? decodeSignatures(tl[9].toBytes()) : new bytes[](0)
            , tl[10].payloadLen() > 0 ? decodeValidators(tl[10].toBytes()) : new address[](0)
        );
        return bu;
    }

    function decodeNSToRoot(bytes memory enc) internal returns (MerkleTreeLib.Path[] memory) {
        RLPReader.RLPItem[] memory tl = enc.toRlpItem().toList();
        MerkleTreeLib.Path[] memory pathes = new MerkleTreeLib.Path[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            pathes[i] = MerkleTreeLib.Path(tl[0].toUint(), bytes32(tl[1].toBytes()));
        }
        return pathes;
    }

    function decodeSignatures(bytes memory enc) private returns (bytes[] memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();

        bytes[] memory signatures = new bytes[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            // TODO nil signatures
            signatures[i] = tl[i].toBytes();
        }
        return signatures;
    }

    function decodeValidators(bytes memory enc) private returns (address[] memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();

        address[] memory validators = new address[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            validators[i] = tl[i].toAddress();
        }
        return validators;
    }

}

