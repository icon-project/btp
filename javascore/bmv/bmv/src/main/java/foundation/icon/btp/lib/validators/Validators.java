package foundation.icon.btp.lib.validators;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.blockupdate.BlockHeader;
import foundation.icon.btp.lib.mta.SerializableMTA;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import scorex.util.ArrayList;

public class Validators {
    private final List<byte[]> validators;

    public Validators(List<byte[]> validators) {
        this.validators = validators;
    }

    public List<byte[]> get() {
        return this.validators;
    }

    public static void writeObject(ObjectWriter w, Validators v) {
        List<byte[]> validators = v.get();
        w.beginList(validators.size());

        for(int i = 0; i < validators.size(); i++) {
            w.write(validators.get(i));
        }

        w.end();
    }

    public static Validators readObject(ObjectReader r) {
        r.beginList();
        List<byte[]> validators = new ArrayList<byte[]>(150);
        while (r.hasNext()) {
            byte[] v = r.readByteArray();
            validators.add(v);
        }
        r.end();

        return new Validators(validators);
    }
}