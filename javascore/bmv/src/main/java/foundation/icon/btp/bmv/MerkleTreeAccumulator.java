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

import score.Address;
import score.ArrayDB;
import score.Context;
import score.ObjectReader;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Merkel Tree Accumulator which keeps track of block witnesses hashes and verifies blocks
 * */
public class MerkleTreeAccumulator {

    final static String RLPn = "RLPn";
    final static byte[] EMPTY_NODE = new byte[]{};

    private int offset = 0;
    private int height = 0;
    private int rootsSize = 0;
    private int cacheSize = 0;

    private ArrayDB<byte[]> roots;
    private ArrayDB<byte[]> cache;

    private boolean allowNewerWitness = false;

    public MerkleTreeAccumulator(byte[] mta) {
        roots = Context.newArrayDB("mta_roots", byte[].class);
        cache = Context.newArrayDB("mta_cache", byte[].class);

        if(mta != null || mta == EMPTY_NODE) {
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, mta);
            assert reader != null;

            reader.beginList(); // begin MTA
            this.height = reader.readInt();

            reader.beginList();

            while (reader.hasNext())
                roots.add(reader.readNullable(byte[].class));

            this.offset = reader.hasNext() ? reader.readInt() : 0;
            this.height = this.height == 0 && this.offset > 0 ? this.offset : height;

            this.rootsSize = reader.hasNext() ? reader.readInt() : 0;
            this.cacheSize = reader.hasNext() ? reader.readInt() : 0;

            if (reader.hasNext())  // cache list
                reader.beginList();

            while (reader.hasNext())
                cache.add(reader.readNullable(byte[].class));

            reader.end(); // end cache

            this.allowNewerWitness = reader.hasNext() && reader.readBoolean();

            reader.end(); // end MTA
        }
    }

    @External(readonly = true)
    public int getHeight() {
        return this.height;
    }

    @External(readonly = true)
    public byte[] getRoot(int index) { return this.roots.get(index);  }

    @External(readonly = true)
    public int getOffset() { return offset;  }

    @External(readonly = true)
    public int getRootsSize() {
        return this.roots.size();
    }

    @External(readonly = true)
    public int getCacheSize() {
        return cacheSize;
    }

    @External
    public void setRootsSize(int size) {
        this.rootsSize = Integer.max(size, 0);
        if (roots.size() > this.rootsSize)
            for(int i = roots.size(); i > this.rootsSize; i--)
                roots.removeLast();
    }

    @External
    public void setCacheSize(int size) {
        //TODO: FIXME setCacheSize
        this.cacheSize = Integer.max(size, 0);
        for(int i = cache.size(); i > this.cacheSize; i--)
            roots.removeLast();
    }

    public void setOffset(int offset){
        this.offset = offset;
    }

    public boolean hasCache(byte[] hash) {
        if (hash.length == 0)
            return false;

        for(int i=0; i < this.cache.size(); i++){
            if (Arrays.equals(this.cache.get(i), hash))
                return true;
        }
        return false;
    }

    public void putCache(byte[] hash) {
        if (cacheSize > 0)
            cache.add(hash);
        if (this.cache.size() > cacheSize)
            setCacheSize(cacheSize);
    }

    @External
    public void add(byte[] hash) {
        if(this.height == 0 || roots.size() == 0)
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
                    if (this.rootsSize > 0 && this.rootsSize <= i + 1){
                        root = hash;
                        this.roots.set(i, root);
                        offset = pow(2, i);
                        break;
                    } else {
                        hash  = Context.sha3_256(concat(this.roots.get(i), hash));
                        this.roots.set(i, EMPTY_NODE);
                    }
                }
            }
        }
        this.height += 1;
        Context.require(this.height >= 0);
        AccumulatorUpdate(Context.getCaller(), BigInteger.valueOf(this.height), hash);
    }

    public boolean newerWitnessAllowed() {
        return allowNewerWitness;
    }

    public int indexOfRootByHeight(int h) {
        int idx = height - 1 - this.offset;
        int rootIdx = 0;

        for(int i = this.roots.size();;) {
            if (idx < 0)
                throw new IndexOutOfBoundsException("Index is out of bounds for height " + h);
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

    private boolean _verify(byte[][] witnesses, byte[] root, byte[] hash, int index){
        byte[] _hash = hash;
        int idx = index;
        for (byte[] witness : witnesses) {
            if (idx % 2 == 0)
                _hash = Context.sha3_256(concat(_hash, witness));
            else
                _hash = Context.sha3_256(concat(witness, _hash));
            idx = idx / 2;
        }
        return Arrays.equals(_hash, root);
    }

    @External
    public void verify(byte[][] witness, byte[] hash, int blockHeight, int cur) {
        if (this.height == cur) {
            byte[] root = this.roots.get(witness.length);
            Context.require(_verify(witness, root, hash, blockHeight - 1 - offset));
        } else if (this.height < cur) {
            if (!this.newerWitnessAllowed())
                throw new IllegalStateException("Newer witness not allowed");

            if (this.height < blockHeight)
                throw new IllegalStateException("Not enough witnesses at given height");

            int rootIndex = indexOfRootByHeight(blockHeight);
            byte[] root = this.roots.get(rootIndex);
            Context.require(_verify( subArray(witness, rootIndex), root, hash, blockHeight)); // Unknown Witness
        } else {
            if (this.height - blockHeight - 1 < this.cacheSize)
                if(!this.hasCache(hash))
                    throw new IllegalStateException("Invalid cached witness");
                else
                    throw new IllegalStateException("Verification failed");
        }
    }

    @EventLog(indexed=1)
    private void AccumulatorUpdate(Address addr, BigInteger Height, byte[] hash) {}

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


}
