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

public class BalancesEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return endowed(input);
            case (byte)(0x01):
                return dustLost(input);
            case (byte)(0x02):
                return transfer(input);
            case (byte)(0x03):
                return balanceSet(input);
            case (byte)(0x04):
                return deposit(input);
            case (byte)(0x05):
                return reserved(input);
            case (byte)(0x06):
                return unreserved(input);
            case (byte)(0x07):
                return reserveRepatriated(input);
        }
        return null;
    }

    public static byte[] endowed(ByteSliceInput input) {
        // accountId = input.take(32);
        // balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] dustLost(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] transfer(ByteSliceInput input) {
        // this.from = input.take(32);
        // this.to = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] balanceSet(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.free = ScaleReader.readU128(input);
        // this.reserved = ScaleReader.readU128(input);
        return input.take(32 + 16 + 16);
    }

    public static byte[] deposit(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] reserved(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] unreserved(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] reserveRepatriated(ByteSliceInput input) {
        // this.from = input.take(32);
        // this.to = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        // this.status = input.takeByte();
        return input.take(32 + 32 + 16 + 1);
    }
}