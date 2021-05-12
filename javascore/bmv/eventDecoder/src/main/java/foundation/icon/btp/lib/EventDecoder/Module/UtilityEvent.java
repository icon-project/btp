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

public class UtilityEvent {
    public static byte[] batchInterrupted(ByteSliceInput input) {
        int size = 5;
        long errorIndex = ScaleReader.readU32(input);
        byte dispatchError = input.takeByte();
        if ((dispatchError & 0xff) == 0x03) {
            // byte[] module = input.take(2);
            size += 2;
        }
        input.seek(input.getOffset() - size);
        return input.take(size);
    }

    public static byte[] batchCompleted(ByteSliceInput input) {
        return null;
    }

    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return batchInterrupted(input);
            case (byte)(0x01):
                return batchCompleted(input);
        }
        return null;
    }
}