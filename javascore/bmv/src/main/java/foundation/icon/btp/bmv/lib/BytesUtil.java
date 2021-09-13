package foundation.icon.btp.bmv.lib;

public class BytesUtil {
    public static final byte[] EMPTY_BYTES = new byte[]{};

    public static boolean isEmptyOrNull(byte[] bytes) {
        return (bytes == null || bytes == EMPTY_BYTES);
    }
}