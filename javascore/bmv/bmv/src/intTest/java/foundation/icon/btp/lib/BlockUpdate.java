package foundation.icon.test.cases;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.scale.ScaleWriter;
import foundation.icon.btp.lib.utils.ByteSliceOutput;
import foundation.icon.btp.lib.utils.HexConverter;
import score.ObjectReader;
import score.util.Crypto;

import scorex.util.ArrayList;

import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class BlockUpdate {
    private final byte[] hash;
    private final byte[] parentHash;
    private final long number;
    private final byte[] stateRoot;
    private final byte[] extrinsicsRoot;
    private final byte[] digest;
    private final byte[] serializedHeader;
    private final List<BlockVote> vote = new ArrayList<BlockVote>(5);
    private final BigInteger round;
    private final BigInteger setId;

    public BlockUpdate(byte[] parentHash, long number, byte[] stateRoot, BigInteger round, BigInteger setId) {
        ByteSliceOutput serializedHeaderOutput = new ByteSliceOutput(32 + 4 + 32 + 32 + 1);
        this.parentHash = parentHash;
        serializedHeaderOutput.addMany(parentHash);
        this.number = number;
        ScaleWriter.writeCompactUint(serializedHeaderOutput, (int) number);
        this.stateRoot = stateRoot;
        serializedHeaderOutput.addMany(stateRoot);

        this.extrinsicsRoot = Crypto.hash("blake2b-256", "abc".getBytes());
        serializedHeaderOutput.addMany(extrinsicsRoot);
        this.digest = HexConverter.hexStringToByteArray("080642414245b50103010000009e08cd0f000000006290af7c4a31713b4b36280943a39d8179d36e765c0614176c7c6560e515466197827f75f8c6a0c1c655dde4074710f976c86c2f07766e57063afc07efaaef01aedae58676a262cc4f7264836a17ce8f336ee49865530afbdc560d1d131c050905424142450101fcbc268c71e173ccf1cf8b87b17bcb56914eadb31b71bdd70232ca4bea023f3e501dff7a902c6485b24f7426ad941f7cc086690817d5de0183688923104d1b8d");
        serializedHeaderOutput.addMany(this.digest);
        this.hash = Crypto.hash("blake2b-256", serializedHeaderOutput.toArray());
        this.serializedHeader = serializedHeaderOutput.toArray();

        this.round = round;
        this.setId = setId;
    }

    public static byte[] voteMessage(byte[] targetHash, long targetNumber, BigInteger round, BigInteger setId) {
        ByteSliceOutput serializedSignMessageOutput = new ByteSliceOutput(53);
        serializedSignMessageOutput.add((byte) 0x01);
        serializedSignMessageOutput.addMany(targetHash);
        ScaleWriter.writeU32(serializedSignMessageOutput, (int) targetNumber);
        ScaleWriter.writeU64(serializedSignMessageOutput, round);
        ScaleWriter.writeU64(serializedSignMessageOutput, setId);
        return serializedSignMessageOutput.toArray();
    }

    public void vote(byte[] publicKey, byte[] secretKey, BigInteger round, BigInteger setId) {
        this.vote.add(new BlockVote(voteMessage(this.hash, this.number, round, setId), publicKey, secretKey));
    }

    public byte[] encode() {
        List<RlpType> blockUpdate = new ArrayList<RlpType>(2);
        blockUpdate.add(RlpString.create(this.serializedHeader));

        List<RlpType> votes = new ArrayList<RlpType>(2);
        votes.add(RlpString.create(voteMessage(this.hash, this.number, this.round, this.setId)));
        List<RlpType> validatorSignatures = new ArrayList<RlpType>(2);
        for (BlockVote signature: this.vote) {
            validatorSignatures.add(RlpString.create(signature.encode()));
        }
        votes.add(new RlpList(validatorSignatures));

        blockUpdate.add(RlpString.create(RlpEncoder.encode(new RlpList(votes))));
        return RlpEncoder.encode(new RlpList(blockUpdate));
    }

    public byte[] encodeWithoutVote() {
        List<RlpType> blockUpdate = new ArrayList<RlpType>(2);
        blockUpdate.add(RlpString.create(this.serializedHeader));

        blockUpdate.add(RlpString.create("")); // empty votes
        return RlpEncoder.encode(new RlpList(blockUpdate));
    }

    public byte[] encodeWithVoteMessage(byte[] voteMessage) {
        List<RlpType> blockUpdate = new ArrayList<RlpType>(2);
        blockUpdate.add(RlpString.create(this.serializedHeader));

        List<RlpType> votes = new ArrayList<RlpType>(2);
        votes.add(RlpString.create(voteMessage));
        List<RlpType> validatorSignatures = new ArrayList<RlpType>(2);
        for (BlockVote signature: this.vote) {
            validatorSignatures.add(RlpString.create(signature.encode()));
        }
        votes.add(new RlpList(validatorSignatures));

        blockUpdate.add(RlpString.create(RlpEncoder.encode(new RlpList(votes))));
        return RlpEncoder.encode(new RlpList(blockUpdate));
    }

    public byte[] getEncodedHeader() {
        return this.serializedHeader;
    }

    public byte[] getHash() {
        return this.hash;
    }

    public byte[] getParentHash() {
        return this.parentHash;
    }

    public long getNumber() {
        return this.number;
    }

    public byte[] getStateRoot() {
        return this.stateRoot;
    }
}