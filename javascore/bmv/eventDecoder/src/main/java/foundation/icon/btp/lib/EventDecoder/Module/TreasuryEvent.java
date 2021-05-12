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

public class TreasuryEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return proposed(input);
            case (byte)(0x01):
                return spending(input);
            case (byte)(0x02):
                return awarded(input);
            case (byte)(0x03):
                return rejected(input);
            case (byte)(0x04):
                return burnt(input);
            case (byte)(0x05):
                return rollover(input);
            case (byte)(0x06):
                return deposit(input);
        }
        return null;
    }

    public static byte[] proposed(ByteSliceInput input) {
        // this.proposalIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] spending(ByteSliceInput input) {
        // this.balance = ScaleReader.readU128(input);
        return input.take(16);
    }

    public static byte[] awarded(ByteSliceInput input) {
        // this.proposalIndex = ScaleReader.readU32(input);
        // this.balance = ScaleReader.readU128(input);
        // this.accountId = input.take(32);
        return input.take(4 + 16 + 32);
    }

    public static byte[] rejected(ByteSliceInput input) {
        // this.proposalIndex = ScaleReader.readU32(input);
        // this.balance = ScaleReader.readU128(input);
        return input.take(4 + 16);
    }

    public static byte[] burnt(ByteSliceInput input) {
        // this.balance = ScaleReader.readU128(input);
        return input.take(16);
    }

    public static byte[] rollover(ByteSliceInput input) {
        // this.balance = ScaleReader.readU128(input);
        return input.take(16);
    }

    public static byte[] deposit(ByteSliceInput input) {
        // this.balance = ScaleReader.readU128(input);
        return input.take(16);
    }
}