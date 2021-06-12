package foundation.icon.btp.lib.scale;

import score.Context;

import java.math.BigInteger;
import java.util.Arrays;

import foundation.icon.btp.lib.utils.ByteSliceInput;

public class ScaleReader {
    public static int readUintCompactSize(ByteSliceInput input) {
        int i = input.takeUByte();
        byte mode = (byte) (i & 0b00000011);
        if (mode == 0b00) {
            return i >> 2;
        }
        if (mode == 0b01) {
            return (i >> 2)
                    + (input.takeUByte() << 6);
        }
        if (mode == 0b10) {
            return (i >> 2) +
                    (input.takeUByte() << 6) +
                    (input.takeUByte() << (6 + 8)) +
                    (input.takeUByte() << (6 + 2 * 8));
        }
        throw new AssertionError("Mode " + mode  + " is not implemented");
    }

    public static byte[] readBytes(ByteSliceInput input) {
        int dataSize = ScaleReader.readUintCompactSize(input);
        return input.take(dataSize);
    }

    public static BigInteger readCompacBigInteger(ByteSliceInput input) {
        int type = input.takeUByte();
        if ((type & 0b11) != 0b11) {
            input.skip(-1);
            int value = ScaleReader.readUintCompactSize(input);
            return BigInteger.valueOf(value);
        }
        int len = (type >> 2) + 4;
        byte[] value = input.take(len);
        //LE encoded, so need to reverse it
        for (int i = 0; i < value.length / 2; i++) {
            byte temp = value[i];
            value[i] = value[value.length -i -1];
            value[value.length - i - 1] = temp;
        }
        //unsigned, i.e. always positive, signum=1
        return new BigInteger(1, value);
    }

    public static int readU8(ByteSliceInput input) {
        int result = 0;
        result += (int)input.takeUByte();
        return result;
    }

    public static int readU16(ByteSliceInput input) {
        int result = 0;
        result += (int)input.takeUByte();
        result += ((int)input.takeUByte()) << 8;
        return result;
    }

    public static long readU32(ByteSliceInput input) {
        long result = 0;
        result += (long)input.takeUByte();
        result += ((long)input.takeUByte()) << 8;
        result += ((long)input.takeUByte()) << (2 * 8);
        result += ((long)input.takeUByte()) << (3 * 8);
        return result;
    }

    public static BigInteger readU64(ByteSliceInput input) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i <= 7; i++) {
            result = result.add(BigInteger.valueOf(input.takeUByte()).shiftLeft(8*i));
        }
        return result;
    }

    public static BigInteger readU128(ByteSliceInput input) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i <= 15; i++) {
            result = result.add(BigInteger.valueOf(input.takeUByte()).shiftLeft(8*i));
        }
        return result;
    }

    public static BigInteger readU256(ByteSliceInput input) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i <= 31; i++) {
            result = result.add(BigInteger.valueOf(input.takeUByte()).shiftLeft(8*i));
        }
        return result;
    }
}