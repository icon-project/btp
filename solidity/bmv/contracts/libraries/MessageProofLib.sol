// SPDX-License-Identifier: MIT
pragma solidity >= 0.4.22 < 0.9.0;

import "./RLPReader.sol";

library MessageProofLib {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;
    using MessageProofLib for MessageProofLib.Queue;

    struct MessageProof {
        MessageProofNode[] lefts;
        bytes[] mesgs;
        MessageProofNode[] rights;
    }

    struct MessageProofNode {
        uint level;
        uint leafCount;
        bytes32 hash;
    }

    struct Queue {
        uint front;
        uint rear;
        uint length;
        MessageProofNode[] nodes;
    }

    function decode(bytes memory enc) internal returns (MessageProof memory) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();

        RLPReader.RLPItem[] memory ms = tl[1].toList();
        MessageProofNode[] memory lefts = toMessageProofNodes(tl[0].toList());
        MessageProofNode[] memory rights = toMessageProofNodes(tl[2].toList());

        bytes[] memory mesgs = new bytes[](ms.length);
        for (uint i = 0; i < ms.length; i++) {
            mesgs[i] = ms[i].toBytes();
        }

        return MessageProof(lefts, mesgs, rights);
    }

    function getLeafCount(MessageProofNode[] memory nodes) internal returns (uint cnt) {
        for (uint i = 0; i < nodes.length; i++) {
            cnt += nodes[i].leafCount;
        }
    }

    function calculate(MessageProof memory proof) internal returns (bytes32, uint) {
        Queue memory queue = toQueue(proof);
        uint leafCount = getLeafCount(queue.nodes);
        uint t = leafCount;
        uint maxLevel = 1;
        while (t != 0) {
            maxLevel++;
            t = t >> 1;
        }

        uint w;
        for (uint i = 0; i < maxLevel; i++) {
            t = queue.length;
            w = queue.length;
            for (uint j = 0; j < t; j++) {
                MessageProofNode memory l = queue.pop(); w--;
                if (l.level <= (i + 1) && w > 0) {
                    MessageProofNode memory r = queue.pop();
                    l = concat(l, r); w--; j++;
                }
                queue.push(l);
            }
        }
        assert(queue.length == 1);
        return (queue.pop().hash, leafCount);
    }

    function push(Queue memory queue, MessageProofNode memory node) internal {
        assert(queue.length < queue.nodes.length);

        queue.nodes[queue.rear++] = node;
        queue.length++;
        if (queue.rear == queue.nodes.length) {
            queue.rear = 0;
        }
    }

    function pop(Queue memory queue) internal returns (MessageProofNode memory node) {
        assert(queue.length > 0);
        node = queue.nodes[queue.front];
        delete queue.nodes[queue.front++];
        queue.length--;
        if (queue.front == queue.nodes.length) {
            queue.front = 0;
        }
    }

    function levelOf(uint nleaves) private returns (uint) {
        uint t = nleaves - 1;
        uint l = 1;
        while (t != 0) {
            l++;
            t = t >> 1;
        }
        return l;
    }

    function concat(MessageProofNode memory left, MessageProofNode memory right)
    private
    returns (MessageProofNode memory)
    {
        require(left.leafCount == (1 << (left.level - 1)), "MessageProof: Invalid leaf count of left node");
        require(left.leafCount >= right.leafCount, "MessageProof: Invalid leaves balance");
        return MessageProofNode(
            left.level + 1,
            left.leafCount + right.leafCount,
            keccak256(abi.encodePacked(left.hash, right.hash))
        );
    }

    function toQueue(MessageProof memory proof) private returns (Queue memory) {
        Queue memory queue = Queue({
            front: 0,
            rear: 0,
            length: 0,
            nodes: new MessageProofNode[](
                proof.lefts.length + proof.mesgs.length + proof.rights.length
            )
        });

        for (uint i = 0; i < proof.lefts.length; i++) {
            queue.push(proof.lefts[i]);
        }

        for (uint i = 0; i < proof.mesgs.length; i++) {
            queue.push(MessageProofNode({
                level: 1,
                leafCount: 1,
                hash: keccak256(proof.mesgs[i])
            }));
        }

        for (uint i = 0; i < proof.rights.length; i++) {
            queue.push(proof.rights[i]);
        }

        return queue;
    }

    function toMessageProofNodes(RLPReader.RLPItem[] memory items)
    private
    returns (MessageProofNode[] memory)
    {
        RLPReader.RLPItem[] memory tmp;
        MessageProofNode[] memory nodes = new MessageProofNode[](items.length);
        for (uint i = 0 ; i < items.length; i++) {
            tmp = items[i].toList();
            nodes[i] = MessageProofNode(
                levelOf(tmp[0].toUint()),
                tmp[0].toUint(),
                bytes32(tmp[1].toBytes())
            );
        }
        return nodes;
    }

}
