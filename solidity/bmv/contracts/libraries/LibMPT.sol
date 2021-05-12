// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;

import "./LibRLPDecode.sol";
import "./LibBytes.sol";

library LibMerklePatriciaTrie {
    using LibRLPDecode for LibRLPDecode.RLPItem;
    using LibRLPDecode for bytes;
    using LibBytes for bytes;
    using LibMerklePatriciaTrie for LibMerklePatriciaTrie.MPT;

    struct MPT {
        bytes32 hash;
        bytes serialized;
        MPT[] children;
        bytes nibbles;
        bytes1 prefix;
        bytes data;
    }

    function init(bytes32 _hash, bytes memory serialized)
        internal
        returns (MPT memory node)
    {
        node.hash = _hash;
        if (serialized.length != 0) {
            node.serialized = serialized;
            LibRLPDecode.RLPItem[] memory ls = serialized.toRlpItem().toList();
            if (ls.length == 2) {
                bytes memory header = ls[0].toBytes();
                node.prefix = header[0] & 0xF0;
                bytes memory nibbles;

                if ((node.prefix & 0x10) != 0)
                    nibbles = abi.encodePacked(header[0] & 0x0F);

                node.nibbles = bytesToNibbles(
                    header.slice(1, header.length),
                    nibbles
                );

                if ((node.prefix & 0x20) != 0) node.data = ls[1].toBytes();
                else {
                    MPT[] memory children = new MPT[](node.children.length + 1);
                    for (uint256 i = 0; i < node.children.length; i++)
                        children[i] = node.children[i];
                    children[node.children.length] = init(
                        ls[1].toBytes().bytesToBytes32(),
                        ""
                    );
                    delete node.children;
                    node.children = children;
                }
            } else if (ls.length == 17) {
                MPT memory bNode;
                for (uint256 i = 0; i < 17; i++) {
                    if (ls[i].toBytes()[0] >= 0xC0) {
                        bNode = init("", ls[i].toBytes());
                    } else {
                        if (ls[i].toBytes().length > 0) {
                            bNode = init(ls[i].toBytes().bytesToBytes32(), "");
                        }
                    }

                    MPT[] memory children = new MPT[](node.children.length + 1);
                    for (uint256 j = 0; j < node.children.length; j++)
                        children[j] = node.children[j];
                    children[node.children.length] = bNode;
                    delete node.children;
                    node.children = children;
                }
                node.data = ls[16].toBytes();
            } else revert("MPT Exception: Invalid list length");
        }
    }

    function bytesToNibbles(bytes memory data, bytes memory nibbles)
        internal
        pure
        returns (bytes memory)
    {
        bytes memory expanded = new bytes(data.length * 2);
        for (uint256 i = 0; i < data.length; i++) {
            expanded[i * 2] = (data[i] >> 4) & 0x0f;
            expanded[i * 2 + 1] = data[i] & 0x0f;
        }
        return abi.encodePacked(nibbles, expanded);
    }

    function matchNibbles(bytes memory src, bytes memory dst)
        internal
        pure
        returns (uint256)
    {
        uint256 len = (dst.length < src.length) ? dst.length : src.length;
        for (uint256 i = 0; i < len; i++) if (src[i] != dst[i]) return i;
        return len;
    }

    function prove(
        MPT memory mpt,
        bytes memory nibbles,
        bytes[] memory proofs
    ) internal returns (bytes memory) {
        // check if node is a hash
        if (mpt.hash.length > 0 && mpt.serialized.length == 0) {
            bytes memory serialized = proofs[0];
            // TODO: pending SHA3-256
            // bytes32 _hash = keccak256(serialized);
            bytes32 _hash = mpt.hash;
            if (proofs.length > 1) {
                bytes[] memory temp = new bytes[](proofs.length - 1);
                for (uint256 i = 0; i < temp.length; i++)
                    temp[i] = proofs[i + 1];
                delete proofs;
                proofs = temp;
            } else if (proofs.length == 1) delete proofs;

            require(mpt.hash == _hash, "MPT Exception: Mismatch hash");
            MPT memory node = init(_hash, serialized);
            return node.prove(nibbles, proofs);
        // check if node is extension
        } else if (mpt.children.length == 1) {
            uint256 sharedLen = matchNibbles(mpt.nibbles, nibbles);
            require(
                sharedLen >= mpt.nibbles.length,
                "MPT Exception: Mismatch nibbles on extension"
            );
            return
                mpt.children[0].prove(
                    nibbles.slice(sharedLen, nibbles.length),
                    proofs
                );
        // check if node is branch
        } else if (mpt.children.length == 16) {
            if (nibbles.length == 0) return mpt.data;
            else {
                MPT memory node = mpt.children[uint8(nibbles[0])];
                return node.prove(nibbles.slice(1, nibbles.length), proofs);
            }
        } else {
            uint256 sharedLen = matchNibbles(mpt.nibbles, nibbles);
            require(
                sharedLen >= nibbles.length,
                "MPT Exception: mismatch nibbles on leaf"
            );
            return mpt.data;
        }
    }

    function prove(
        bytes32 root,
        bytes memory key,
        bytes[] memory proofs
    ) internal returns (bytes memory) {
        bytes memory nibbles = bytesToNibbles(key, "");
        MPT memory mpt = init(root, "");
        return mpt.prove(nibbles, proofs);
    }
}
