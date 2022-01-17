package foundation.icon.btp.bmv.near.verifier.utils;

import foundation.icon.btp.bmv.near.verifier.types.Hash;

public class Hasher<H extends Hash> {
    private static final int HASH_LEN = 32;
    private final H hasher;

    public Hasher(H hasher) {  
        this.hasher = hasher;
    }

    public byte[] computeHash(byte[] data) {
        return this.hasher.computeHash(data);
    }

    public byte[] combineHash(byte[] a, byte[] b) {
        byte[] data = new byte[HASH_LEN * 2];
        System.arraycopy(a, 0, data, 0, HASH_LEN);
        System.arraycopy(b, 0, data, HASH_LEN, HASH_LEN);
        return this.hasher.computeHash(data);
    }
}
