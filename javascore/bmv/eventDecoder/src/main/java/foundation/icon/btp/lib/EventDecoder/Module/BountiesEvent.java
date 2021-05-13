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

public class BountiesEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return bountyProposed(input);
            case (byte)(0x01):
                return bountyRejected(input);
            case (byte)(0x02):
                return bountyBecameActive(input);
            case (byte)(0x03):
                return bountyAwarded(input);
            case (byte)(0x04):
                return bountyClaimed(input);
            case (byte)(0x05):
                return bountyCanceled(input);
            case (byte)(0x06):
                return bountyExtended(input);
        }
        return null;
    }

    public static byte[] bountyProposed(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] bountyRejected(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        // this.balance = ScaleReader.readU128(input);
        return input.take(4 + 16);
    }

    public static byte[] bountyBecameActive(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] bountyAwarded(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        // this.accountId = input.take(32);
        return input.take(4 + 32);
    }

    public static byte[] bountyClaimed(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        // this.balance = ScaleReader.readU128(input);
        // this.accountId = input.take(32);
        return input.take(4 + 16 + 32);
    }

    public static byte[] bountyCanceled(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] bountyExtended(ByteSliceInput input) {
        // this.bountyIndex = ScaleReader.readU32(input);
        return input.take(4);
    }
}