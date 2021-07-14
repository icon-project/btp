package foundation.icon.test.cases;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import score.ObjectReader;
import score.util.Crypto;

import scorex.util.ArrayList;

import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class ParaBlockUpdate {
    private final byte[] hash;
    private final byte[] parentHash;
    private final long number;
    private final byte[] stateRoot;
    private final byte[] extrinsicsRoot;
    private final byte[] digest;
    private final byte[] serializedHeader;
    private byte[] relayChainData;

    public ParaBlockUpdate(byte[] parentHash, long number, byte[] stateRoot, byte[] relayChainData) {
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

        this.relayChainData = relayChainData;
    }

    public ParaBlockUpdate(byte[] parentHash, long number, byte[] stateRoot) {
        this(parentHash, number, stateRoot, null);
    }

    public ParaBlockUpdate(byte[] relayChainData) {
        this.parentHash = null;
        this.number = 0;
        this.stateRoot = null;

        this.extrinsicsRoot = null;
        this.digest = null;
        this.hash = null;
        this.serializedHeader = null;

        this.relayChainData = relayChainData;
    }

    public byte[] encode() {
        List<RlpType> blockUpdate = new ArrayList<RlpType>(2);
        if (this.serializedHeader != null) {
            blockUpdate.add(RlpString.create(this.serializedHeader));
        } else {
            blockUpdate.add(RlpString.create(""));
        }

        if (this.relayChainData != null) {
            blockUpdate.add(RlpString.create(this.relayChainData));
        } else {
            blockUpdate.add(RlpString.create(""));
        }
        return RlpEncoder.encode(new RlpList(blockUpdate));
    }

    public byte[] getEncodedHeader() {
        return this.serializedHeader;
    }

    public byte[] getHash() {
        return this.hash;
    }

    public byte[] setRelayChainData(byte[] relayChainData) {
        return this.relayChainData = relayChainData;
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