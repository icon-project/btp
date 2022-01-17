package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.Borsh;
import org.near.borshj.BorshBuffer;
import score.ObjectReader;
import score.Context;
import foundation.icon.btp.bmv.near.verifier.utils.Hasher;
import foundation.icon.score.util.StringUtil;

public class HeaderInnerLite implements Borsh {
    private long height;
    private byte[] epochId;
    private byte[] nextEpochId;
    private byte[] previousStateRoot;
    private byte[] outcomeRoot;
    private long timestamp;
    private byte[] nextBlockProducerHash;
    private byte[] blockMerkleRoot;

    public long getHeight() {
        return height;
    }

    public byte[] nextBlockProducerHash() {
        return nextBlockProducerHash;
    }

    @Override
    public void append(BorshBuffer buffer) {
        buffer.write(height);
        buffer.write(epochId);
        buffer.write(nextEpochId);
        buffer.write(previousStateRoot);
        buffer.write(outcomeRoot);
        buffer.write(timestamp);
        buffer.write(nextBlockProducerHash);
        buffer.write(blockMerkleRoot);
    }

    public static HeaderInnerLite readObject(ObjectReader reader) {
        HeaderInnerLite headerInnerLite = new HeaderInnerLite();
        reader.beginList();
        headerInnerLite.height = reader.readLong();
        headerInnerLite.epochId = reader.readByteArray();
        headerInnerLite.nextEpochId = reader.readByteArray();
        headerInnerLite.previousStateRoot = reader.readByteArray();
        headerInnerLite.outcomeRoot = reader.readByteArray();
        headerInnerLite.timestamp = reader.readLong();
        headerInnerLite.nextBlockProducerHash = reader.readByteArray();
        headerInnerLite.blockMerkleRoot = reader.readByteArray();
        reader.end();
        return headerInnerLite;
    }

    public static HeaderInnerLite fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);

        return HeaderInnerLite.readObject(reader);
    }

    public byte[] borshSerialize() {
        byte[] bytes = Borsh.serialize(this);
        return bytes;
    }

    public byte[] hash() {
        Hasher<SHA256> hasher = new Hasher<SHA256>(new SHA256());
        return hasher.computeHash(borshSerialize());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HeaderInnerLite{");
        sb.append("height=").append(height);
        sb.append(", epochId=").append(StringUtil.bytesToHex(epochId));
        sb.append(", nextEpochId=").append(StringUtil.bytesToHex(nextEpochId));
        sb.append(", previousStateRoot=").append(StringUtil.bytesToHex(previousStateRoot));
        sb.append(", outcomeRoot=").append(StringUtil.bytesToHex(outcomeRoot));
        sb.append(", timestamp=").append(timestamp);
        sb.append(", nextBlockProducerHash=").append(StringUtil.bytesToHex(nextBlockProducerHash));
        sb.append(", logsBloom=").append(StringUtil.bytesToHex(blockMerkleRoot));
        sb.append('}');
        return sb.toString();
    }
}
