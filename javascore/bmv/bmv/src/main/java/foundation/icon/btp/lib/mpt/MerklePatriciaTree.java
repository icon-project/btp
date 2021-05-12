package foundation.icon.btp.lib.mpt;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import scorex.util.ArrayList;

public class MerklePatriciaTree {
    public static byte[] prove(byte[] root, Nibbles provingNibbles, ArrayList<MPTNode> proofs) {
        for (int i = 0; i< proofs.size(); i++) {
            MPTNode node = proofs.get(i);
            if (root != null && !java.util.Arrays.equals(node.getHash(), root)) {
                continue;
            }
            if (node.getType() == MPTNodeType.BRANCH) {
                int matchNibbleSize = 0;
                if (node.getNibbles() != null) {
                    matchNibbleSize = node.getNibbles().match(provingNibbles);

                    if (matchNibbleSize < node.getNibbles().size()) {
                        continue;
                    }
                }

                if (matchNibbleSize == provingNibbles.size()) {
                    if (node.getData() == null || node.getData().length == 0) {
                        continue;
                    }
                    return node.getData();
                }

                int childNodeIndex = provingNibbles.getNibble(matchNibbleSize); // next nibbles, 4 bits
                byte[] childData = node.getChildrens()[childNodeIndex];
                if (childData == null) {
                    continue;
                }

                Nibbles remainingProvingNibble = provingNibbles.createChildNibbles(matchNibbleSize + 1);
                if (childData.length < 32) { // inline child node
                    // if (proofs.size() > 0) { // inline child node is already valid, do not need any proof here
                    //     throw new AssertionError("MPT redundant proof");
                    // }
                    MPTNode childNode = new MPTNode(childData);
                    ArrayList<MPTNode> remainingProofs = new ArrayList<MPTNode>(1);
                    remainingProofs.add(childNode);
                    return MerklePatriciaTree.prove(null, remainingProvingNibble, remainingProofs);
                } else { // hash of child node store in branch node
                    if (proofs.size() == 1) {
                        throw new AssertionError("MPT missing proof");
                    }
                    ArrayList<MPTNode> remainingProofs = new ArrayList<MPTNode>(proofs);
                    remainingProofs.remove(i);
                    return MerklePatriciaTree.prove(childData, remainingProvingNibble, remainingProofs);
                }
            } else if (node.getType() == MPTNodeType.LEAF) {
                int matchNibbleSize = 0;
                if (node.getNibbles() != null) {
                    matchNibbleSize = node.getNibbles().match(provingNibbles);
                    
                    if (matchNibbleSize < node.getNibbles().size()) {
                        throw new AssertionError("MPT mismatch nibbles on leaf node");
                    }
                }

                if (matchNibbleSize != provingNibbles.size()) {
                    throw new AssertionError("MPT redundant nibble");
                }

                return node.getData();
            } else {
                throw new AssertionError("unhandle empty MPT node");
            }
        }
        throw new AssertionError("invalid MPT proof");
    }
}
