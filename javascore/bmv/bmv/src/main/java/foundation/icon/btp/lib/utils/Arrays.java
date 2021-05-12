package foundation.icon.btp.lib.utils;

import java.util.List;
import score.Context;

public class Arrays {
    private Arrays() {}

    public static byte[] copyOfRangeByte(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    public static byte[][] copyOfRangeByteArray(byte[][] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        byte[][] copy = new byte[newLength][];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    public static boolean arrayListContains(byte[] data, List<byte[]> source) {
        for (byte[] element : source) {
            if (java.util.Arrays.equals(data, element)) {
                return true;
            }
        }
        return false;
    }

    
}