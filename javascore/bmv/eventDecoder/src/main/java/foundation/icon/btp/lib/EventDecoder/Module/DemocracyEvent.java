package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;
import java.math.BigInteger;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class DemocracyEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return proposed(input);
            case (byte)(0x01):
                return tabled(input);
            case (byte)(0x02):
                return externalTabled(input);
            case (byte)(0x03):
                return started(input);
            case (byte)(0x04):
                return passed(input);
            case (byte)(0x05):
                return notPassed(input);
            case (byte)(0x06):
                return cancelled(input);
            case (byte)(0x07):
                return executed(input);
            case (byte)(0x08):
                return delegated(input);
            case (byte)(0x09):
                return undelegated(input);
            case (byte)(0x0a):
                return vetoed(input);
            case (byte)(0x0b):
                return preimageNoted(input);
            case (byte)(0x0c):
                return preimageUsed(input);
            case (byte)(0x0d):
                return preimageInvalid(input);
            case (byte)(0x0e):
                return preimageMissing(input);
            case (byte)(0x0f):
                return preimageReaped(input);
            case (byte)(0x10):
                return unlocked(input);
            case (byte)(0x11):
                return blacklisted(input);
        }
        return null;
    }

    public static byte[] proposed(ByteSliceInput input) {
        // this.propIndex = ScaleReader.readU32(input);
        // this.balance = ScaleReader.readU128(input);
        return input.take(4 + 16);
    }

    public static byte[] tabled(ByteSliceInput input) {
        int startPoint = input.getOffset();
        long propIndex = ScaleReader.readU32(input);
        BigInteger balance = ScaleReader.readU128(input);
        int depositorSize = ScaleReader.readUintCompactSize(input);
        byte[] depositorByte = input.take(32*depositorSize);
        int endpoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endpoint - startPoint);
    }

    public static byte[] externalTabled(ByteSliceInput input) {
        return null;
    }

    public static byte[] started(ByteSliceInput input) {
        // this.referendumIndex = ScaleReader.readU32(input);
        // this.voteThreshold = input.takeByte();
        return input.take(4 + 1);
    }

    public static byte[] passed(ByteSliceInput input) {
        // this.referendumIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] notPassed(ByteSliceInput input) {
        // this.referendumIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] cancelled(ByteSliceInput input) {
        // this.referendumIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] executed(ByteSliceInput input) {
        // this.referendumIndex = ScaleReader.readU32(input);
        // this.isOk = input.takeByte();
        return input.take(4 + 1);
    }

    public static byte[] delegated(ByteSliceInput input) {
        // this.from = input.take(32);
        // this.to = input.take(32);
        return input.take(64);
    }

    public static byte[] undelegated(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] vetoed(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.proposalHash = input.take(32);
        // this.blockNumber = ScaleReader.readU32(input);
        return input.take(32 + 32 + 4);
    }

    public static byte[] preimageNoted(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] preimageUsed(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] preimageInvalid(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.referendumIndex = ScaleReader.readU32(input);
        return input.take(32 + 4);
    }

    public static byte[] preimageMissing(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.referendumIndex = ScaleReader.readU32(input);
        return input.take(32 + 4);
    }

    public static byte[] preimageReaped(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.provider = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        // this.reaper = input.take(32);
        return input.take(32 + 32 + 16 + 32);
    }

    public static byte[] unlocked(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] blacklisted(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        return input.take(32);
    }
}