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

public class ProxyEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return proxyExecuted(input);
            case (byte)(0x01):
                return anonymousCreated(input);
            case (byte)(0x02):
                return announced(input);
        }
        return null;
    }

    public static byte[] proxyExecuted(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte dispatchResultEnum = input.takeByte();
        if (dispatchResultEnum == (byte) (0x01 & 0xff)) {
            byte dispatchErrorEnum = input.takeByte();
            if (dispatchErrorEnum == (byte) (0x03 & 0xff)) {
                byte[] dispatchError = input.take(2);
            }
        }
        int endpoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endpoint - startPoint);
    }

    public static byte[] anonymousCreated(ByteSliceInput input) {
        // this.anonymous = input.take(32);
        // this.accountId = input.take(32);
        // this.proxyType = input.takeByte();
        // this.disambiguationIndex = ScaleReader.readU16(input);
        return input.take(32 + 32 + 1 + 2);
    }

    public static byte[] announced(ByteSliceInput input) {
        // this.realAccount = input.take(32);
        // this.proxyAccount = input.take(32);
        // this.callHash = input.take(32);
        return input.take(32 + 32 + 32);
    }
}