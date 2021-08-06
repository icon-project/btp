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
package foundation.icon.btp.bmv.lib.mta;

import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Merkel Tree Accumulator which keeps track of block witnesses hashes and verifies blocks
 */
public class MerkleTreeAccumulator {

    final static String RLPn = "RLPn";
    final static byte[] EMPTY_NODE = new byte[]{};

    private long offset;
    private long height;
    private int rootsSize;
    private int cacheSize;

    private List<byte[]> roots;
    private List<byte[]> caches;

    private boolean allowNewerWitness = false;

    public MerkleTreeAccumulator(long height,
                                 long offset,
                                 int rootSize,
                                 int cacheSize,
                                 boolean isAllowNewerWitness,
                                 List<byte[]> roots,
                                 List<byte[]> caches) {
        this.height = height;
        this.roots = roots;
        this.offset = offset;
        this.height = height;
        this.rootsSize = rootSize;
        this.cacheSize = cacheSize;
        this.caches = caches;
        this.allowNewerWitness = isAllowNewerWitness;
        if (roots != null && roots.size() > 0) {
            this.roots = roots;
        } else {
            this.roots = new ArrayList<byte[]>(rootSize);
        }

        if (caches != null && caches.size() > 0) {
            this.caches = caches;
        } else {
            this.caches = new ArrayList<byte[]>(cacheSize);
        }

        if (this.height == 0 && this.offset > 0)
            this.height = this.offset;
    }

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] c = new byte[left.length + right.length];
        System.arraycopy(left, 0, c, 0, left.length);
        System.arraycopy(right, 0, c, left.length, right.length);
        return c;
    }

    private static byte[][] subArray(byte[][] arr, int index) {
        byte[][] c = new byte[index + 1][];
        System.arraycopy(arr, 0, c, 0, index + 1);
        return c;
    }

    private static int pow(int base, int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result = result * base;
        }
        return result;
    }

    public int getRootsSize() {
        return this.roots.size();
    }

    public void add(byte[] hash) {
        this.putCache(hash);
        if (this.height == 0 || roots.size() == 0)
            roots.add(hash);
        else {
            byte[] root = EMPTY_NODE;
            for (int i = 0; i < this.roots.size(); i++) {
                if (Arrays.equals(this.roots.get(i), null)
                        || Arrays.equals(this.roots.get(i), EMPTY_NODE)) {
                    root = hash;
                    this.roots.set(i, root);
                    break;
                } else {
                    if (this.rootsSize > 0 && this.rootsSize <= i + 1) {
                        root = hash;
                        this.roots.set(i, root);
                        offset = offset + pow(2, i);
                        break;
                    } else {
                        hash = Context.hash("keccak-256", concat(this.roots.get(i), hash));
                        this.roots.set(i, EMPTY_NODE);
                    }
                }
            }
            if (root == EMPTY_NODE) {
                root = hash;
                this.roots.add(root);
            }
        }
        this.height += 1;
        Context.require(this.height >= 0);
        //AccumulatorUpdate(Context.getCaller(), BigInteger.valueOf(this.height), hash);
    }

    public boolean newerWitnessAllowed() {
        return allowNewerWitness;
    }

    /*
       / when:
       / client MTA height > this.height
       / input height to verify < this.height
       */
    public int indexOfRootByHeight(long h) {
        long idx = h - 1 - this.offset;
        int rootIdx = 0;

        if (idx < 0)
            throw new IndexOutOfBoundsException("Index is out of bounds for height " + h);

        for (int i = this.roots.size(); ; ) {
            --i;
            if (this.roots.get(i).length == 0)
                continue;
            int bitflag = 1 << i;
            if (idx < bitflag) {
                rootIdx = i;
                break;
            }
            idx -= bitflag;
        }
        return rootIdx;
    }

    private void _verify(List<byte[]> witnesses, byte[] root, byte[] hash, long index) throws MTAException {
        byte[] _hash = hash;
        long idx = index;
        for (byte[] witness : witnesses) {
            if (idx % 2 == 0)
                _hash = Context.hash("keccak-256", concat(_hash, witness));
            else
                _hash = Context.hash("keccak-256", concat(witness, _hash));
            idx = idx / 2;
        }
        if (!Arrays.equals(_hash, root)) {
            throw new MTAException("Verification failed: Invalid Witness");
        }
    }

    public void verify(List<byte[]> witness, byte[] hash, long blockHeight, long cur) throws MTAException {
        if (this.height == cur) {
            byte[] root = this.roots.get(witness.size());
            _verify(witness, root, hash, blockHeight - 1 - offset);
        } else if (this.height < cur) {
            if (!this.newerWitnessAllowed())
                throw new MTAException("Verification failed: Newer witness not allowed");

            if (this.height < blockHeight)
                throw new MTAException("Verification failed: Given witness for newer node");

            int rootIndex = indexOfRootByHeight(blockHeight);
            byte[] root = this.roots.get(rootIndex);
            List<byte[]> acceptedWitness = new ArrayList<byte[]>(witness);
            while (acceptedWitness.size() > rootIndex) {
                acceptedWitness.remove(acceptedWitness.size() - 1);
            }
            //TODO: check the index param- blockheight
            _verify(acceptedWitness, root, hash, blockHeight - 1 - offset); // Unknown Witness
        } else {
            if (this.height - blockHeight - 1 < this.cacheSize) {
                if (!this.hasCache(hash))
                    throw new MTAException("Verification failed: Invalid old cached witness");
            } else {
                throw new MTAException("Verification failed: Cannot allow old cached witness");
            }

        }
    }

    public boolean hasCache(byte[] hash) {
        if (hash.length == 0)
            return false;

        for (int i = 0; i < this.caches.size(); i++) {
            if (Arrays.equals(this.caches.get(i), hash))
                return true;
        }
        return false;
    }


    private void putCache(byte[] hash) {
        if (this.cacheSize > 0) {
            if (this.caches.size() == this.cacheSize) {
                this.caches.remove(0);
            }
            this.caches.add(hash);
        }
    }


    public long getHeight() {
        return this.height;
    }


    public long getOffset() {
        return offset;
    }


    public byte[] getRoot(int index) {
        try {
            return this.roots.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public List<byte[]> getRoots() {
        return roots;
    }

    public boolean isAllowNewerWitness() {
        return allowNewerWitness;
    }

    public List<byte[]> getCaches() {
        return caches;
    }

    private void AccumulatorUpdate(Address addr, BigInteger Height, byte[] hash) {
    }

    public byte[] getHash(byte[] data) {
        return Context.hash("keccak-256", data);
    }

    public void setOffset(long offset) {
        Context.require(this.roots.size() == 0);
        this.offset = offset;
        if (this.offset > 0 && this.height < this.offset)
            this.height = this.offset;
    }

    // Below methods for javaloop to serialize
    public static void writeObject(ObjectWriter w, MerkleTreeAccumulator obj) {
        w.beginList(6);
        w.write(obj.getHeight());
        w.write(obj.getOffset());
        w.write(obj.getRootsSize());
        w.write(obj.getCacheSize());
        w.write(obj.isAllowNewerWitness());

        w.beginList(obj.getRootsSize());
        for (byte[] root : obj.getRoots()) {
            w.writeNullable(root);
        }
        w.end();

        w.beginList(obj.getCacheSize());
        for (byte[] cache : obj.getCaches()) {
            w.writeNullable(cache);
        }
        w.end();

        w.end();
    }


    public static MerkleTreeAccumulator readObject(ObjectReader r) {
        r.beginList();
        long height = r.readLong();
        long offset = r.readLong();
        int rootSize = r.readInt();
        int cacheSize = r.readInt();
        boolean isAllowNewerWitness = r.readBoolean();

        List<byte[]> roots = new scorex.util.ArrayList<>();
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] root = r.readNullable(byte[].class);
                roots.add(root);
            }
            r.end();
        }

        List<byte[]> caches = new scorex.util.ArrayList<>();
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] cache = r.readNullable(byte[].class);
                if (cache != null && cache.length > 0)
                    caches.add(cache);
            }
            r.end();
        }

        r.end();

        return new MerkleTreeAccumulator(
                height,
                offset,
                rootSize,
                cacheSize,
                isAllowNewerWitness,
                roots,
                caches
        );
    }


}
