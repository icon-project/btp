package foundation.icon.btp.lib.scale;

import score.Context;

import java.math.BigInteger;
import java.util.Arrays;

import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.ByteSliceOutput;

public class ScaleWriter {
    public static void writeCompactUint(ByteSliceOutput output, int compactNumber) {
        int compactSize = ScaleWriter.compactUintSize(compactNumber);
        int compact = (compactNumber << 2) + compactSize - 1;
        while (compactSize > 0) {
            byte b = (byte) (compact & 0xff);
            output.add(b);
            compact >>= 8;
            compactSize--;
        }
    }

    public static int compactUintSize(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Negative numbers are not supported");
        }
        if (number <= 0x3f) {
            return 1;
        } else if (number <= 0x3fff) {
            return 2;
        } else if (number <= 0x3fffffff) {
            return 3;
        } else {
            return 4;
        }
    }
}