package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class AssetsEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return created(input);
            case (byte)(0x01):
                return issued(input);
            case (byte)(0x02):
                return transferred(input);
            case (byte)(0x03):
                return burned(input);
            case (byte)(0x04):
                return teamChanged(input);
            case (byte)(0x05):
                return ownerChanged(input);
            case (byte)(0x06):
                return frozen(input);
            case (byte)(0x07):
                return thawed(input);
            case (byte)(0x08):
                return destroyed(input);
            case (byte)(0x09):
                return forceCreated(input);
            case (byte)(0x0a):
                return maxZombiesChanged(input);
        }
        return null;
    }

    public static byte[] created(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.creator = input.take(32);
        // this.owner = input.take(32);
        return input.take(4 + 32 + 32);
    }

    public static byte[] issued(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        // this.balance = ScaleReader.readU64(input);
        return input.take(4 + 32 + 8);
    }

    public static byte[] transferred(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.from = input.take(32);
        // this.to = input.take(32);
        // this.balance = ScaleReader.readU64(input);
        return input.take(4 + 32 + 32 + 8);
    }

    public static byte[] burned(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        // this.balance = ScaleReader.readU64(input);
        return input.take(4 + 32 + 8);
    }

    public static byte[] teamChanged(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.issuer = input.take(32);
        // this.admin = input.take(32);
        // this.freezer = input.take(32);
        return input.take(4 + 32 + 32 + 32);
    }

    public static byte[] ownerChanged(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        return input.take(4 + 32);
    }

    public static byte[] frozen(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        return input.take(4 + 32);
    }

    public static byte[] thawed(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        return input.take(4 + 32);
    }

    public static byte[] destroyed(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] forceCreated(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.owner = input.take(32);
        return input.take(4 + 32);
    }

    public static byte[] maxZombiesChanged(ByteSliceInput input) {
        // this.assetId = ScaleReader.readU32(input);
        // this.max = ScaleReader.readU32(input);
        return input.take(4 + 4);
    }
}