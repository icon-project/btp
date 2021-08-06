package foundation.icon.btp.bmv.lib;

import java.util.Arrays;

public class ExtraDataTypeDecoder {

    private static byte[] data;

    public ExtraDataTypeDecoder(byte[] data) {
        this.data = data;
    }


    public static byte[] getBytes(int offset, int len) {
        byte[] bytes = slice(offset, len);
        return bytes;
    }

    /*
    public static byte[] getBytes() {
        byte[] bytes = slice(data.length - offset);
        return bytes;
    }

*/
    public static byte[] slice(int offset, int len) {
        byte[] result = Arrays.copyOfRange(data, offset, len + offset);
        return result;
    }
}
