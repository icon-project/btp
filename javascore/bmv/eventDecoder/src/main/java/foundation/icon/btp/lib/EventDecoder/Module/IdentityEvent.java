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

public class IdentityEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return identitySet(input);
            case (byte)(0x01):
                return identityCleared(input);
            case (byte)(0x02):
                return identityKilled(input);
            case (byte)(0x03):
                return judgementRequested(input);
            case (byte)(0x04):
                return judgementUnrequested(input);
            case (byte)(0x05):
                return judgementGiven(input);
            case (byte)(0x06):
                return registrarAdded(input);
            case (byte)(0x07):
                return subIdentityAdded(input);
            case (byte)(0x08):
                return subIdentityRemoved(input);
            case (byte)(0x09):
                return subIdentityRevoked(input);
        }
        return null;
    }

    public static byte[] identitySet(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] identityCleared(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] identityKilled(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] judgementRequested(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.registrarIndex = ScaleReader.readU32(input);
        return input.take(32 + 4);
    }

    public static byte[] judgementUnrequested(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.registrarIndex = ScaleReader.readU32(input);
        return input.take(32 + 4);
    }

    public static byte[] judgementGiven(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.registrarIndex = ScaleReader.readU32(input);
        return input.take(32 + 4);
    }

    public static byte[] registrarAdded(ByteSliceInput input) {
        // this.registrarIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] subIdentityAdded(ByteSliceInput input) {
        // this.sub = input.take(32);
        // this.main = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] subIdentityRemoved(ByteSliceInput input) {
        // this.sub = input.take(32);
        // this.main = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] subIdentityRevoked(ByteSliceInput input) {
        // this.sub = input.take(32);
        // this.main = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);

    }
}