package foundation.icon.btp.lib.mpt;

import foundation.icon.btp.lib.utils.Arrays;

import score.Context;

public class Nibbles {
    private byte[] raw;
    private final boolean hasPadding;

    public Nibbles(byte[] raw, boolean hasPadding) {
        this.raw = raw;
        this.hasPadding = hasPadding;
    }

    public int size() {
        int baseLength = this.raw.length*2;
        return hasPadding ? baseLength - 1 : baseLength;
    }

    public byte[] getRaw() {
        return this.raw;
    }

    public byte getNibble(int idx) {
        if ((idx % 2) == 0) {
            if (this.hasPadding) {
                return (byte) (this.raw[idx/2] & 0x0f);
            } else {
                return (byte) ((this.raw[idx/2] & 0xf0) >> 4);
            }
        } else {
            if (this.hasPadding) {
                return (byte) ((this.raw[idx/2 + 1] & 0xf0) >> 4);
            } else {
                return (byte) (this.raw[idx/2] & 0x0f);
            }
        }
    }

    public int match(Nibbles provingNibbles) {
        if (this.size() == 0 || provingNibbles.size() == 0) {
            return 0;
        }

        if (provingNibbles.size() < this.size()) {
            throw new AssertionError("proving nibble length is less than node nibble length");
        }

        for (int i = 0; i < this.size(); i++) {
            if (this.getNibble(i) != provingNibbles.getNibble(i)) {
                return i;
            }
        }
        return this.size();
    }

    public Nibbles createChildNibbles(int from) {
        byte[] childRaw;
        boolean hasPadding = false;
        if ((from % 2) == 0) {
            if (this.hasPadding) {
                childRaw = Arrays.copyOfRangeByte(this.raw, from/2, this.raw.length);
                System.arraycopy(this.raw, from/2, childRaw, 0,
                         this.raw.length - from/2);
                childRaw[0] = (byte) (this.raw[from/2] & 0x0f);
                hasPadding = true;
            } else {
                childRaw = Arrays.copyOfRangeByte(this.raw, from/2, this.raw.length);
            }
        } else {
            if (this.hasPadding) {
                childRaw = Arrays.copyOfRangeByte(this.raw, from/2 + 1, this.raw.length);
            } else {
                childRaw = Arrays.copyOfRangeByte(this.raw, from/2, this.raw.length);
                childRaw[0] = (byte) (this.raw[from/2] & 0x0f);
                hasPadding = true;
            }
        }
        return new Nibbles(childRaw, hasPadding);
    }
}