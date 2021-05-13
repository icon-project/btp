package foundation.icon.test.cases;

import score.Context;

import java.math.BigInteger;
import java.util.Arrays;

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

    public static void writeU32(ByteSliceOutput output, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative values are not supported: " + value);
        }
        output.add( (byte) (value & 0xff));
        output.add( (byte) ((value >> 8) & 0xff));
        output.add( (byte) ((value >> 16) & 0xff));
        output.add( (byte) ((value >> 24) & 0xff));
    }

    public static void writeU64(ByteSliceOutput output, BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Negative values are not supported: " + value);
        }
        output.add(value.and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(8).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(16).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(24).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(32).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(40).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(48).and(BigInteger.valueOf(255)).intValue());
        output.add(value.shiftRight(56).and(BigInteger.valueOf(255)).intValue());
    }
}