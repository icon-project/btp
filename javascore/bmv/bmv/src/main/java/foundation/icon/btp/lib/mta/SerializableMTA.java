package foundation.icon.btp.lib.mta;

import java.util.List;

import score.ObjectReader;
import score.ObjectWriter;
import score.Context;
import scorex.util.ArrayList;

public class SerializableMTA extends MerkleTreeAccumulator {
    public SerializableMTA(
        long height,
        long offset,
        int rootSize,
        int cacheSize,
        boolean isAllowNewerWitness,
        byte[] lastBlockHash,
        List<byte[]> roots,
        List<byte[]> caches
    ) {
        super(height, offset, rootSize, cacheSize, isAllowNewerWitness, lastBlockHash, roots, caches);
    }

    public static void writeObject(ObjectWriter w, SerializableMTA t) {
        w.beginList(6);
        w.write(t.height());
        w.write(t.offset());
        w.write(t.rootSize());
        w.write(t.cacheSize());
        w.write(t.isAllowNewerWitness());
        w.write(t.lastBlockHash());

        w.beginList(t.rootSize());
        for (int idx = 0; idx < t.rootSize(); idx++) {
            w.writeNullable(t.getNullableRoot(idx));
        }
        w.end();

        w.beginList(t.cacheSize());
        for (int idx = 0; idx < t.cacheSize(); idx++) {
            w.writeNullable(t.getNullableCache(idx));
        }
        w.end();

        w.end();
    }

    public static SerializableMTA readObject(ObjectReader r) {
        r.beginList();
        long height = r.readLong();
        long offset = r.readLong();
        int rootSize = r.readInt();
        int cacheSize = r.readInt();
        boolean isAllowNewerWitness = r.readBoolean();
        byte[] lastBlockHash = r.readByteArray();

        List<byte[]> roots = new ArrayList<byte[]>(rootSize);
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] root = r.readNullable(byte[].class);
                roots.add(root);
            }
            r.end();
        }

        List<byte[]> caches = new ArrayList<byte[]>(cacheSize);
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] cache = r.readNullable(byte[].class);
                if (cache != null && cache.length > 0)
                    caches.add(cache);
            }
            r.end();
        }

        r.end();

        return new SerializableMTA(
            height,
            offset,
            rootSize,
            cacheSize,
            isAllowNewerWitness,
            lastBlockHash,
            roots,
            caches
        );
    }
}
