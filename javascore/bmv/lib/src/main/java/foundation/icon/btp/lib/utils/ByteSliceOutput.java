package foundation.icon.btp.lib.utils;

import java.util.Arrays;

import score.Context;

public class ByteSliceOutput {
    private byte[] arr;
    private int size;
    private int allocationSize;

    public ByteSliceOutput(int initialSize) {
        this.arr = new byte[initialSize];
        this.allocationSize = initialSize;
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public byte[] toArray() {
        if (this.size < this.allocationSize) {
            byte[] original = new byte[this.size];
            this.copy(this.arr, original, 0, size);
            return original;
        } else {
            return this.arr;
        }
    }

    public void add(byte b) {
        if (this.size == this.allocationSize) {
            this.setSize(this.allocationSize*2);
        }

        this.arr[size] = b;
        this.size++;
    }

    public void add(int b) {
        this.add( (byte) b);
    }

    public void set(int idx, byte b) {
        if (0 <= idx && idx < this.size) {
            this.arr[idx] = b;
        } else {
            throw new AssertionError("idx is out of range, use add instead");
        }
    }

    public void addMany(byte[] bs) {
        for(byte b : bs)
            this.add(b);
    }

    public byte get(int idx) {
        if (0 <= idx && idx < this.size) {
            return this.arr[idx];
        } else {
            throw new AssertionError("root idx is out of range");
        }
    }

    public void setSize(int size) {
        int copySize = size > this.size ? this.size : size;
        byte[] newArr = new byte[size];
        copy(this.arr, newArr, 0, copySize);
        this.arr = newArr;
        this.allocationSize = size;
    }

    private void copy(byte[] src, byte[] dst, int from, int to) {
        for(int i = from; i < to; i++) {
            dst[i] = src[i];
        }
    }
}
