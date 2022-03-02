package foundation.icon.btp.lib.validators;

import java.util.List;
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