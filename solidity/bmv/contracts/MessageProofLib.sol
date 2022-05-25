// SPDX-License-Identifier: MIT
pragma solidity >= 0.4.22 < 0.9.0;

import "./RLPReader.sol";

library MessageProofFragmentLib {

    struct MessageProofFragment {
        uint level;
        uint nleaves;
        bytes32 hash;
    }

    function levelOf(uint nleaves) internal returns (uint) {
        uint tmp = nleaves - 1;
        for (uint i = 1;; i++) {
            if (tmp == 0) {
                return i;
            }
            tmp = tmp >> 1;
        }
    }

    function merge(MessageProofFragment memory l, MessageProofFragment memory r)
    internal
    returns (MessageProofFragment memory)
    {
        require(l.nleaves == 1 << (l.level - 1), "invalid leaf size of left node");
        require(l.nleaves >= r.nleaves, "invalid balance of leaves");
        return MessageProofFragment(
            l.level + 1,
            l.nleaves + r.nleaves,
            keccak256(abi.encodePacked(l.hash, r.hash))
        );
    }

}

library MessageProofLib {

    using MessageProofFragmentLib for MessageProofFragmentLib.MessageProofFragment;
    using MessageProofLib for MessageProofLib.MessageProof;

    struct MessageProof {
        uint front;
        uint rear;
        uint length;
        MessageProofFragmentLib.MessageProofFragment[] nodes;
    }

    function create(
        MessageProofFragmentLib.MessageProofFragment[] memory rights,
        bytes[] memory msgs,
        MessageProofFragmentLib.MessageProofFragment[] memory lefts
    )
    internal
    returns (MessageProof memory)
    {
        uint cap = rights.length + msgs.length + lefts.length;
        MessageProof memory mp = MessageProof(
            0, 0, 0, new MessageProofFragmentLib.MessageProofFragment[](cap));

        for (uint i = 0; i < rights.length; i++) {
            mp.push(rights[i]);
        }

        for (uint i = 0; i < msgs.length; i++) {
            mp.push(MessageProofFragmentLib.MessageProofFragment(1, 1, keccak256(msgs[i])));
        }

        for (uint i = 0; i < lefts.length; i++) {
            mp.push(lefts[i]);
        }

        return mp;
    }

    function push(MessageProof memory tree, MessageProofFragmentLib.MessageProofFragment memory node)
    internal
    {
        require(tree.length < tree.nodes.length, "tree overflow");
        tree.nodes[tree.rear++] = node;
        tree.length++;
        if (tree.rear == tree.nodes.length) {
            tree.rear = 0;
        }
    }

    function pop(MessageProof memory tree)
    internal
    returns (MessageProofFragmentLib.MessageProofFragment memory node)
    {
        require(tree.length > 0, "tree empty");
        node = tree.nodes[tree.front++];
        tree.length--;
        if (tree.front == tree.nodes.length) {
            tree.front = 0;
        }
    }

    function prove(MessageProof memory tree, bytes32 root, uint nleaves) internal returns (bytes32) {
        uint _nleaves = sizeOfLeaves(tree);
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
            tmp = tree.length;
            for (uint j = 0; j < tmp; j++) {
                MessageProofFragmentLib.MessageProofFragment memory l = tree.pop();
                if (l.level <= (i + 1) && tree.length > 0) {
                    MessageProofFragmentLib.MessageProofFragment memory r = tree.pop();
                    l = l.merge(r);
                    j++;
                }
                tree.push(l);
            }
        }
        return tree.pop().hash;
    }

    function sizeOfLeaves(MessageProof memory tree) private returns (uint nleaves) {
        uint position = tree.front;
        for (uint i = 0 ; i < tree.length; i++) {
            nleaves += tree.nodes[position++].nleaves;
            if (position == tree.nodes.length) {
                position = 0;
            }
        }
    }

}

library MessageDecoder {

    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;

    function decode(bytes memory enc)
    internal
    returns (
        MessageProofFragmentLib.MessageProofFragment[] memory lefts,
        bytes[] memory msgs,
        MessageProofFragmentLib.MessageProofFragment[] memory rights 
    ) {
        RLPReader.RLPItem memory ti = enc.toRlpItem();
        RLPReader.RLPItem[] memory tl = ti.toList();
        tl = tl[0].toList();
        tl = tl[1].toList();
        ti = tl[1].toBytes().toRlpItem();
        tl = ti.toList();

        RLPReader.RLPItem[] memory tp;
        RLPReader.RLPItem[] memory ls = tl[0].toList();
        RLPReader.RLPItem[] memory ms = tl[1].toList();
        RLPReader.RLPItem[] memory rs = tl[2].toList();

        lefts = new MessageProofFragmentLib.MessageProofFragment[](ls.length);
        for (uint i = 0; i < ls.length; i++) {
            tp = ls[i].toList();
            lefts[i] = MessageProofFragmentLib.MessageProofFragment(
                MessageProofFragmentLib.levelOf(tp[0].toUint()),
                tp[0].toUint(),
                bytes32(tp[1].toBytes()));
        }

        msgs = new bytes[](ms.length);
        for (uint i = 0; i < ms.length; i++) {
            msgs[i] = ms[i].toBytes();
        }

        rights = new MessageProofFragmentLib.MessageProofFragment[](rs.length);
        for (uint i = 0; i < rs.length; i++) {
            tp = rs[i].toList();
            rights[i] = MessageProofFragmentLib.MessageProofFragment(
                MessageProofFragmentLib.levelOf(tp[0].toUint()),
                tp[0].toUint(),
                bytes32(tp[1].toBytes())
            );
        }
    }
}
