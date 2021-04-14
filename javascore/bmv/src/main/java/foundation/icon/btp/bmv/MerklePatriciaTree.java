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
package foundation.icon.btp.bmv;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class MerklePatriciaTree {

    final static String RLPn = "RLPn";

    public static class Node {
        private byte[] hash;
        private byte[] bytes;
        private byte[] data;
        private byte prefix;
        private List<Byte> nibbles = new ArrayList<>();
        private List<Node> children = new ArrayList<>();

        public Node(byte[] hash, byte[] serialized) {
            if (serialized == null)
                bytes = new byte[]{};

            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, hash);
            assert reader != null;
            reader.beginList();
            List<byte[]> trie = new ArrayList<>();

            while (reader.hasNext())
                trie.add(reader.readByteArray());

            if (trie.size() == 2) {
                byte[] header = trie.get(0);
                prefix = (byte) (header[0] & 0xF0);

                if ((prefix & 0x10) != 0) // odd
                    nibbles.add((byte) (header[0] & 0xF0));

                byte[] tmp = new byte[header.length - 1];
                System.arraycopy(header, 1, tmp, 0, header.length);
                nibbles = MerklePatriciaTree.bytesToNibbbles(tmp, nibbles);

                if ((prefix & 0x20) == 0) { // leaf
                    Node node = new MerklePatriciaTree.Node(trie.get(1), null);
                    children.add(node);
                } else
                    data = trie.get(1);
            } else if (trie.size() == 17) {
                for (int i = 0; i < 16; i++) {
                    Node node = null;
                    if (trie.get(i)[0] >= 0xC0)
                        node = new MerklePatriciaTree.Node(new byte[0], trie.get(i));
                    else if (trie.get(i).length > 0)
                        node = new MerklePatriciaTree.Node(trie.get(i), null);
                }
                data = trie.get(16);
            } else
                throw new IllegalArgumentException("Invalid Merkel Patricia Tree encoded list size");
        }

        public boolean isHash(){
          return hash.length > 0 && bytes.length == 0;
        }

        public boolean isExtension() {
            return children.size() == 1;
        }

       public boolean isBranch() {
            return children.size() == 16;
       }

       public byte[] prove(List<Byte> nibbles, List<byte[]> proofs) {
            if(isHash()){
               byte[] serialized = proofs.get(0);
               byte[] nodeHash = Context.sha3_256(serialized);
               if(!this.hash.equals(nodeHash))
                   throw new IllegalArgumentException("Nibbles and extensions don't match");
               Node node = new MerklePatriciaTree.Node(nodeHash, serialized);
               return node.prove(nibbles, proofs.subList(1, proofs.size()));
            } else if (isExtension()) {
                int idx = MerklePatriciaTree.matchNibbles(this.nibbles, nibbles);
                return children.get(0).prove(nibbles.subList(idx, nibbles.size()), proofs);
            } else if(isBranch()) {
               if(nibbles.size() == 0)
                   return data;
               else {
                   Node node = children.get(nibbles.get(0));
                   return node.prove(nibbles.subList(1, nibbles.size()), proofs);
               }
            } else {
               int idx = MerklePatriciaTree.matchNibbles(this.nibbles, nibbles);
               return data;
           }
       }
    }

    private static int matchNibbles(List<Byte> src, List<Byte> dst) {
        int len = Integer.min(src.size(), dst.size());
        for (int i = 0; i < len; i++) {
            if (!src.get(i).equals(dst.get(i)))
                return i;
        }
        return len;
    }

    private static List<Byte> bytesToNibbbles(byte[] bytes, List<Byte> nibbles){
        if(nibbles == null)
            nibbles = new ArrayList<>();
        for(byte b:bytes) {
            nibbles.add((byte) ((b >> 4) & 0x0f));
            nibbles.add((byte) (b & 0x0f));
        }

      return nibbles;
    }

    public static byte[] prove(byte[] root, byte[] key, byte[][] proofs){
        List<Byte> nibbles = MerklePatriciaTree.bytesToNibbbles(root, null);
        Node node = new Node(key, root);
        List<byte[]> prfList = new ArrayList<>();
        for(byte[] prf : proofs)
            prfList.add(prf);
        return node.prove(nibbles, prfList);
    }

}
