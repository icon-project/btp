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
    public abstract List<byte[][]> getRaw();
    public abstract byte[] getValue();
    public abstract void setValue(byte[] value);

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

    public static TrieNode decodeRaw(List<Object> raw) throws MPTException {
        if (raw.size() == 17) {
            return BranchNode.fromList(raw);
        } else if (raw.size() == 2) {
            var _raw = (byte[][]) raw.get(0);
            var nibbles = Nibbles.bytesToNibbles(_raw[0]);
            if(raw.get(1) instanceof List){
                var _raw2 = (List<byte[][]>) raw.get(1);
                if (Nibbles.isTerminator(nibbles)) {
                    return new LeafNode(LeafNode.decodeKey(nibbles), _raw2.get(1)[0]);
                }
                return new ExtensionNode(ExtensionNode.decodeKey(nibbles), _raw2);
            } else{
                _raw = (byte[][]) raw.get(1);
                if (Nibbles.isTerminator(nibbles)) {
                    return new LeafNode(LeafNode.decodeKey(nibbles), _raw[0]);
                }
                return new ExtensionNode(ExtensionNode.decodeKey(nibbles), _raw[0]);
            }
        } else {
            throw new MPTException("Decode err: invalid node");
        }
    }

    public static TrieNode decode(byte[] serialized) throws MPTException {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", serialized);
        List<Object> raw = new ArrayList<>();

        reader.beginList();

        while (reader.hasNext())
            try {
                raw.add(new byte[][]{reader.readNullable(byte[].class)});
            } catch (IllegalStateException e) {
                break;
            }

        if(reader.hasNext()) {
            reader.beginList();
            List<byte[][]> lst = new ArrayList<>();
            while (reader.hasNext()) {
                try {
                    reader.readNullable(byte[].class);
                    lst.add(new byte[][]{});
                } catch(IllegalStateException e) {
                    reader.beginList();
                    lst.add(new byte[][]{reader.readByteArray(), reader.readByteArray()});
                    reader.end();
                }
            }
            raw.add(lst);
            reader.end();
        }

        reader.end();

        return decodeRaw(raw);
    }

    public static class BranchNode extends TrieNode {
        private byte[] value;
        private  List<Value> branches;

        public BranchNode() {
            this.branches = new ArrayList<>(16);
            for(int i=0; i < 16; i++) {
                this.branches.add(null);
            }
            this.value = null;
        }

        public BranchNode(List<byte[][]> _branches) {
            this.branches = new ArrayList<>(16);
            for(int i=0; i < 16; i++) {
                if(_branches.get(i) == null || _branches.get(i).length == 0)
                    this.branches.add(null);
                else
                    this.branches.add(new Value(_branches.get(i)));
            }
            if (_branches.get(0) != null && _branches.get(0).length > 0)
                this.value = _branches.get(16)[0];
        }

        public static BranchNode fromBytes(byte[][] bytes) {
            BranchNode node = new BranchNode();
            node.value = bytes[16];
            return node;
        }

        public static BranchNode fromList(List bytes) {
            return new BranchNode(bytes);
        }

        public byte[][] getBranch(int index) {
            if (branches.get(index) == null)
                return null;
            return branches.get(index).first();
        }

        public void setBranch(int index, byte[][] branch) {
            this.branches.set(index, new Value(branch));
        }

        public void setBranch(int index, List<byte[][]> branch) {
            this.branches.set(index, new Value(branch));
        }

        public List<Value> getChildren() {
            /*List<Pair<Integer, byte[]>> children = new ArrayList<Pair<Integer, byte[]>>();
            for(int i = 0; i < 16; i++) {
                byte[] b = branches.get(i).first();
                if (BytesUtil.isEmptyOrNull(b))
                    children.add(Pair.of(i, b));
            }
            return children;*/
            return null;
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
            for(Value b : branches){
                if (b == null)
                    raw.add(null);
                else
                    raw.add(b.first());
            }
            if (value != null)
                raw.add(new byte[][]{value});
            else
                raw.add(null);
            return raw;
        }

        @Override
        public byte[] getValue() {
            return this.value;
        }

        @Override
        public void setValue(byte[] value) {
            this.value = value;
        }

        public byte[] encodeRLP() {
            ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
            writer.beginNullableList(17);
            for (Value branch : branches){
                if (branch == null || branch.isEmpty()) {
                    writer.write(new byte[]{});
                } else {
                    writer.beginList(2);
                    writer.writeNullable(branch.first()[0]);
                    writer.writeNullable(branch.first()[1]);
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

        public ExtensionNode(byte[] nibbles, List<byte[][]> values) {
            this.nibbles = nibbles;
            this.values = values;
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

        @Override
        public byte[] getValue() {
            return this.value;
        }

        @Override
        public void setValue(byte[] value) {
            this.value = value;
        }

        public void setValues(List<byte[][]> values) {
            this.value = null;
            this.values = values;
        }

        public List<byte[][]> getValues() {
            return this.values;
        }

        @Override
        public byte[] getKey() {
            return this.nibbles;
        }

        @Override
        public void setKey(byte[] key) {
            this.nibbles = key;
        }

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
                  if (val == null || val.length == 0) {
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
        public byte[] getValue() {
            return this.value;
        }

        @Override
        public void setValue(byte[] value) {
            this.value = value;
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
