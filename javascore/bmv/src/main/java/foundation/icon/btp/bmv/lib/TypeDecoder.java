package foundation.icon.btp.bmv.lib;

import java.math.BigInteger;
import java.util.Arrays;

public class TypeDecoder {
    private static byte[] data;
    private static int offset;

    public TypeDecoder(byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    public static BigInteger getUint() {
        byte[] data = slice(32);
        BigInteger uint = BigInteger.ZERO;
        int count = 0;
        while (count < 32) {
            if (data[count] != 0) {
                uint = uint.add(BigInteger.valueOf(data[count] & 0xff).shiftLeft(8 * (31 - count)));
            }
            count++;
        }
        return uint;
    }

    public static byte[] getBytes() {
        int serLen = (getUint().intValue());
        int totalLen = (serLen / 32 + 1) * 32;
        byte[] bytes = slice(totalLen);
        return bytes;
    }

    public static byte[] slice(int len) {
        byte[] result = Arrays.copyOfRange(data, offset, len + offset);
        offset += len;
        return result;
    }

    public static byte[] getData() {
        return data;
    }

    public static int getOffset() {
        return offset;
    }
}
