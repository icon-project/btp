package foundation.icon.btp.bmv.near.verifier.types;

import score.ObjectReader;
import score.Context;
import foundation.icon.btp.bmv.near.verifier.utils.Hasher;
import foundation.icon.score.util.StringUtil;
import io.ipfs.multibase.Base58;

public class BlockHeader {
    private byte[] previousBlockHash;
    private HeaderInnerLite innerLite;
    private HeaderInnerRest innerRest;
    private byte[] signature;

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public static BlockHeader readObject(ObjectReader reader) {
        BlockHeader blockHeader = new BlockHeader();
        reader.beginList();
        blockHeader.previousBlockHash = reader.readByteArray();
        blockHeader.innerLite = HeaderInnerLite.readObject(reader);
        blockHeader.innerRest = HeaderInnerRest.readObject(reader);
        blockHeader.signature = reader.readByteArray();
        reader.end();
        return blockHeader;
    }

    public byte[] hash(byte[] previousBlockHash) {
        Hasher<SHA256> hasher = new Hasher<SHA256>(new SHA256());
        byte[] innerHash = hasher.combineHash(innerLite.hash(), innerRest.hash());
        return hasher.combineHash(innerHash, previousBlockHash);
    }

    public byte[] hash() {
        Hasher<SHA256> hasher = new Hasher<SHA256>(new SHA256());
        byte[] innerHash = hasher.combineHash(innerLite.hash(), innerRest.hash());
        return hasher.combineHash(innerHash, previousBlockHash);
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);

        return BlockHeader.readObject(reader);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockHeader{");
        sb.append("previousBlockHash=").append(Base58.encode(previousBlockHash));
        sb.append(", innerLite=").append(StringUtil.toString(innerLite));
        sb.append(", innerRest=").append(StringUtil.toString(innerRest));
        sb.append(", signature=").append(StringUtil.bytesToHex(signature));
        sb.append('}');
        return sb.toString();
    }
}
