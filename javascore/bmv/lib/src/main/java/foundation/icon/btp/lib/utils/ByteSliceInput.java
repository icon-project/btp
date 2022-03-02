package foundation.icon.btp.lib.utils;

import foundation.icon.btp.lib.utils.Arrays;

import java.util.List;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class ByteSliceInput {
    private byte[] input;
    private int offset;

    public ByteSliceInput(byte[] input) {
        this.input = input;
        this.offset = 0;
    }

    public int getOffset() {
        return this.offset;
    }

    public byte[] take(int size) {
        if (this.offset + size > this.input.length) {
			throw new AssertionError("byte input out of range");
		}

        byte[] data = Arrays.copyOfRangeByte(this.input, this.offset, this.offset + size);
        this.offset += size;
        return data;
    }

    public byte takeByte() {
        if (this.offset + 1 > this.input.length) {
			throw new AssertionError("byte input out of range");
		}

        byte b = this.input[this.offset];
        this.offset ++;
        return b;
    }

    /*
     * return int with the same bit pattern byte
    */
    public int takeUByte() {
        if (this.offset + 1 > this.input.length) {
			throw new AssertionError("byte input out of range");
		}

        byte b = this.input[this.offset];
        this.offset ++;
        if (b < 0) {
            return 256 + (int)b;
        }
        return (int)b;
    }

    public byte[] remain() {
        if (this.offset + 1 > this.input.length) {
			throw new AssertionError("byte input out of range");
		}
        this.offset = this.input.length;
        return Arrays.copyOfRangeByte(input, this.offset, this.input.length);
    }

    public void seek(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative: " + offset);
        } else if (offset >= this.input.length) {
            throw new IllegalArgumentException("Offset " + offset + " must be strictly smaller than source length: " + this.input.length);
        }

        this.offset = offset;
    }

    public void skip(int len) {
        if (len < 0 && Math.abs(len) > this.offset) {
            throw new IllegalArgumentException("Position cannot be negative: " + this.offset + " " + len);
        }

        if (this.offset + len >= this.input.length) {
            throw new IllegalArgumentException("Offset " + offset + " must be strictly smaller than source length: " + this.input.length);
        }

        this.offset += len;
    }
}
