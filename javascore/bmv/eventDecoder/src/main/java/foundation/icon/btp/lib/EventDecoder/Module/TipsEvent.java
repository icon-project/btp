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

public class TipsEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return newTip(input);
            case (byte)(0x01):
                return tipClosing(input);
            case (byte)(0x02):
                return tipClosed(input);
            case (byte)(0x03):
                return tipRetracted(input);
            case (byte)(0x04):
                return tipSlashed(input);
        }
        return null;
    }

    public static byte[] newTip(ByteSliceInput input) {
        // this.tipHash = input.take(32);
        return input.take(32);
    }

    public static byte[] tipClosing(ByteSliceInput input) {
        // this.tipHash = input.take(32);
        return input.take(32);
    }

    public static byte[] tipClosed(ByteSliceInput input) {
        // this.tipHash = input.take(32);
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }

    public static byte[] tipRetracted(ByteSliceInput input) {
        // this.tipHash = input.take(32);
        return input.take(32);
    }

    public static byte[] tipSlashed(ByteSliceInput input) {
        // this.tipHash = input.take(32);
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 16);
    }
}