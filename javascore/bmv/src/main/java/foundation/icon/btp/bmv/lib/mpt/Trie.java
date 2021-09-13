package foundation.icon.btp.bmv.lib.mpt;

import foundation.icon.btp.bmv.lib.ArraysUtil;
import foundation.icon.btp.bmv.lib.BytesUtil;
import foundation.icon.btp.bmv.lib.HexConverter;
import score.Context;
import scorex.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Trie {

    private static final byte[] EMPTY_HASH = HexConverter
            .hexStringToByteArray("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421");

    private byte[] root = null;
    private final Map<byte[], byte[]> db = new HashMap<>();

    public Trie() {
        this.root = EMPTY_HASH;
    }

    public byte[] getRoot() {
        return this.root;
    }

    private void createInitialNode(byte[] key, byte[] value) {
       var node = new TrieNode.LeafNode(Nibbles.bytesToNibbles(key), value);
       this.root = node.hash();
       this.db.put(this.root, node.encodeRLP());
    }

    public void put(byte[] key, byte[] value) {
        if(BytesUtil.isEmptyOrNull(value)){
            delete(key);
        } else if (this.root == EMPTY_HASH) {
            createInitialNode(key, value);
        } else {

        }
    }

    private void delete(byte[] key){
    }

    private static class Path{
        TrieNode node;
        byte[] remaining;
        List<TrieNode> stack;
        private Path(TrieNode node, byte[] remaining, List<TrieNode> stack) {
            this.node = node;
            this.remaining = remaining;
            this.stack = stack;
        }
    }

    private Path findPath(byte[] key) throws MPTException {
        byte[] targetKey = Nibbles.bytesToNibbles(key);
        TrieNode node = lookupNode(root);
        Path path = _findPath(node, root, targetKey, new byte[]{});

        return null;
    }

    private Path _findPath(TrieNode node, byte[] nodeRef, byte[] key, byte[] currentKey) throws MPTException {
        List<TrieNode> stack = new ArrayList<>();
        byte[] keyRemainder = ArraysUtil.copyOfRangeByte(key, Nibbles.matchingNibbleLength(currentKey, key), key.length);
        stack.add(0, node);

        if (node instanceof TrieNode.BranchNode) {
            if(keyRemainder.length == 0)
                return new Path(node, BytesUtil.EMPTY_BYTES, stack);
            else {
                byte[] branchNode = ((TrieNode.BranchNode) node).getBranch(keyRemainder[0]);
                if(BytesUtil.isEmptyOrNull(branchNode)){
                    return new Path(null, keyRemainder, stack);
                } else {
                    // node found
                }
            }
        } else if(node instanceof TrieNode.LeafNode) {
            if(Nibbles.match(keyRemainder, node.getKey())){
                return new Path(node, BytesUtil.EMPTY_BYTES, stack);
            } else {
                return new Path(null, keyRemainder, stack);
            }
        }

        return null;
    }

    private void updateNode(byte[] _key, byte[] value, byte[] keyRemainder, List<TrieNode> stack) {
        TrieNode lastNode = stack.remove(stack.size()-1);
        byte[] key = Nibbles.bytesToNibbles(_key);

        int l = 0;
        var matchLeaf = false;
        if(lastNode instanceof TrieNode.LeafNode) {
            //for(int i=stack.size()-1; i > 0; i--) {
            for (TrieNode node : stack) {
                if (node instanceof TrieNode.BranchNode) {
                    l++;
                } else {
                    l += node.getKey().length;
                }
            }

            byte[] keyMatch = ArraysUtil.copyOfRangeByte(key, l, key.length);
            matchLeaf = (Nibbles.matchingNibbleLength(lastNode.getKey(), keyMatch) ==
                    lastNode.getKey().length && keyRemainder.length == 0);
        }

        if(matchLeaf) {
            lastNode.setValue(value);
            stack.add(0, lastNode); // push
        } else if(lastNode instanceof TrieNode.BranchNode) {
           if (keyRemainder.length != 0) {
                // add an extension to a branch node
            } else {
                lastNode.setValue(value);
            }
         } else {
            byte[] lastKey = lastNode.getKey();
            int matchingLength = Nibbles.matchingNibbleLength(lastKey, keyRemainder);
            TrieNode.BranchNode branchNode = new TrieNode.BranchNode();

            if (matchingLength != 0) {
                byte[] newKey =  ArraysUtil.copyOfRangeByte(key, 0, matchingLength);
                TrieNode.ExtensionNode extensionNode = new TrieNode.ExtensionNode(newKey, value);
                stack.add(0, extensionNode);
                lastKey =  ArraysUtil.copyOfRangeByte(lastKey, 0, matchingLength);
                keyRemainder =  ArraysUtil.copyOfRangeByte(keyRemainder, 0, matchingLength);
            }

            stack.add(0, branchNode);

            if(lastKey.length != 0) {
                byte[] branchKey = new byte[]{lastKey[0]};
                lastKey = ArraysUtil.copyOfRangeByte(lastKey, 0, lastKey.length - 1);

                if(lastKey.length != 0 || lastNode instanceof TrieNode.LeafNode) {
                    lastNode.setKey(lastKey);
                    //branchNode
                }
            }
        }
    }

    private TrieNode lookupNode(byte[] bytes) throws MPTException {
        var value = db.get(bytes);
        if(value != null) {
            return TrieNode.decode(bytes);
        } else {
            throw new MPTException("missing node in db");
        }
    }

    private byte[] formatNode(TrieNode node, boolean topLevel) {
        byte[] rlpNode = node.encodeRLP();

        if (rlpNode.length >= 32 || topLevel) {
            return Keccak256Hash(rlpNode);
        }

        return rlpNode;
    }

    public static byte[] Keccak256Hash(byte[] data) {
        return Context.hash("keccak-256", data);
    }

}
