package foundation.icon.btp.lib.utils;

import java.math.BigInteger;

import score.Context;

public class AbiDecoder {
    public static BigInteger decodeUInt(ByteSliceInput input) {
        byte[] b = input.take(32);
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < 32; i++) {
            if (b[i] != 0) {
                result = result.add(BigInteger.valueOf(b[i] & 0xff).shiftLeft(8*(31-i)));
            }
        }
        return result;
    }

    public static String decodeString(ByteSliceInput input) {
        int msgLength = AbiDecoder.decodeUInt(input).intValue();
        int totalLength = (msgLength/32 + 1)*32;

        String result = new String(input.take(msgLength));
        input.take(totalLength - msgLength); // remove padding
        return result;
    }

    public static byte[] decodeBytes(ByteSliceInput input) {
        int byteLength = AbiDecoder.decodeUInt(input).intValue();
        int totalLength = (byteLength/32 + 1)*32;

        byte[] result = input.take(byteLength);
        input.take(totalLength - byteLength); // remove padding
        return result;
    }
}