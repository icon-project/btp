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

public class ContractsEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return instantiated(input);
            case (byte)(0x01):
                return evicted(input);
            case (byte)(0x02):
                return restored(input);
            case (byte)(0x03):
                return codeStored(input);
            case (byte)(0x04):
                return scheduleUpdated(input);
            case (byte)(0x05):
                return contractExecution(input);
        }
        return null;
    }

    public static byte[] instantiated(ByteSliceInput input) {
        // this.owner = input.take(32);
        // this.contract = input.take(32);
        return input.take(64);
    }

    public static byte[] evicted(ByteSliceInput input) {
        // this.contract = input.take(32);
        // this.tombstone = input.takeByte();
        return input.take(32 + 1);
    }

    public static byte[] restored(ByteSliceInput input) {
        // this.donor = input.take(32);
        // this.destination = input.take(32);
        // this.codeHash = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 32 + 32 + 16);
    }

    public static byte[] codeStored(ByteSliceInput input) {
        // this.codeHash = input.take(32);
        return input.take(32);
    }

    public static byte[] scheduleUpdated(ByteSliceInput input) {
        // this.schedule = ScaleReader.readU32(input);;
        return input.take(4);
    }

    public static byte[] contractExecution(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte[] accountId = input.take(32);
        int dataSize = ScaleReader.readUintCompactSize(input);
        byte[] codeData = input.take(dataSize);
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }
}