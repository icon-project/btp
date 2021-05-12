package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class VestingEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return vestingUpdated(input);
            case (byte)(0x01):
                return vestingCompleted(input);
        }
        return null;
    }

    public static byte[] vestingUpdated(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] vestingCompleted(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }
}