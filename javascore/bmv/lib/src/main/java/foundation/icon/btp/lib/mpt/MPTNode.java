package foundation.icon.btp.lib.mpt;

import foundation.icon.btp.lib.utils.ByteSliceInput;

import score.Context;

public class MPTNode {
    private Nibbles nibbles;
    private MPTNodeType type;
    private byte[] data;
    private byte[][] childrens;
    private byte[] hash;

    public MPTNode(byte[] serialized) {
        this.hash = Context.hash("blake2b-256", serialized);
        byte prefix = (byte) ((serialized[0] & 0xff) >> 6);
        if (prefix == 0b00) {
            this.type = MPTNodeType.EMPTY;
        } else { // leaf node
            this.type = MPTNodeType.LEAF;

            ByteSliceInput input = new ByteSliceInput(serialized);
            short nibblesCount = this.decodeNibbleSize(input);
            boolean padding = nibblesCount % 2 != 0;
            if (padding && (serialized[input.getOffset()] & 0b11110000) != 0) {
                throw new AssertionError("invalid mpt node format");
            }

            if (nibblesCount > 0) {
                short nibblesCountInByte = (short) ((nibblesCount + 1) / 2);
                this.nibbles = new Nibbles(input.take(nibblesCountInByte), padding);
            }

            if (prefix == 0b01) {
                this.data = this.readValue(input);
            } else {
                short bitmap = this.readBitMap(input);

                this.type = MPTNodeType.BRANCH;
                if (prefix == 0b11) { // branch node with value
                    this.data = this.readValue(input);
                    this.type = MPTNodeType.BRANCH_WITH_VALUE;
                }

                this.childrens = new byte[16][];
                for (int i = 0; i < 16; i++) {
                    if ((bitmap & 1<<i) != 0) {
                        this.childrens[i] = this.readValue(input);
                    }
                }
            }
        }
    }

    public Nibbles getNibbles() {
        return this.nibbles;
    }

    public byte[] getData() {
        return this.data;
    }

    public byte[][] getChildrens() {
        return this.childrens;
    }

    public MPTNodeType getType() {
        return this.type;
    }

    public byte[] getHash() {
        return this.hash;
    }

    private short decodeNibbleSize(ByteSliceInput input) {
        short size = (byte) (input.takeByte() & 0b00111111);
        if (size < 63) {
            return size;
        }

        while (size <= Short.MAX_VALUE) {
            byte n = input.take((short) 1)[0];
            if (n < 255) {
                return (short) (size + n);
            }
            size +=255;
        }

        return Short.MAX_VALUE;
    }

    private short readBitMap(ByteSliceInput input) {
        int firstByte = input.takeUByte();
        int secondByte = input.takeUByte();
        // int firstByteRevert = ((firstByte & 0xf0) >> 4) | ((firstByte & 0x0f) << 4);
        // int secondByteRevert = ((secondByte & 0xf0) >> 4) | ((secondByte & 0x0f) << 4);
        return (short) ((secondByte << 8) | firstByte);
    }

    private int readCompactSize(ByteSliceInput input) {
        int i = input.takeUByte();
        byte mode = (byte) (i & 0b00000011);
        if (mode == 0b00) {
            return i >> 2;
        }
        if (mode == 0b01) {
            return (i >> 2)
                    + (input.takeUByte() << 6);
        }
        if (mode == 0b10) {
            return (i >> 2) +
                    (input.takeUByte() << 6) +
                    (input.takeUByte() << (6 + 8)) +
                    (input.takeUByte() << (6 + 2 * 8));
        }
        throw new AssertionError("Mode " + mode  + " is not implemented");
    }

    private byte[] readValue(ByteSliceInput input) {
        int valueSize = this.readCompactSize(input);
        return input.take(valueSize);
    }
}
