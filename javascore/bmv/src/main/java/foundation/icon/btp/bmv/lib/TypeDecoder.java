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

    public static String getString() {
        int serLen = getUint().intValue();
        int totalLen = (serLen / 32 + 1) * 32;
        String str = new String(slice(serLen));
        slice(totalLen - serLen);
        return str;
    }

    public static byte[] getBytes() {
        int serLen = (getUint().intValue());
        int totalLen = (serLen / 32 + 1) * 32;
        offset = offset + 32;//skiping another 32 bytes : TODO: check what the number is for
        byte[] bytes = slice(data.length - offset);
        return bytes;
    }


    public static byte[] slice(int len) {
        byte[] result = Arrays.copyOfRange(data, offset, len + offset);
        offset += len;
        return result;
    }

}
