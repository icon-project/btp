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

public class SystemEvent {

    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return extrinsicSuccess(input);
            case (byte)(0x01):
                return extrinsicFailed(input);
            case (byte)(0x02):
                return codeUpdated(input);
            case (byte)(0x03):
                return newAccount(input);
            case (byte)(0x04):
                return killedAccount(input);
        }
        return null;
    }

    public static byte[] extrinsicSuccess(ByteSliceInput input) {
        return input.take(10);

    }

    public static byte[] extrinsicFailed(ByteSliceInput input) {
        int size = 11;
        int startPoint = input.getOffset();
        byte dispatchError = input.takeByte();
        if ((dispatchError & 0xff) == 0x03) {
            size += 2;
        }
        input.seek(startPoint);
        return input.take(size);
    }

    public static byte[] codeUpdated(ByteSliceInput input) {
        return null;
    }

    public static byte[] newAccount(ByteSliceInput input) {
        return input.take(32);
    }

    public static byte[] killedAccount(ByteSliceInput input) {
        return input.take(32);
    }
}