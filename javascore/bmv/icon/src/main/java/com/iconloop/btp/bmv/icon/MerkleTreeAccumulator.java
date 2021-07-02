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

package com.iconloop.btp.bmv.icon;

import com.iconloop.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class MerkleTreeAccumulator {
    private static final int HASH_LEN = 32;

    private long height;
    private byte[][] roots;
    private long offset;
    //optional reader.hasNext()
    private Integer rootSize;
    private Integer cacheSize;
    private byte[][] cache;
    private Boolean allowNewerWitness;
    //
    private Integer cacheIdx;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public byte[][] getRoots() {
        return roots;
    }

    public void setRoots(byte[][] roots) {
        this.roots = roots;
    }

    public Integer getRootSize() {
        return rootSize;
    }

    public void setRootSize(Integer rootSize) {
        this.rootSize = rootSize;
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    public Integer getCacheIdx() {
        return cacheIdx;
    }

    public void setCacheIdx(Integer cacheIdx) {
        this.cacheIdx = cacheIdx;
    }

    public byte[][] getCache() {
        return cache;
    }

    public void setCache(byte[][] cache) {
        this.cache = cache;
    }

    public Boolean getAllowNewerWitness() {
        return allowNewerWitness;
    }

    public void setAllowNewerWitness(Boolean allowNewerWitness) {
        this.allowNewerWitness = allowNewerWitness;
    }

    public boolean isAllowNewerWitness() {
        return allowNewerWitness != null && allowNewerWitness;
    }

    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        byte[] data = new byte[HASH_LEN * 2];
        System.arraycopy(b1, 0, data, 0, HASH_LEN);
        System.arraycopy(b2, 0, data, HASH_LEN, HASH_LEN);
        return Context.hash("sha3-256", data);
    }

    private static void verify(byte[][] witness, int witnessLen, byte[] root, byte[] hash, long idx) {
        for (int i = 0; i < witnessLen; i++) {
            if (idx % 2 == 0) {
                hash = concatAndHash(hash, witness[i]);
            } else {
                hash = concatAndHash(witness[i], hash);
            }
            idx = idx / 2;
        }
        if (!Arrays.equals(root, hash)) {
            throw new MTAException("invalid witness"+
                    ", root: "+StringUtil.toString(root) + ", hash: "+StringUtil.toString(hash));
        }
    }

    public void verify(byte[][] witness, byte[] hash, long height, long at) {
        if (this.height == at) {
            byte[] root = getRoot(witness.length);
            verify(witness, witness.length, root, hash, height - 1 - offset);
        } else if (this.height < at) {
            if (!isAllowNewerWitness()) {
                throw new MTAException.InvalidWitnessNewerException("not allowed newer witness");
            }
            if (this.height < height) {
                throw new MTAException("given witness for newer node");
            }
            int rootIdx = getRootIdxByHeight(height);
            byte[] root = getRoot(rootIdx);
            verify(witness, rootIdx, root, hash, height - 1 - offset);
        } else {
            // acc: new, wit: old
            // rebuild witness is not supported, but able to verify by cache if enabled
            if (isCacheEnabled() && (this.height - height - 1) < cacheSize) {
                if (!hasCache(hash)) {
                    throw new MTAException("invalid old witness");
                }
            } else {
                throw new MTAException.InvalidWitnessOldException("not allowed old witness");
            }
        }
    }

    private int getRootIdxByHeight(long height) {
        if (height <= offset) {
            throw new MTAException("given height is out of range");
        }
        long idx = height - 1 - offset;
        int rootIdx = (roots == null ? 0 : roots.length) - 1;
        while (rootIdx >= 0) {
            if (roots[rootIdx] != null) {
                long bitFlag = 1L << rootIdx;
                if (idx < bitFlag) {
                    break;
                }
                idx -= bitFlag;
            }
            rootIdx--;
        }
        if (rootIdx < 0) {
            throw new MTAException("given height is out of range");
        }
        return rootIdx;
    }

    private byte[] getRoot(int idx) {
        if (idx < 0 || roots == null || idx >= roots.length) {
            throw new MTAException("root idx is out of range");
        } else {
            return roots[idx];
        }
    }

    private void appendRoot(byte[] hash) {
        int len = roots == null ? 0 : roots.length;
        byte[][] roots = new byte[len + 1][];
        roots[len] = hash;
        this.roots = roots;
    }

    public boolean isRootSizeLimitEnabled() {
        return rootSize != null && rootSize > 0;
    }

    /**
     * call after update rootSize
     */
    public void ensureRoots() {
        if (isRootSizeLimitEnabled() && rootSize < this.roots.length) {
            byte[][] roots = new byte[rootSize][];
            int i = rootSize - 1;
            int j = this.roots.length - 1;
            while(i >= 0) {
                roots[i--] = this.roots[j--];
            }
            while(j >= 0) {
                if (this.roots[j] != null) {
                    addOffset(j--);
                }
            }
            this.roots = roots;
        }
    }

    public void add(byte[] hash) {
        putCache(hash);
        if (height == offset) {
            appendRoot(hash);
        } else {
            boolean isAdded = false;
            int len = roots == null ? 0 : roots.length;
            int pruningIdx = (isRootSizeLimitEnabled() ? rootSize : 0) - 1;
            for (int i = 0; i < len; i++) {
                if (roots[i] == null) {
                    roots[i] = hash;
                    isAdded = true;
                    break;
                } else {
                    if (i == pruningIdx) {
                        roots[i] = hash;
                        addOffset(i);
                        isAdded = true;
                        break;
                    } else {
                        hash = concatAndHash(roots[i], hash);
                        roots[i] = null;
                    }
                }
            }
            if (!isAdded) {
                appendRoot(hash);
            }
        }
        height++;
    }

    private void addOffset(int rootIdx) {
        long offset = (long) StrictMath.pow(2, rootIdx);
        this.offset += offset;
    }

    public boolean isCacheEnabled() {
        return cacheSize != null && cacheSize > 0;
    }

    /**
     * call after update cacheSize
     */
    public void ensureCache() {
        if (isCacheEnabled()) {
            if (cache == null) {
                cache = new byte[cacheSize][];
                cacheIdx = 0;
            } else if (cache.length != cacheSize) {
                byte[][] cache = new byte[cacheSize][];
                //copy this.cache to cache
                int len = this.cache.length;
                int src = this.cacheIdx;
                int dst = 0;
                for (int i = 0; i < len; i++) {
                    byte[] v = this.cache[src++];
                    if (src >= len) {
                        src = 0;
                    }
                    if (v == null) {
                        continue;
                    }
                    cache[dst++] = v;
                    if (dst >= cacheSize) {
                        dst = 0;
                        break;
                    }
                }
                this.cache = cache;
                this.cacheIdx = dst;
            }
        } else {
            cache = null;
        }
    }

    private boolean hasCache(byte[] hash) {
        if (isCacheEnabled()) {
            for (byte[] v : cache) {
                if (Arrays.equals(v, hash)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void putCache(byte[] hash) {
        if (isCacheEnabled()) {
            cache[cacheIdx++] = hash;
            if (cacheIdx >= cache.length) {
                cacheIdx = 0;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MerkleTreeAccumulator{");
        sb.append("height=").append(height);
        sb.append(", roots=").append(StringUtil.toString(roots));
        sb.append(", offset=").append(offset);
        sb.append(", rootSize=").append(rootSize);
        sb.append(", cacheSize=").append(cacheSize);
        sb.append(", cache=").append(StringUtil.toString(cache));
        sb.append(", allowNewerWitness=").append(allowNewerWitness);
        sb.append(", cacheIdx=").append(cacheIdx);
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, MerkleTreeAccumulator obj) {
        obj.writeObject(writer);
    }

    public static MerkleTreeAccumulator readObject(ObjectReader reader) {
        MerkleTreeAccumulator obj = new MerkleTreeAccumulator();
        reader.beginList();
        obj.setHeight(reader.readLong());
        if (reader.beginNullableList()) {
            byte[][] roots = null;
            List<byte[]> rootsList = new ArrayList<>();
            while(reader.hasNext()) {
                rootsList.add(reader.readNullable(byte[].class));
            }
            roots = new byte[rootsList.size()][];
            for(int i=0; i<rootsList.size(); i++) {
                roots[i] = (byte[])rootsList.get(i);
            }
            obj.setRoots(roots);
            reader.end();
        }
        obj.setOffset(reader.readLong());
        obj.setRootSize(reader.readNullable(Integer.class));
        obj.setCacheSize(reader.readNullable(Integer.class));
        if (reader.beginNullableList()) {
            byte[][] cache = null;
            List<byte[]> cacheList = new ArrayList<>();
            while(reader.hasNext()) {
                cacheList.add(reader.readNullable(byte[].class));
            }
            cache = new byte[cacheList.size()][];
            for(int i=0; i<cacheList.size(); i++) {
                cache[i] = (byte[])cacheList.get(i);
            }
            obj.setCache(cache);
            reader.end();
        }
        obj.setAllowNewerWitness(reader.readNullable(Boolean.class));
        if (reader.hasNext()) {
            obj.setCacheIdx(reader.readNullable(Integer.class));
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(8);
        writer.write(this.getHeight());
        byte[][] roots = this.getRoots();
        if (roots != null) {
            writer.beginNullableList(roots.length);
            for(byte[] v : roots) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.write(this.getOffset());
        writer.writeNullable(this.getRootSize());
        writer.writeNullable(this.getCacheSize());
        byte[][] cache = this.getCache();
        if (cache != null) {
            writer.beginNullableList(cache.length);
            for(byte[] v : cache) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.writeNullable(this.getAllowNewerWitness());
        writer.writeNullable(this.getCacheIdx());
        writer.end();
    }

    public static MerkleTreeAccumulator fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MerkleTreeAccumulator.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        MerkleTreeAccumulator.writeObject(writer, this);
        return writer.toByteArray();
    }

}
