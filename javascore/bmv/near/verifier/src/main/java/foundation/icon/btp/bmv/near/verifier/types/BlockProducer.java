package foundation.icon.btp.bmv.near.verifier.types;

import java.math.BigInteger;
import org.near.borshj.BorshBuffer;
import io.ipfs.multibase.Base58;
import score.ObjectReader;

public class BlockProducer extends Item {
    private byte[] validatorStakeStructVersion;
    private String accountId;
    private PublicKey publicKey;
    private BigInteger stake;

    public BlockProducer() {
    }

    protected BlockProducer(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public BlockProducer(String str) {
        this.publicKey = new PublicKey(str);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockProducer{");
        sb.append("publicKey=").append(Base58.encode(publicKey.getKey()));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Item fromString(String str) {
        return new BlockProducer(str);
    }

    @Override
    public void append(BorshBuffer writer) {
        writer.write(validatorStakeStructVersion);
        writer.write(accountId);
        writer.write(publicKey);
        writer.write(stake);
    }

    public static BlockProducer readObject(ObjectReader reader) {
        BlockProducer blockProducer = new BlockProducer();
        reader.beginList();
        blockProducer.validatorStakeStructVersion = reader.readByteArray();
        blockProducer.accountId = reader.readString();
        reader.skip();
        // blockProducer.publicKey = PublicKey.readObject(reader);
        blockProducer.stake = reader.readBigInteger();
        reader.end();
        return blockProducer;
    }
}
