// SPDX-License-Identifier: MIT
pragma solidity >= 0.4.22 < 0.9.0;

import "./RLPReader.sol";

library MessageProofLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;
    using MessageProofLib for MessageProofLib.MessageProof;
    using MessageProofLib for MessageProofLib.MessageProofNode;

    struct MessageProof {
        uint front;
        uint rear;
        uint length;
        MessageProofNode[] nodes;
    }

    struct MessageProofNode {
        uint level;
        uint nleaves;
        bytes32 hash;
    }

    function decode(bytes memory enc) internal returns (MessageProof memory) {

        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();

        RLPReader.RLPItem[] memory tp;
        RLPReader.RLPItem[] memory ls = tl[0].toList();
        RLPReader.RLPItem[] memory ms = tl[1].toList();
        RLPReader.RLPItem[] memory rs = tl[2].toList();

        MessageProof memory mp = MessageProof(
            0, 0, 0, new MessageProofNode[](ls.length + ms.length + rs.length));

        for (uint i = 0; i < ls.length; i++) {
            tp = ls[i].toList();
            mp.push(MessageProofNode(
                levelOf(tp[0].toUint()),
                tp[0].toUint(),
                bytes32(tp[1].toBytes())
            ));
        }

        for (uint i = 0; i < ms.length; i++) {
            mp.push(MessageProofNode(
                1, 1, keccak256(ms[i].toBytes())));
        }

        for (uint i = 0; i < rs.length; i++) {
            tp = rs[i].toList();
            mp.push(MessageProofNode(
                levelOf(tp[0].toUint()),
                tp[0].toUint(),
                bytes32(tp[1].toBytes())
            ));
        }
    }

    function push(MessageProof memory proof, MessageProofLib.MessageProofNode memory node)
    internal
    {
        require(proof.length < proof.nodes.length, "proof queue overflow");
        proof.nodes[proof.rear++] = node;
        proof.length++;
        if (proof.rear == proof.nodes.length) {
            proof.rear = 0;
        }
    }

    function pop(MessageProof memory proof)
    internal
    returns (MessageProofNode memory node)
    {
        require(proof.length > 0, "proof empty");
        node = proof.nodes[proof.front++];
        proof.length--;
        if (proof.front == proof.nodes.length) {
            proof.front = 0;
        }
    }

    function prove(MessageProof memory proof, bytes32 root, uint nleaves) internal returns (bytes32) {
        uint _nleaves = sizeOfLeaves(proof);
        require(nleaves == _nleaves, "invalid the number of leaves");

        uint tmp = nleaves - 1;
        uint maxlevels = 1;
        while(true) {
            maxlevels++;
            tmp = tmp >> 1;
            if (tmp == 0) {
                break;
            }
        }

        for (uint i = 0; i < maxlevels; i++) {
            tmp = proof.length;
            for (uint j = 0; j < tmp; j++) {
                MessageProofNode memory l = proof.pop();
                if (l.level <= (i + 1) && proof.length > 0) {
                    MessageProofNode memory r = proof.pop();
                    l = merge(l, r);
                    j++;
                }
                proof.push(l);
            }
        }
        // require(proof.length == 1);
        return proof.pop().hash;
    }

    function sizeOfLeaves(MessageProof memory proof) private returns (uint nleaves) {
        uint position = proof.front;
        for (uint i = 0 ; i < proof.length; i++) {
            nleaves += proof.nodes[position++].nleaves;
            if (position == proof.nodes.length) {
                position = 0;
            }
        }
    }

    function levelOf(uint nleaves) private returns (uint) {
        uint tmp = nleaves - 1;
        for (uint i = 1;; i++) {
            if (tmp == 0) {
                return i;
            }
            tmp = tmp >> 1;
        }
    }

    function merge(MessageProofNode memory l, MessageProofNode memory r)
    private
    returns (MessageProofNode memory)
    {
        require(l.nleaves == 1 << (l.level - 1), "invalid leaf size of left node");
        require(l.nleaves >= r.nleaves, "invalid balance of leaves");
        return MessageProofNode(
            l.level + 1,
            l.nleaves + r.nleaves,
            keccak256(abi.encodePacked(l.hash, r.hash))
        );
    }

}
