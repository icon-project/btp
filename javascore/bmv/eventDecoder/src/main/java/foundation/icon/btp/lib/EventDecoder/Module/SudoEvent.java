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

public class SudoEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return sudid(input);
            case (byte)(0x01):
                return keyChanged(input);
            case (byte)(0x02):
                return sudoAsDone(input);
        }
        return null;
    }

    public static byte[] sudid(ByteSliceInput input) {
        int startPoint = input.getOffset();
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

    public static byte[] keyChanged(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] sudoAsDone(ByteSliceInput input) {
        int startPoint = input.getOffset();
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