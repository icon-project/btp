package foundation.icon.btp.lib.blockupdate;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;

import score.Context;
import score.ObjectReader;

public class BlockHeader {
    private final byte[] hash;
    private final byte[] parentHash;
    private final long number;
    private final byte[] stateRoot;
    private final byte[] extrinsicsRoot;
    private final byte[] digest;

    public BlockHeader(byte[] serialized) {
        this.hash = this.getHashDigest(serialized);
        ByteSliceInput input = new ByteSliceInput(serialized);
        this.parentHash = input.take(32); // 32 bytes hash
        this.number = ScaleReader.readUintCompactSize(input);// u32 compact number
        this.stateRoot = input.take(32);
        this.extrinsicsRoot = input.take(32);
        this.digest = input.remain();
    }

    private byte[] getHashDigest(byte[] data) {
        return Hash.getHash(data);
    }

    public byte[] getHash() {
        return this.hash;
    }

    public byte[] getParentHash() {
        return this.parentHash;
    }

    public long getNumber() {
        return this.number;
    }

    public byte[] getStateRoot() {
        return this.stateRoot;
    }
}