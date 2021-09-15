package foundation.icon.btp.bmv.lib.mpt;

import foundation.icon.btp.bmv.lib.BytesUtil;
import foundation.icon.btp.bmv.lib.Pair;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public abstract class TrieNode {
    final static String RLPn = "RLPn";

    public abstract byte[] getKey();
    public abstract void setKey(byte[] key);
    public abstract byte[] encodeRLP();
    //public abstract Pair<byte[][], byte[]> getRaw();
    public abstract List<byte[][]> getRaw();

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
        //private byte[][] branches;
        private List<byte[][]> branches;

        public BranchNode() {
            //this.branches = new byte[16][];
            this.branches = new ArrayList<>(16);
            for(int i=0; i < 16; i++) {
                this.branches.add(null);
            }
            this.value = null;
        }

        public static BranchNode fromBytes(byte[][] bytes) {
            BranchNode node = new BranchNode();
            //node.branches = ArraysUtil.copyOfRangeByteArray(bytes, 0, 16);
            node.value = bytes[16];
            return node;
        }

        public byte[] getBranch(int index) {
            byte[] b = branches.get(index)[0];
            if (BytesUtil.isEmptyOrNull(b))
                return b;
            else
                return null;
        }

        public void setBranch(int index, byte[][] branch) {
            this.branches.set(index, branch);
        }

        public List<Pair<Integer, byte[]>> getChildren() {
            List<Pair<Integer, byte[]>> children = new ArrayList<Pair<Integer, byte[]>>();
            for(int i = 0; i < 16; i++) {
                byte[] b = branches.get(i)[0];
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

        @Override
        public List<byte[][]> getRaw() {
            List<byte[][]> raw = new ArrayList<>();
            for(byte[][] b : branches){
                raw.add(b);
            }
            if (value != null)
                raw.add(new byte[][]{value});
            else
                raw.add(null);
            return raw;
        }

        public byte[] encodeRLP() {
            ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            writer.beginNullableList(17);
            for (byte[][] branch : branches){
                if (branch == null) {
                    writer.write(new byte[]{});
                } else {
                    writer.beginList(2);
                    writer.writeNullable(branch[0]);
                    writer.writeNullable(branch[1]);
                    writer.end();
                }
            }
            if(value == null)
                writer.write(new byte[]{});
            else
                writer.write(value);
            writer.end();
            return writer.toByteArray();
        }
     }

    public static class ExtensionNode extends TrieNode {
        private byte[] nibbles;
        private byte[] value;
        private List<byte[][]> values;

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
            return encodeKey(this.nibbles);
        }

        public void setValues(List<byte[][]> values) {
            this.value = null;
            this.values = values;
        }

        @Override
        public byte[] getKey() {
            return this.nibbles;
        }

        @Override
        public void setKey(byte[] key) {
            this.nibbles = key;
        }

        /*@Override
        public List<byte[][]> getRaw() {
            List<byte[][]> raw = new ArrayList<>();
            raw.add(new byte[][]{nibbles});
            raw.add(new byte[][]{value});
            return raw;
        }*/

        @Override
        public List<byte[][]> getRaw() {
            List<byte[][]> raw = new ArrayList<>();
            raw.add(new byte[][]{Nibbles.nibblesToBytes(encodedKey())});

            if(this.value != null)
               raw.add(new byte[][]{value});
            else for (byte[][] val : this.values)
                raw.add(val);

            return raw;
        }

        public byte[] encodeRLP() {
            ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            writer.beginList(2);
            writer.write(Nibbles.nibblesToBytes(encodedKey()));
            if (value != null) {
                writer.write(value);
                writer.end();
            } else if (values != null) {
              writer.beginNullableList(values.size());
              for (int i=0; i < this.values.size(); i++) {
                  byte[][] val = values.get(i);
                  if (val == null) {
                      writer.write(new byte[]{});
                  } else {
                      writer.beginList(2);
                      writer.writeNullable(val[0]);
                      writer.writeNullable(val[1]);
                      writer.end();
                  }
              }
             writer.end();
            } else {
                writer.write(new byte[]{});
            }
            writer.end();
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

        @Override
        public List<byte[][]> getRaw() {
            List<byte[][]> raw = new ArrayList<>();
            raw.add(new byte[][]{Nibbles.nibblesToBytes(encodedKey())});
            raw.add(new byte[][]{value});
            return raw;
        }

        public byte[] encodeRLP() {
            ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            writer.beginList(2);
            writer.write(Nibbles.nibblesToBytes(encodedKey()));
            writer.write(value);
            writer.end();
            return (writer).toByteArray();
        }
    }

}
