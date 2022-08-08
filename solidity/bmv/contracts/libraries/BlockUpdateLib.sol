// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./MerkleTreeLib.sol";
import "./RLPEncode.sol";
import "./RLPReader.sol";

library BlockUpdateLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    struct Header {
        uint mainHeight;
        uint round;
        bytes32 nextProofContextHash;
        MerkleTreeLib.Path[] networkSectionToRoot;
        uint networkId;
        uint messageSn;
        bool hasNextValidators;
        bytes32 prevNetworkSectionHash;
        uint messageCount;
        bytes32 messageRoot;
        address[] nextValidators;
    }

    struct Proof {
        bytes[] signatures;
    }

    function decode(bytes memory enc)
    internal
    pure
    returns (
        Header memory header,
        Proof memory proof
    )
    {
        RLPReader.RLPItem memory i = enc.toRlpItem();
        RLPReader.RLPItem[] memory l = i.toList();
        return (
            decodeHeader(l[0].toBytes()),
            decodeProof(l[1].toBytes())
        );
    }

    function decodeHeader(bytes memory enc) internal pure returns (Header memory header) {
        RLPReader.RLPItem memory i = enc.toRlpItem();
        RLPReader.RLPItem[] memory l = i.toList();

        return Header(
            l[0].toUint(),
            l[1].toUint(),
            bytes32(l[2].toBytes()),
            l[3].payloadLen() > 0
                ? decodeNSRootPath(l[3].toRlpBytes())
                : new MerkleTreeLib.Path[](0),
            l[4].toUint(),
            l[5].toUint() >> 1,
            l[5].toUint() & 1 == 1,
            l[6].payloadLen() > 0
                ? bytes32(l[6].toBytes())
                : bytes32(0),
            l[7].toUint(),
            l[8].payloadLen() > 0
                ? bytes32(l[8].toBytes())
                : bytes32(0),
            l[5].toUint() & 1 == 1
                ? decodeValidators(l[9].toBytes())
                : new address[](0)
        );
    }

    function decodeProof(bytes memory enc) internal pure returns (Proof memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();

        ti = tl[0];
        tl = ti.toList();

        bytes[] memory signatures = new bytes[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            signatures[i] = tl[i].payloadLen() > 0
                ? tl[i].toBytes()
                : new bytes(0);
        }
        return Proof(signatures);
    }

    function getNetworkSectionHash(Header memory self) internal pure returns (bytes32) {
        bytes[] memory ns = new bytes[](5);
        ns[0] = RLPEncode.encodeUint(self.networkId);
        ns[1] = RLPEncode.encodeUint((self.messageSn << 1) | (self.hasNextValidators ? 1 : 0));
        ns[2] = self.prevNetworkSectionHash != bytes32(0)
            ? RLPEncode.encodeBytes(abi.encodePacked(self.prevNetworkSectionHash))
            : RLPEncode.encodeNil();
        ns[3] = RLPEncode.encodeUint(self.messageCount);
        ns[4] = self.messageRoot != bytes32(0)
            ? RLPEncode.encodeBytes(abi.encodePacked(self.messageRoot))
            : RLPEncode.encodeNil();
        return keccak256(RLPEncode.encodeList(ns));
    }

    function getNetworkTypeSectionDecisionHash(
        Header memory self,
        string memory srcNetworkId,
        uint networkType
    )
    internal
    pure
    returns (bytes32)
    {
        bytes[] memory ntsd = new bytes[](5);
        ntsd[0] = RLPEncode.encodeString(srcNetworkId);
        ntsd[1] = RLPEncode.encodeUint(networkType);
        ntsd[2] = RLPEncode.encodeUint(self.mainHeight);
        ntsd[3] = RLPEncode.encodeUint(self.round);
        ntsd[4] = RLPEncode.encodeBytes(abi.encodePacked(getNetworkTypeSectionHash(self)));
        return keccak256(RLPEncode.encodeList(ntsd));
    }

    function getNetworkTypeSectionHash(Header memory self) internal pure returns (bytes32) {
        bytes[] memory nts = new bytes[](2);
        nts[0] = RLPEncode.encodeBytes(abi.encodePacked(self.nextProofContextHash));
        nts[1] = RLPEncode.encodeBytes(abi.encodePacked(getNetworkSectionRoot(self)));
        return keccak256(RLPEncode.encodeList(nts));
    }

    function getNetworkSectionRoot(Header memory self) internal pure returns (bytes32) {
        return MerkleTreeLib.calculate(getNetworkSectionHash(self), self.networkSectionToRoot);
    }

    function decodeNSRootPath(bytes memory enc) private pure returns (MerkleTreeLib.Path[] memory) {
        RLPReader.RLPItem[] memory tl = enc.toRlpItem().toList();
        MerkleTreeLib.Path[] memory pathes = new MerkleTreeLib.Path[](tl.length);
        for (uint i = 0; i < tl.length; i++) {
            RLPReader.RLPItem[] memory tm = tl[i].toList();
            pathes[i] = MerkleTreeLib.Path(tm[0].toUint(), bytes32(tm[1].toBytes()));
        }
        return pathes;
    }

    function decodeValidators(bytes memory enc) private pure returns (address[] memory) {
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

