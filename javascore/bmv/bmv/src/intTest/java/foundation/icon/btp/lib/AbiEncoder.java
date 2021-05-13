package foundation.icon.test.cases;

import java.math.BigInteger;

import score.Context;

public class AbiEncoder {
    public static void encodeUInt(ByteSliceOutput output, long number) {
        output.addMany(HexConverter.hexStringToByteArray("000000000000000000000000000000000000000000000000")); // 24 byte padding
        for (int i = 7; i>=0; i--) {
            output.add((byte) ((number >> (8*i)) & 0xff));
        }
    }

    public static void encodeBytes(ByteSliceOutput output, byte[] data) {
        int padding = (32 - data.length%32);
        encodeUInt(output, data.length);
        output.addMany(data);
        for (int i = 0; i<padding;i++) {
            output.add(0);
        }
    }
}