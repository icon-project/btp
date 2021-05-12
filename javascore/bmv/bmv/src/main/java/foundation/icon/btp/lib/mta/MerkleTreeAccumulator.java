package foundation.icon.btp.lib.mta;

import java.util.List;

import foundation.icon.btp.lib.utils.Arrays;
import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.exception.mta.MTAException;
import foundation.icon.btp.lib.exception.mta.InvalidWitnessNewerException;
import foundation.icon.btp.lib.exception.mta.InvalidWitnessOldException;

import scorex.util.ArrayList;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class MerkleTreeAccumulator {
    private long height;
    private List<byte[]> roots;
    private long offset;
    private int rootSize;
    private int cacheSize;
    private List<byte[]> caches;
    private boolean isAllowNewerWitness;
    private byte[] lastBlockHash;

    public MerkleTreeAccumulator(
        long height,
        long offset,
        int rootSize,
        int cacheSize,
        boolean isAllowNewerWitness,
        byte[] lastBlockHash,
        List<byte[]> roots,
        List<byte[]> caches
    ) {
        this.height = height;
        this.offset = offset;
        this.rootSize = rootSize;
        this.cacheSize = cacheSize;
        this.isAllowNewerWitness = isAllowNewerWitness;
        this.lastBlockHash = lastBlockHash;

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

    public byte[] getHash(byte[] data) {
        return Context.hash("sha3-256", data);
    }

    public byte[] lastBlockHash() {
        return this.lastBlockHash;
    }

    private byte[] getConcatenationHash(byte[] item1, byte[] item2) {
        byte[] concatenation = new byte[item1.length + item2.length];
        System.arraycopy(item1, 0, concatenation, 0, item1.length);
        System.arraycopy(item2, 0, concatenation, item1.length, item2.length);  
        return this.getHash(concatenation);
    }

    public long height() {
        return this.height;
    }

    public long offset() {
        return this.offset;
    }

    public void setOffset(long offset) throws MTAException {
        if (this.roots.size() > 0) {
            throw new MTAException("not allow to set offset if roots is not empty");
        }

        this.offset = offset;
        if (this.offset > 0 && this.height < this.offset)
            this.height = this.offset;
    }

    public int rootSize() {
        return this.rootSize;
    }

    public int cacheSize() {
        return this.cacheSize;
    }

    public boolean isAllowNewerWitness() {
        return this.isAllowNewerWitness;
    }

    public byte[] getRoot(int idx) {
        return this.roots.get(idx);
    }

    public List<byte[]> getRootList() {
        return this.roots;
    }

    public List<byte[]> getCacheList() {
        return this.caches;
    }

    // return null if idx out of range
    public byte[] getNullableRoot(int idx) {
        try {
            return this.roots.get(idx);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    // public void setRootSize(int size) {
    //     if (size < 0)
    //         size = 0;
    //     this.roots.setSize(size);
    //     this.rootSize = size;
    // }

    // public void setCacheSize(int size) {
    //     if (size < 0)
    //         size = 0;
    //     this.cacheSize = size;
    //     if (this.caches.size() > this.cacheSize)
    //         this.caches.truncate(size);
    //     else 
    //         this.caches.setSize(size);
    // }

    public void setIsAllowNewerWitness(boolean allow) {
        this.isAllowNewerWitness = allow;
    }

    private boolean hasCache(byte[] hash) {
        if (hash == null) {
            for (int i = 0; i < this.caches.size(); i++)
                if (this.caches.get(i) == null)
                    return true;
        } else {
            for (int i = 0; i < this.caches.size(); i++)
                if (java.util.Arrays.equals(this.caches.get(i), hash))
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

    public byte[] getNullableCache(int idx) {
        try {
            return this.caches.get(idx);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void add(byte[] hash) {
        this.putCache(hash);
        this.lastBlockHash = hash;
        if (this.height == 0)
            this.roots.add(hash);
        else if (this.roots.size() == 0)
            this.roots.add(hash);
        else {
            byte[] root = null;
            for (int i = 0; i < this.roots.size(); i++) {
                byte[] currentRoot = this.roots.get(i);
                if (currentRoot == null || currentRoot.length == 0) {
                    root = new byte[32];
                    root = hash;
                    this.roots.set(i, root);
                    break;
                } else {
                    if (0 < this.rootSize && this.rootSize <= (i + 1)) {
                        root = new byte[32];
                        root = hash;
                        this.roots.set(i, root);
                        long offset = 1<<i; // equal to Math.pow(2, i)
                        this.offset += offset;
                        break;
                    } else { 
                        hash = this.getConcatenationHash(currentRoot, hash);
                        this.roots.set(i, null);
                    }
                }
            }
            if (root == null) {
                root = new byte[32];
                root = hash;
                this.roots.add(root);
            }
        }
        this.height +=1;
    }

    // public void dump() {
    //     Context.println("height:" + this.height + "| offset:" + this.offset);
    //     for (int i = 0; i < this.roots.size(); i++) {
    //         byte[] currentRoot = this.roots.get(i);
    //         if (currentRoot == null || currentRoot.length == 0)
    //             Context.println("root[" + i + "]: None");
    //         else
    //             Context.println("root[" + i + "]: " + Arrays.toString(currentRoot));
    //     }
    // }

    /*
    / get index of root in case
    / client MTA height > this.height
    / input height to verify < this.height
    */
    private int getRootIdxByHeight(long height) throws MTAException {
        long idx = height - 1 - this.offset;
        int rootIdx = 0;
        int i = this.roots.size();
        if (idx < 0)
            throw new MTAException("given height is out of range");
        while (i > 0) {
            i -= 1;
            if (this.roots.get(i) == null)
                continue;
            int bitFlag = 1 << i; // Math.pow(2, i)
            if (idx < bitFlag) {
                rootIdx = i;
                break;
            }
            idx -= bitFlag;
        }
        return rootIdx;
    }

    private void _verify(List<byte[]> witness, byte[] root, byte[] hash, long idx) throws MTAException {
        for (byte[] w : witness) {
            if (idx % 2 == 0) {
                hash = this.getConcatenationHash(hash, w);
            } else {
                hash = this.getConcatenationHash(w, hash);
            }
            idx = (int) idx/2;
        }

        if (!java.util.Arrays.equals(hash, root))
            throw new MTAException("invalid witness");
    }

    public void verify(List<byte[]> witness, byte[] hash, long height, long at) throws MTAException, InvalidWitnessNewerException, InvalidWitnessOldException {
        if (this.height == at) {
            byte[] root = this.getRoot(witness.size());
            this._verify(witness, root, hash, height - 1 - this.offset);
        } else if (this.height < at) {
            if (!this.isAllowNewerWitness)
                throw new InvalidWitnessNewerException("not allowed newer witness");

            if (this.height < height)
                throw new MTAException("given witness for newer node");

            int rootIdx = this.getRootIdxByHeight(height);
            byte[] root = this.getRoot(rootIdx);
            List<byte[]> acceptedWitness = new ArrayList<byte[]>(witness);
            while(acceptedWitness.size() > rootIdx) {
                acceptedWitness.remove(acceptedWitness.size() - 1);
            }
            this._verify(acceptedWitness, root, hash, height - 1 - this.offset);
        } else if (this.height > at) {
            if ((this.height - height - 1) < this.cacheSize) {
                if (!this.hasCache(hash))
                    throw new MTAException("invalid old witness");
            } else {
                throw new InvalidWitnessOldException("not allowed old witness");
            }
        }
    }

    public MTAStatus getStatus() {
        return new MTAStatus(this.height, this.offset);
    }
}
