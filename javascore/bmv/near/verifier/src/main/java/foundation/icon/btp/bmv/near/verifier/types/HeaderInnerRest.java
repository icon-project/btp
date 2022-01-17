package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.Borsh;
import org.near.borshj.BorshBuffer;
import score.ObjectReader;
import score.Context;
import java.math.BigInteger;
import scorex.util.ArrayList;
import java.util.List;
import java.util.Optional;
import foundation.icon.btp.bmv.near.verifier.utils.Hasher;
import foundation.icon.score.util.StringUtil;

public class HeaderInnerRest implements Borsh {
    private byte[] chunkReceiptsRoot;
    private byte[] chunkHeadersRoot;
    private byte[] chunkTransactionRoot;
    private byte[] challengesRoot;
    private byte[] randomValue;
    private List<String> validatorProposals; // TODO
    private List<Boolean> chunkMask;
    private BigInteger gasPrice;
    private BigInteger totalSupply;
    private List<String> challengesResult; // TODO
    private byte[] lastFinalBlock;
    private byte[] lastDSFinalBlock;
    private long blockOrdinal;
    private long previousHeight;
    private Optional<byte[]> epochSyncDataHash;
    private List<Optional<byte[]>> approvals;
    private int latestProtocolVersion;

    @Override
    public void append(BorshBuffer buffer) {
        buffer.write(chunkReceiptsRoot);
        buffer.write(chunkHeadersRoot);
        buffer.write(chunkTransactionRoot);
        buffer.write(challengesRoot);
        buffer.write(randomValue);
        buffer.write(validatorProposals);
        buffer.write(chunkMask);
        buffer.write(gasPrice);
        buffer.write(totalSupply);
        buffer.write(challengesResult);
        buffer.write(lastFinalBlock);
        buffer.write(lastDSFinalBlock);
        buffer.write(blockOrdinal);
        buffer.write(previousHeight);
        buffer.write(epochSyncDataHash);
        buffer.write(approvals);
        buffer.write(latestProtocolVersion);
    }

    public static HeaderInnerRest readObject(ObjectReader reader) {
        HeaderInnerRest headerInnerRest = new HeaderInnerRest();

        reader.beginList();
        headerInnerRest.chunkReceiptsRoot = reader.readByteArray();
        headerInnerRest.chunkHeadersRoot = reader.readByteArray();
        headerInnerRest.chunkTransactionRoot = reader.readByteArray();
        headerInnerRest.challengesRoot = reader.readByteArray();
        headerInnerRest.randomValue = reader.readByteArray();

        reader.beginList();
        headerInnerRest.validatorProposals = new ArrayList<>();
        while (reader.hasNext()) {
            headerInnerRest.validatorProposals.add(reader.readString());
        }
        reader.end();

        reader.beginList();
        headerInnerRest.chunkMask = new ArrayList<>();
        while (reader.hasNext()) {
            headerInnerRest.chunkMask.add(reader.readBoolean());
        }
        reader.end();

        headerInnerRest.gasPrice = new BigInteger(reader.readString());
        headerInnerRest.totalSupply = new BigInteger(reader.readString());

        reader.beginList();
        headerInnerRest.challengesResult = new ArrayList<>();
        while (reader.hasNext()) {
            headerInnerRest.challengesResult.add(reader.readString());
        }
        reader.end();

        headerInnerRest.lastFinalBlock = reader.readByteArray();
        headerInnerRest.lastDSFinalBlock = reader.readByteArray();
        headerInnerRest.blockOrdinal = reader.readLong();
        headerInnerRest.previousHeight = reader.readLong();
        headerInnerRest.epochSyncDataHash = Optional.ofNullable(reader.readNullable(byte[].class));

        reader.beginList();
        headerInnerRest.approvals = new ArrayList<>();
        while (reader.hasNext()) {
            headerInnerRest.approvals.add(Optional.ofNullable(reader.readNullable(byte[].class)));
        }
        reader.end();

        headerInnerRest.latestProtocolVersion = reader.readInt();
        reader.end();
        return headerInnerRest;
    }

    public static HeaderInnerRest fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);

        return HeaderInnerRest.readObject(reader);
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
        final StringBuilder sb = new StringBuilder("HeaderInnerRest{");
        sb.append("chunkReceiptsRoot=").append(StringUtil.bytesToHex(chunkReceiptsRoot));
        sb.append(", chunkHeadersRoot=").append(StringUtil.bytesToHex(chunkHeadersRoot));
        sb.append(", chunkTransactionRoot=").append(StringUtil.bytesToHex(chunkTransactionRoot));
        sb.append(", challengesRoot=").append(StringUtil.bytesToHex(challengesRoot));
        sb.append(", randomValue=").append(StringUtil.bytesToHex(randomValue));
        sb.append(", validatorProposals=").append(StringUtil.toString(validatorProposals));
        sb.append(", chunkMask=").append(StringUtil.toString(chunkMask));
        sb.append(", gasPrice=").append(StringUtil.toString(gasPrice));
        sb.append(", totalSupply=").append(StringUtil.toString(totalSupply));
        sb.append(", challengesResult=").append(StringUtil.toString(challengesResult));
        sb.append(", lastFinalBlock=").append(StringUtil.bytesToHex(lastFinalBlock));
        sb.append(", lastDSFinalBlock=").append(StringUtil.bytesToHex(lastDSFinalBlock));
        sb.append(", blockOrdinal=").append(blockOrdinal);
        sb.append(", previousHeight=").append(previousHeight);
        sb.append(", epochSyncDataHash=").append(StringUtil.toString(epochSyncDataHash));
        sb.append(", approvals=").append(StringUtil.toString(approvals));
        sb.append(", latestProtocolVersion=").append(latestProtocolVersion);
        sb.append('}');
        return sb.toString();
    }
}
