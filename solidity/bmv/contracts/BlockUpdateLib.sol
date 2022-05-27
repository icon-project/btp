// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./RLPReader.sol";
import "./MerkleTreeLib.sol";

library BlockUpdateLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

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
        // proof
        bytes32[] signatures;
        // proof context
        address[] nextValidators;
    }

    function decode(bytes memory enc) internal returns (BlockUpdate memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();

        BlockUpdate memory bu = BlockUpdate(
            tl[0].toUint()
            , tl[1].toUint()
            , bytes32(tl[2].toBytes())
            , decodeNSToRoot(tl[3].toRlpBytes())
            , tl[4].toUint()
            , tl[5].toUint() >> 1
            , tl[5].toUint() & 1 == 1
            , tl[6].toBytes()
            , tl[7].toUint()
            , bytes32(tl[8].toBytes())
            , decodeSignatures(tl[9].toBytes())
            , decodeValidators(tl[10].toBytes())
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

    function decodeSignatures(bytes memory enc) private returns (bytes32[] memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();

        bytes32[] memory signatures = new bytes32[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            // TODO nil signatures
            signatures[i] = bytes32(tl[i].toBytes());
        }
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

