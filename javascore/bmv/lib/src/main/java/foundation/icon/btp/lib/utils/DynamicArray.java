package foundation.icon.btp.lib.utils;

import java.util.Arrays;

import score.Context;

public class DynamicArray {
    private byte[][] arr;
    private int length;
    private int allocatedSize;

    public DynamicArray(int initialSize) {
        arr = new byte[initialSize][];
        allocatedSize = initialSize;
        length = 0;
    }

    public int length() {
        return this.length;
    }

    public byte[][] toArray() {
        return this.arr;
    }

    public int allocatedSize() {
        return this.allocatedSize;
    }

    public void add(byte[] hash) {
        arr[length] = hash;

        if (length == this.allocatedSize) {
            this.setSize(this.allocatedSize*2);
        }
        length++;
    }

    public void addAndTruncate(byte[] hash) {
        if (this.length == this.allocatedSize) {
            byte[][] newArr = new byte[this.allocatedSize][];
            copy(this.arr, newArr, 1, this.length - 1);
            this.arr = newArr;
            this.length--;
        }
        this.arr[length] = hash;
        this.length++;
    }

    public void set(int idx, byte[] hash) {
        if (0 <= idx && idx < this.length) {
            this.arr[idx] = hash;
        } else {
            throw new AssertionError("idx is out of range, use add instead");
        }
    }

    public void addMany(byte[][] hashes) {
        for(byte[] hash : hashes)
            this.add(hash);
    }

    public byte[] get(int idx) {
        if (0 <= idx && idx < this.length) {
            return this.arr[idx];
        } else {
            throw new AssertionError("root idx is out of range");
        }
    }

    public byte[] getNullable(int idx) {
        if (0 <= idx && idx < this.length) {
            return this.arr[idx];
        } else {
            return null;
        }
    }

    public void truncate(int size) {
        if (size > this.length) {
            throw new AssertionError("size greater than array length");
        } else {
            byte[][] newArr = new byte[size][];
            copy(this.arr, newArr, size - this.length, size);
            this.arr = newArr;
        }
        this.allocatedSize = size;
        this.length = size;
    }

    public void setSize(int size) {
        if (size > this.length) {
            byte[][] newArr = new byte[size][];
            copy(this.arr, newArr, 0, this.length);
            this.arr = newArr;
        } else {
            shrink(size, this.arr);
            this.length = size;
        }
        this.allocatedSize = size;
    }

    public boolean contains(byte[] hash) {
        if (hash == null || hash.length == 0)
            return false;
        for (int idx = 0; idx < this.length; idx++)
            if (Arrays.equals(hash, this.arr[idx]))
                return true;
        return false;
    }

    private void copy(byte[][] src, byte[][] dst, int from, int to) {
        for(int i = from; i < to; i++) {
            dst[i] = src[i];
        }
    }

    private void shrink(int size, byte[][] arr) {
        for(int i = size; i < this.length; i++) {
            this.arr[i] = null;
        }
    }
}
