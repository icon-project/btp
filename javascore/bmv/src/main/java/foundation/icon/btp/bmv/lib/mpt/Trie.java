package foundation.icon.btp.bmv.lib.mpt;

import foundation.icon.btp.bmv.lib.ArraysUtil;
import foundation.icon.btp.bmv.lib.BytesUtil;
import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.Pair;
import score.Context;
import scorex.util.HashMap;
import scorex.util.ArrayList;

import java.util.List;

public class Trie {

    /**
     *   Merkle root of an empty tree, which the Keccak hash of an empty byte sequence.
     */
    private static final byte[] EMPTY_HASH = HexConverter
            .hexStringToByteArray("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421");

    private byte[] root = null;
    private final HashMap<String, byte[]> db = new HashMap<>();

    public Trie() {
        this.root = EMPTY_HASH;
    }

    public Trie(byte[] rootHash) {
        this.root = rootHash;
    }

    public byte[] getRoot() {
        return this.root;
    }

    private void createInitialNode(byte[] key, byte[] value) {
       var node = new TrieNode.LeafNode(Nibbles.bytesToNibbles(key), value);
       this.root = node.hash();
       this.db.put(HexConverter.bytesToHex(this.root), node.encodeRLP());
    }

    public void put(byte[] key, byte[] value) throws MPTException {
        if(BytesUtil.isEmptyOrNull(value)){
            delete(key);
        } else if (this.root == EMPTY_HASH) {
            createInitialNode(key, value);
        } else {
            Path path = findPath(key);
            updateNode(key, value, path.remaining, path.stack);
        }
    }

    public byte[] get(byte[] key) throws MPTException {
        Path path = findPath(key);
        if(path.node != null && path.remaining.length == 0){
           return path.node.getValue();
        }
        return null;
    }

    private void delete(byte[] key){
    }

    public static List<byte[]> createProof(Trie trie, byte[] key) throws MPTException {
        Path path = trie.findPath(key);
        List<byte[]> proofs = new ArrayList<>();
        for (TrieNode node:path.stack)
            proofs.add(node.encodeRLP());
        return proofs;
    }

    public static byte[] verifyProof(byte[] hashRoot, byte[] key, List<byte[]> proof) throws MPTException {
        Trie proofTrie = new Trie(hashRoot);
        Trie.fromProof(proof, proofTrie);
        return proofTrie.get(key);
    }

    public static Trie fromProof(List<byte[]> proof, Trie trie){
        ArrayList<DBUpdate> stack = new ArrayList<>();
        for(byte[] value:proof){
            stack.add(new DBUpdate(Operation.PUT,
                    Keccak256Hash(value),
                    value));
        }
        if (trie == null)
            trie = new Trie();
        trie.updateDB(stack);
        return trie;
    }

    public static class Path{
        TrieNode node;
        byte[] remaining;
        List<TrieNode> stack;
        private Path(TrieNode node, byte[] remaining, List<TrieNode> stack) {
            this.node = node;
            this.remaining = remaining;
            this.stack = stack;
        }
        public List<TrieNode> getNodes() {
            return stack;
        }
    }

    public Path findPath(byte[] key) throws MPTException {
        byte[] targetKey = Nibbles.bytesToNibbles(key);
        TrieNode node = lookupNode(root);
        List<TrieNode> stack = new ArrayList<>();
        return _findPath(node, targetKey, new byte[]{}, stack);
    }

    private Path _findPath(TrieNode node, byte[] key, byte[] currentKey, List<TrieNode> stack) throws MPTException {
        stack.add(node);
        byte[] keyRemainder = ArraysUtil.copyOfRangeByte(key, Nibbles.matchingNibbleLength(currentKey, key), key.length);
        if (node instanceof TrieNode.BranchNode) {
            if(keyRemainder.length == 0)
                return new Path(node, BytesUtil.EMPTY_BYTES, stack);
            else {
                int branchIndex = keyRemainder[0];
                var branchNode = ((TrieNode.BranchNode) node).getBranch(branchIndex);
                if(branchNode == null){
                    return new Path(null, keyRemainder, stack);
                } else {
                    byte[] childKey = new byte[currentKey.length+1];
                    System.arraycopy(currentKey, 0, childKey, 0, currentKey.length);
                    childKey[currentKey.length] = (byte) branchIndex;
                    TrieNode childNode = lookupNode(branchNode);
                    // node found
                    return _findPath(childNode, key, childKey, stack);
                }
            }
        } else if(node instanceof TrieNode.LeafNode) {
            if(Nibbles.match(keyRemainder, node.getKey())){
                return new Path(node, BytesUtil.EMPTY_BYTES, stack);
            } else {
                return new Path(null, keyRemainder, stack);
            }
        } else if(node instanceof TrieNode.ExtensionNode) {
            int matchingLen = Nibbles.matchingNibbleLength(keyRemainder, node.getKey());
            if(matchingLen != node.getKey().length) {
               return new Path(null, keyRemainder, stack);
            } else {
                return childrenPath(node, currentKey, key, stack);
            }
        }

        return null;
    }

    private Path childrenPath(TrieNode node, byte[] key, byte[] currentKey, List<TrieNode> stack) throws MPTException {
        List<Pair<byte[], Object>> children = new ArrayList<>();
        if (node instanceof TrieNode.ExtensionNode) {
            Pair<byte[], Object> value;
            if (node.getValue() != null)
                value = Pair.of(node.getKey(), node.getValue());
            else
                value = Pair.of(node.getKey(), ((TrieNode.ExtensionNode)node).getValues());
            children.add(value);
        } else if(node instanceof TrieNode.BranchNode){
            //children = node.getChildren().map((b) => [[b[0]], b[1]])
        }
        for(Pair<byte[], Object> child:children){
            //_findPath(childNode, key, childKey, stack);
            var childKey = child.key;
            childKey = ArraysUtil.concat(key, childKey);
            TrieNode childNode = null;
            if(child.value instanceof byte[]){
                childNode = lookupNode((byte[]) child.value);
            } else if (child.value instanceof List) {
                childNode = lookupNode((List) child.value);
            }
            return _findPath(childNode, currentKey, childKey, stack);
        }
        return null;
    }

    private void updateNode(byte[] _key, byte[] value, byte[] keyRemainder, List<TrieNode> stack) {
        List<DBUpdate> opStack = new ArrayList<>();
        TrieNode lastNode = stack.remove(stack.size()-1);
        byte[] key = Nibbles.bytesToNibbles(_key);
        int l = 0;
        var matchLeaf = false;
        if(lastNode instanceof TrieNode.LeafNode) {
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
            stack.add(lastNode); // push
        } else if(lastNode instanceof TrieNode.BranchNode) {
            stack.add(lastNode);
           if (keyRemainder.length != 0) {
                // add an extension to a branch node
               keyRemainder = ArraysUtil.copyOfRangeByte(keyRemainder, 1, keyRemainder.length);
               stack.add(new TrieNode.LeafNode(keyRemainder, value));
            } else {
                lastNode.setValue(value);
            }
         } else {
            byte[] lastKey = lastNode.getKey();
            int matchingLength = Nibbles.matchingNibbleLength(lastKey, keyRemainder);
            TrieNode.BranchNode newBranchNode = new TrieNode.BranchNode();

            if (matchingLength != 0) {
                byte[] newKey =  ArraysUtil.copyOfRangeByte(lastNode.getKey(), 0, matchingLength);
                TrieNode.ExtensionNode extensionNode = new TrieNode.ExtensionNode(newKey, value);
                stack.add(extensionNode);
                lastKey = ArraysUtil.copyOfRangeByte(lastKey, matchingLength, lastKey.length);
                keyRemainder = ArraysUtil.copyOfRangeByte(keyRemainder, matchingLength, keyRemainder.length);
            }

            stack.add(newBranchNode);

            if(lastKey.length != 0) {
                int branchKey = lastKey[0];
                lastKey = ArraysUtil.copyOfRangeByte(lastKey, 1, lastKey.length);

                if(lastKey.length != 0 || lastNode instanceof TrieNode.LeafNode) {
                    lastNode.setKey(lastKey);
                    var raw = formatNode(lastNode, false, opStack, false);
                    byte[][] branch = null;
                    if (raw.size() == 2)
                        branch = new byte[][]{raw.get(0)[0], raw.get(1)[0]};
                    else
                        branch = new byte[][]{raw.get(0)[0], new byte[0]};
                    newBranchNode.setBranch(branchKey, branch);
                } else {
                    formatNode(lastNode, false, opStack, true);
                    newBranchNode.setBranch(branchKey, ((TrieNode.ExtensionNode)lastNode).getValues());
                }
            }  else {
                newBranchNode.setValue(lastNode.getValue());
            }

            if (keyRemainder.length != 0) {
                keyRemainder = ArraysUtil.copyOfRangeByte(keyRemainder, 1, keyRemainder.length);
                TrieNode newLeafNode = new TrieNode.LeafNode(keyRemainder, value);
                stack.add(newLeafNode);
            } else {
                newBranchNode.setValue(value);
            }
        }

        save(key, stack, opStack);
    }

    private void save(byte[] key, List<TrieNode> stack, List<DBUpdate> toSave){
        List<byte[][]> lastRoot = null;

        while(stack.size() > 0) {
            TrieNode node = stack.remove(stack.size() - 1);
            if (node instanceof TrieNode.LeafNode)
                key = ArraysUtil.copyOfRangeByte(key, 0, key.length - node.getKey().length);
            else if (node instanceof TrieNode.ExtensionNode) {
                key = ArraysUtil.copyOfRangeByte(key, 0, key.length - node.getKey().length);
                if(lastRoot != null){
                    if (lastRoot.size() == 1)
                        node.setValue(lastRoot.get(0)[0]);
                    else
                       ((TrieNode.ExtensionNode) node).setValues(lastRoot);
                }
            } else if (node instanceof TrieNode.BranchNode) {
                if(lastRoot != null) {
                    int branchKey = key[key.length-1];
                    key = ArraysUtil.copyOfRangeByte(key, 0, key.length-1);
                    byte[][] branch = null;
                    if (lastRoot.size() == 2)
                        branch = new byte[][]{lastRoot.get(0)[0], lastRoot.get(1)[0]};
                    else
                        branch = new byte[][]{lastRoot.get(0)[0], new byte[0]};
                    ((TrieNode.BranchNode) node).setBranch(branchKey, branch);
                }
            }
            lastRoot = formatNode(node, stack.size() == 0, toSave, false);
        }

        if (lastRoot != null) {
            this.root = lastRoot.get(0)[0];
        }

        updateDB(toSave);
    }

    private enum Operation{
        PUT,
        DELETE
    }

    private static class DBUpdate{
        byte[] rlpNode;
        byte[] key;
        Operation op;
        private DBUpdate(Operation op, byte[] key, byte[] node) {
            this.op = op;
            this.key = key;
            this.rlpNode = node;
        }
    }

    private void updateDB(List<DBUpdate> stack) {
        for(DBUpdate dbUpdate:stack) {
            switch(dbUpdate.op){
                case PUT:this.db.put(HexConverter.bytesToHex(dbUpdate.key), dbUpdate.rlpNode);
                break;
                case DELETE:this.db.remove(HexConverter.bytesToHex(dbUpdate.key));
                break;
                default:
            }
        }
    }

    private TrieNode lookupNode(byte[] bytes) throws MPTException {
        var value = this.db.get(HexConverter.bytesToHex(bytes));
        if(value != null) {
            return TrieNode.decode(value);
        } else {
            throw new MPTException("missing node in db");
        }
    }

    private TrieNode lookupNode(byte[][] bytes) throws MPTException {
        if(bytes.length == 1 || bytes[1].length == 0)
           return lookupNode(bytes[0]);
        return TrieNode.decodeRaw(bytes);
    }

    private TrieNode lookupNode(List<Object> bytes) throws MPTException {
        return TrieNode.decodeRaw(bytes);
    }

    private List<byte[][]> formatNode(TrieNode node, boolean topLevel, List<DBUpdate> opStack, boolean remove) {
       byte[] rlpNode = node.encodeRLP();

        if (rlpNode.length >= 32 || topLevel) {
            List<byte[][]> hashRoot = new ArrayList<>();
            hashRoot.add(new byte[][]{Keccak256Hash(rlpNode)});

            if(remove)
                opStack.add(new DBUpdate(Operation.DELETE, hashRoot.get(0)[0], null));
            else
                opStack.add(new DBUpdate(Operation.PUT, hashRoot.get(0)[0], rlpNode));

            return hashRoot;
        }

        return node.getRaw();
    }

    public static byte[] Keccak256Hash(byte[] data) {
        return Context.hash("keccak-256", data);
    }
}
