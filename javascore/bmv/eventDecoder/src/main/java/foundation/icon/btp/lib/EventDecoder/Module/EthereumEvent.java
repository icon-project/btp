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

public class EthereumEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return executed(input);
        }
        return null;
    }

    private static byte exitErrorDecode(ByteSliceInput input) {
        byte error = input.takeByte();
        if ((error & 0xff) == 0x0d) {
            int textSize = ScaleReader.readUintCompactSize(input);
            input.take(textSize);
        }
        return error;
    }

    public static byte[] executed(ByteSliceInput input) {
        // this.from = input.take(20);
        // this.to = input.take(20);
        // this.transactionHash = input.take(32);

        int startPoint = input.getOffset();
        input.seek(startPoint + 20 + 20 + 32);
        byte exitReasonEnum = input.takeByte();
        if ((exitReasonEnum & 0xff) == 0x00) {
            byte success = input.takeByte();
        } else if ((exitReasonEnum & 0xff) == 0x01) {
            exitErrorDecode(input);
        } else if ((exitReasonEnum & 0xff) == 0x02) {
            byte revert = input.takeByte();
        } else if ((exitReasonEnum & 0xff) == 0x03) {
            byte fatal = input.takeByte();
            if ((fatal & 0xff) == 0x02) {
                exitErrorDecode(input);
            }
        }
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }
}