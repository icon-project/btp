package foundation.icon.btp.bmv.lib.mpt;

import foundation.icon.btp.bmv.lib.ArraysUtil;

import java.util.List;

public class Nibbles {

    public boolean isNibble(byte nibble) {
        // 0-9 && a-f
        return (int) nibble >= 0 && (int) nibble < 16;
    }

    public static byte[] bytesToNibbles(byte[] original) {
        int length = original.length;
        byte[] expand = new byte[length * 2];
        int pos = 0;
        for (byte b : original) {
            expand[pos++] = (byte) ((b & 0xf0) >> 4);
            expand[pos++] = (byte) (b & 0x0f);
        }
        return expand;
    }

    public static byte[] nibblesToBytes(byte[] nibbles) {
        byte[] compact = new byte[nibbles.length / 2];
        for (int i = 0; i < compact.length; i++){
            int pos = i * 2;
            compact[i] = (byte) ((nibbles[pos] << 4) + nibbles[++pos]);
        }

        return compact;
    }

    public static int matchingNibbleLength(List<Byte> a, List<Byte> b) {
        int len = Integer.min(a.size(), b.size());
        for (int i = 0; i < len; i++) {
            if (!a.get(i).equals(b.get(i)))
                return i;
        }
        return len;
    }

    public static int matchingNibbleLength(byte[] a, byte[] b) {
        int len = Integer.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i])
                return i;
        }
        return len;
    }

    public static boolean match(byte[] a, byte[] b) {
        int len = matchingNibbleLength(a, b);
        return (len == a.length && len == b.length);
    }

    public static boolean isTerminator(byte[] key) {
        return key[0] > 1;
    }

    public static byte[] addHexPrefix(byte[] keyNibbles, boolean terminator) {
        byte[] key;

        if(keyNibbles.length % 2 != 0){
            key = new byte[keyNibbles.length + 1];
            System.arraycopy(keyNibbles, 0, key, 1, keyNibbles.length);
            key[0] = 1;
        } else {
            key = new byte[keyNibbles.length + 2];
            System.arraycopy(keyNibbles, 0, key, 2, keyNibbles.length);
            key[0] = 0;
            key[1] = 0;
        }

        if (terminator) {
            key[0] += 2;
        }

        return key;
    }

    public static byte[] removeHexPrefix(byte[] keyNibbles) {
        if (keyNibbles[0] % 2 != 0)
            return ArraysUtil.copyOfRangeByte(keyNibbles, 1, keyNibbles.length);
        else
            return ArraysUtil.copyOfRangeByte(keyNibbles,2, keyNibbles.length);
    }
}
