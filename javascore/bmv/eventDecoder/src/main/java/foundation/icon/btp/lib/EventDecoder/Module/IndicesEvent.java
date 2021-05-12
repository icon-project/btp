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

public class IndicesEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return indexAssigned(input);
            case (byte)(0x01):
                return indexFreed(input);
            case (byte)(0x02):
                return indexFrozen(input);
        }
        return null;
    }

    public static byte[] indexAssigned(ByteSliceInput input) {
        // accountId = input.take(32);
        // accountIndex = ScaleReader.readU32(input);
        return input.take(36);
    }

    public static byte[] indexFreed(ByteSliceInput input) {
        // accountIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] indexFrozen(ByteSliceInput input) {
        // accountIndex = ScaleReader.readU32(input);
        // accountId = input.take(32);
        return input.take(36);
    }
}