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

public class SchedulerEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return scheduled(input);
            case (byte)(0x01):
                return canceled(input);
            case (byte)(0x02):
                return dispatched(input);
        }
        return null;
    }

    public static byte[] scheduled(ByteSliceInput input) {
        // this.blockNumber = ScaleReader.readU32(input);
        // this.taskIndex = ScaleReader.readU32(input);
        return input.take(4 + 4);
    }

    public static byte[] canceled(ByteSliceInput input) {
        // this.blockNumber = ScaleReader.readU32(input);
        // this.taskIndex = ScaleReader.readU32(input);
        return input.take(4 + 4);
    }

    public static byte[] dispatched(ByteSliceInput input) {
        int startPoint = input.getOffset();
        long taskAddressBlockNumber = ScaleReader.readU32(input);
        long taskAddressIndex = ScaleReader.readU32(input);

        int isHasId = input.takeUByte();
        if (isHasId > 0) {
            byte[] id = ScaleReader.readBytes(input);
        }

        byte dispatchResultEnum = input.takeByte();
        if (dispatchResultEnum == (byte) (0x01 & 0xff)) {
            byte dispatchErrorEnum = input.takeByte();
            if (dispatchErrorEnum == (byte) (0x03 & 0xff)) {
                byte[] dispatchError = input.take(2);
            }
        }
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }
}