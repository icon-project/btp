package foundation.icon.btp.bmv.lib.mpt;

import scorex.util.ArrayList;

import java.util.List;

public class Value {
    private List<byte[][]> val;

    public Value(List<byte[][]> v) {
        this.val = v;
    }

    public Value(byte[][] v) {
        this.val = new ArrayList<>();
        this.set(v);
    }

    public Value() {
        this.val = new ArrayList<>();
    }

    public void set(byte[][] v) {
        this.val.add(v);
    }

    public List<byte[][]> get() {
        return this.val;
    }

    public byte[][] get(int i) {
        return val.get(i);
    }

    public byte[][] first() {
        if (isEmpty())
            return null;
        return val.get(0);
    }

    public boolean isEmpty() {
        return this.val.isEmpty();
    }

}
