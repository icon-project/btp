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
        int paddingLength = 0;
        if (msgLength%32 != 0) {
            paddingLength = 32 - msgLength%32;
        } 

        String result = new String(input.take(msgLength));
        input.take(paddingLength); // remove padding
        return result;
    }

    public static byte[] decodeBytes(ByteSliceInput input) {
        int byteLength = AbiDecoder.decodeUInt(input).intValue();
        int paddingLength = 0;
        if (byteLength%32 != 0) {
            paddingLength = 32 - byteLength%32;
        } 

        byte[] result = input.take(byteLength);
        input.take(paddingLength); // remove padding
        return result;
    }
}