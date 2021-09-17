package foundation.icon.btp.bmv.lib;

public class ArraysUtil {

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

    public static byte[] concat(byte[] left, byte[] right) {
        byte[] c = new byte[left.length + right.length];
        System.arraycopy(left, 0, c, 0, left.length);
        System.arraycopy(right, 0, c, left.length, right.length);
        return c;
    }

}