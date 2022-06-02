/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.bmv.btp;

public class Node {
    public static final int LEVEL_INIT = 0;
    public static final int LEVEL_LEAF = 1;
    public static final int LEVEL_BRANCH = 2;
    private Node left;
    private Node right;
    private byte[] value;
    private int level;
    private int numOfLeaf;

    public Node() {}

    public Node(Node left, Node right, int level, int numOfLeaf, byte[] value) {
        this.left = left;
        this.right = right;
        this.level = level;
        this.numOfLeaf = numOfLeaf;
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public int getNumOfLeaf() {
        return numOfLeaf;
    }

    Node add(int numOfLeaf, byte[] value) {
        if (level == LEVEL_INIT) {
            level = numberToLevel(numOfLeaf);
            this.numOfLeaf = numOfLeaf;
            this.value = value;
        } else {
            if (left != null && left.numOfLeaf != right.numOfLeaf) {
                right = right.add(numOfLeaf, value);
                this.numOfLeaf += numOfLeaf;
            } else {
                var right = new Node(null, null, numberToLevel(numOfLeaf), numOfLeaf, value);
                return new Node(
                        this,
                        right,
                        this.level + 1,
                        this.numOfLeaf + numOfLeaf,
                        new byte[0]
                );
            }
        }
        return this;
    }

    void ensureHash(boolean force) {
        if (level <= LEVEL_LEAF) return;
        if (force || value.length == 0) {
            if (left != null) left.ensureHash(force);
            if (right != null) right.ensureHash(force);
            value = concatAndHash(left.value, right.value);
        }
    }

    void verify() {
        if (level == LEVEL_LEAF) {
            if (numOfLeaf != 1) {
                throw BMVException.unknown("invalid numOfLeaf, expected: 1, value: " + numOfLeaf);
            }
            return;
        }
        if (left != null) {
            if (left.level < right.level) {
                throw BMVException.unknown("invalid level left : " + left.level + " right : " + right.level);
            }
            left.verify();
            if (level > LEVEL_BRANCH) {
                var v = 1 << (left.level - LEVEL_LEAF);
                if (v != left.numOfLeaf) {
                    throw BMVException.unknown("invalid numOfLeaf, expected : " + v + ", value : " + left.numOfLeaf);
                }
            }
            right.verify();
        }
    }

    private static int numberToLevel(int n) {
        if (n <= LEVEL_BRANCH) return n;
        int l = LEVEL_BRANCH + 1;
        for (int i = (n - 1) >> 2; i > 0; i = i >> 1 ) {
            l++;
        }
        return l;
    }

    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        int len = 0, accum = 0;
        if (b1 != null) len += b1.length;
        if (b2 != null) len += b2.length;
        byte[] data = new byte[len];
        if (b1 != null) {
            System.arraycopy(b1, 0, data, 0, b1.length);
            accum += b1.length;
        }
        if (b2 != null) {
            System.arraycopy(b2, 0, data, accum, b2.length);
        }
        return BTPMessageVerifier.hash(data);
    }
}
