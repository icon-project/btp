/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.bmv.icon;

import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import java.util.Arrays;

public class MerklePatriciaTree {
    public static class MPTException extends RuntimeException {
        public MPTException(String message) {
            super(message);
        }

        public MPTException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static byte[] encodeKey(Object key) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.write(key);
        return writer.toByteArray();
    }

    public static byte[] prove(byte[] rootHash, byte[] key, byte[][] proofs) {
        byte[] nibbles = bytesToNibbles(key, 0, null);
        Node node = new Node(rootHash);
        return node.prove(nibbles, proofs, 0);
    }

    public static byte[] bytesToNibbles(byte[] bytes, int from, byte[] nibbles) {
        int len = (bytes.length - from) * 2;
        if (nibbles != null) {
            len += nibbles.length;
        }
        byte[] ret = new byte[len];
        int j = 0;
        if (nibbles != null) {
            System.arraycopy(nibbles, 0, ret, 0, nibbles.length);
            j = nibbles.length;
        }
        for (int i = from; i < bytes.length; i++) {
            ret[j++] = (byte)(bytes[i] >> 4 & 0x0F);
            ret[j++] = (byte)(bytes[i] & 0x0F);
        }
        return ret;
    }

    public static class Node {
        private byte[] hash;
        private byte[] nibbles;
        private Node[] children;
        private byte[] serialized;
        private byte[] data;

        public Node() {}

        public Node(byte[] hash) {
            this.hash = hash;
        }

        public static Node readObject(ObjectReader reader) {
            Node obj = new Node();
            reader.beginList();
            Object[] arr = new Object[17];
            int i = 0;
            while(reader.hasNext()) {
                try {
                    arr[i] = reader.readByteArray();
                } catch (IllegalStateException e) {
                    if (i < 16) {
                        arr[i] = readObject(reader);
                    } else {
                        throw new MPTException("decode failure, branchNode.data required byte[]");
                    }
                }
                i++;
            }
            reader.end();
            if (i == 2) {
                if (arr[0] instanceof byte[] && arr[1] instanceof byte[]) {
                    byte[] header = (byte[])arr[0];
                    int prefix = header[0] & 0xF0;
                    byte[] nibbles = null;
                    if ((prefix & 0x10) != 0) {
                        nibbles = new byte[]{(byte) (header[0] & 0x0F)};
                    }
                    obj.nibbles = bytesToNibbles(header, 1, nibbles);
                    if ((prefix & 0x20) != 0) {
                        obj.data = (byte[])arr[1];
                    } else {
                        Node node = new Node((byte[])arr[1]);
                        obj.children = new Node[]{node};
                    }
                } else {
                    throw new MPTException("decode failure, required byte[]");
                }
            } else if (i == 17){
                obj.children = new Node[16];
                for(int j = 0; j < 16; j++) {
                    if (arr[j] instanceof Node) {
                        obj.children[j] = (Node) arr[j];
                    } else if (arr[j] instanceof byte[]) {
                        byte[] bytes = (byte[]) arr[j];
                        if (bytes.length > 0) {
                            obj.children[j] = new Node(bytes);
                        }
                    } else {
                        throw new MPTException("decode failure, required byte[] or Node");
                    }
                }
                obj.data = (byte[])arr[16];
            } else {
                throw new MPTException("decode failure, invalid list length "+i);
            }
            return obj;
        }

        public static Node fromBytes(byte[] bytes) {
            ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
            return readObject(reader);
        }

        public byte[] prove(byte[] nibbles, byte[][] proofs, int i) {
            if (isHash()) {
                byte[] serialized = proofs[i];
                byte[] hash = hash(serialized);
                if (!Arrays.equals(this.hash, hash)) {
                    throw new MPTException("mismatch hash");
                }
                Node node = Node.fromBytes(serialized);
                node.hash = hash;
                node.serialized = serialized;
                return node.prove(nibbles, proofs, i+1);
            } else if (isExtension()) {
                int cnt = ArrayUtil.matchCount(this.nibbles, nibbles);
                if (cnt < this.nibbles.length) {
                    throw new MPTException("mismatch nibbles on extension");
                }
                return children[0].prove(Arrays.copyOfRange(nibbles, cnt, nibbles.length), proofs, i);
            } else if (isBranch()) {
                if(nibbles.length == 0) {
                    return data;
                } else {
                    Node node = children[nibbles[0]];
                    return node.prove(Arrays.copyOfRange(nibbles, 1, nibbles.length), proofs, i);
                }
            } else {
                int cnt = ArrayUtil.matchCount(this.nibbles, nibbles);
                if (cnt < nibbles.length) {
                    throw new MPTException("mismatch nibbles on leaf");
                }
                return data;
            }
        }

        static byte[] hash(byte[] bytes) {
            return Context.hash("sha3-256",bytes);
        }

        private boolean isHash() {
            return hash.length > 0 && serialized == null;
        }

        private boolean isExtension() {
            return children != null && children.length == 1;
        }

        private boolean isBranch() {
            return children != null && children.length == 16;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Node{");
            sb.append("hash=").append(StringUtil.toString(hash));
            sb.append(", nibbles=").append(StringUtil.toString(nibbles));
            sb.append(", children=").append(StringUtil.toString(children));
            sb.append(", serialized=").append(StringUtil.toString(serialized));
            sb.append(", data=").append(StringUtil.toString(data));
            sb.append('}');
            return sb.toString();
        }
    }

}
