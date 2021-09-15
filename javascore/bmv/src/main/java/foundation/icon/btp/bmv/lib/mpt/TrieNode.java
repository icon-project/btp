package foundation.icon.btp.bmv.lib.mpt;

import a.ByteArray;
import foundation.icon.btp.bmv.lib.ArraysUtil;
import foundation.icon.btp.bmv.lib.BytesUtil;
import foundation.icon.btp.bmv.lib.Pair;
import foundation.icon.ee.io.RLPDataWriter;
import pi.ObjectWriterImpl;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public abstract class TrieNode {
    final static String RLPn = "RLPn";

    public abstract byte[] getKey();
    public abstract void setKey(byte[] key);
    public abstract byte[] encodeRLP();

    private byte[] value;
    public byte[] getValue() {
        return value;
    }
    public void setValue(byte[] value){
        this.value = value;
    }

    public byte[] hash() {
        return Trie.Keccak256Hash(encodeRLP());
    }

    public static TrieNode decodeRaw(byte[][] raw) throws MPTException {
        if (raw.length == 17) {
            return BranchNode.fromBytes(raw);
        } else if (raw.length == 2) {
            var nibbles = Nibbles.bytesToNibbles(raw[0]);
            if (Nibbles.isTerminator(nibbles)) {
                return new LeafNode(LeafNode.decodeKey(nibbles), raw[1]);
            }
            return new ExtensionNode(ExtensionNode.decodeKey(nibbles), raw[1]);
        } else {
            throw new MPTException("Decode err: invalid node");
        }
    }

    public static TrieNode decode(byte[] serialized) throws MPTException {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        assert reader != null;
        reader.beginList();
        List<byte[]> trie = new ArrayList<>();
        while (reader.hasNext())
            trie.add(reader.readByteArray());
        reader.end();

        byte[][] raw = new byte[trie.size()][];
        for(int i=0; i < trie.size(); i++) {
            raw[i] = trie.get(i);
        }
        return decodeRaw(raw);
    }

    public static class BranchNode extends TrieNode {
        private byte[] value;
        private byte[][] branches;

        public BranchNode() {
            this.branches = new byte[16][];
            this.value = null;
        }

        public static BranchNode fromBytes(byte[][] bytes) {
            BranchNode node = new BranchNode();
            node.branches = ArraysUtil.copyOfRangeByteArray(bytes, 0, 16);
            node.value = bytes[16];
            return node;
        }

        public byte[] getBranch(int index) {
            byte[] b = branches[index];
            if (BytesUtil.isEmptyOrNull(b))
                return b;
            else
                return null;
        }

        public List<Pair<Integer, byte[]>> getChildren() {
            List<Pair<Integer, byte[]>> children = new ArrayList<Pair<Integer, byte[]>>();
            for(int i = 0; i < 16; i++) {
                byte[] b = branches[i];
                if (BytesUtil.isEmptyOrNull(b))
                    children.add(Pair.of(i, b));
            }
            return children;
        }

        @Override
        public byte[] getKey() {
            return new byte[0];
        }

        @Override
        public void setKey(byte[] key) {

        }

        public byte[] encodeRLP() {
            //ObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            ObjectWriterImpl writer = new ObjectWriterImpl(new RLPDataWriter());
            writer.avm_beginList(2);
            writer.avm_beginList(16);
            for (byte[] branch : branches){
                writer.avm_write(ByteArray.newWithCharge(branch));
            }
            writer.avm_end();
            writer.avm_write(ByteArray.newWithCharge(value));
            writer.avm_end();
            return (writer).toByteArray();
        }

    }

    public static class ExtensionNode extends TrieNode {
        private byte[] nibbles;
        private byte[] value;

        public ExtensionNode(byte[] nibbles, byte[] value) {
            this.nibbles = nibbles;
            this.value = value;
        }

        public static byte[] encodeKey(byte[] key) {
            return Nibbles.addHexPrefix(key, false);
        }

        public static byte[] decodeKey(byte[] key) {
            return Nibbles.removeHexPrefix(key);
        }

        public byte[] encodedKey() {
            return encodeKey(encodeKey(this.nibbles));
        }

        @Override
        public byte[] getKey() {
            return new byte[0];
        }

        @Override
        public void setKey(byte[] key) {
            this.nibbles = key;
        }

        public byte[] encodeRLP() {
            //ObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            ObjectWriterImpl writer = new ObjectWriterImpl(new RLPDataWriter());
            writer.avm_beginList(2);
            writer.avm_write(ByteArray.newWithCharge(Nibbles.nibblesToBytes(encodedKey())));
            writer.avm_write(ByteArray.newWithCharge(value));
            writer.avm_end();
            return writer.toByteArray();
        }

    }

    public static class LeafNode extends TrieNode {
        private byte[] nibbles;
        private byte[] value;

        public LeafNode(byte[] nibbles, byte[] value) {
            this.nibbles = nibbles;
            this.value   = value;
        }

        public static byte[] encodeKey(byte[] key) {
            return Nibbles.addHexPrefix(key, true);
        }

        public static byte[] decodeKey(byte[] key) {
            return Nibbles.removeHexPrefix(key);
        }

        public byte[] encodedKey() {
            return encodeKey(this.nibbles);
        }

        @Override
        public byte[] getKey() {
            return nibbles;
        }

        @Override
        public void setKey(byte[] key) {
            this.nibbles = key;
        }

        public byte[] encodeRLP() {
            //ObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            ObjectWriterImpl writer = new ObjectWriterImpl(new RLPDataWriter());
            writer.avm_beginList(2);
            writer.avm_write(ByteArray.newWithCharge(Nibbles.nibblesToBytes(encodedKey())));
            writer.avm_write(ByteArray.newWithCharge(value));
            writer.avm_end();
            return (writer).toByteArray();
            //return ((ObjectWriterImpl)writer).toByteArray();
        }
    }

}
