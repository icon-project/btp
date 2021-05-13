package foundation.icon.btp.lib.blockproof;

import java.util.List;

import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.blockupdate.BlockHeader;
import foundation.icon.btp.lib.mta.SerializableMTA;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import score.Context;
import score.ObjectReader;

import scorex.util.ArrayList;

public class BlockProof {
    private final BlockHeader blockHeader;
    private final BlockWitness blockWitness;

    public BlockProof(byte[] serialized) {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();

        if (rlpReader.hasNext()) {
            byte[] encodedHeader = rlpReader.readByteArray();
            if (encodedHeader != null && encodedHeader.length > 0) {
                this.blockHeader = new BlockHeader(encodedHeader);
            } else {
                this.blockHeader = null;
            }
        } else {
            this.blockHeader = null;
        }

        long mtaHeight = 0;
        if (rlpReader.hasNext()) {
            mtaHeight = rlpReader.readLong();
        }

        List<byte[]> witnesses = new ArrayList<byte[]>(5);
        if (rlpReader.hasNext()) {
            rlpReader.beginList();
            while (rlpReader.hasNext()) {
                byte[] witness = rlpReader.readByteArray();
                witnesses.add(witness);
            }
            rlpReader.end();
        }

        rlpReader.end();

        this.blockWitness = new BlockWitness(mtaHeight, witnesses);
    }

    public void verify(SerializableMTA mta) {
        if (mta.height() < this.blockHeader.getNumber()) {
            Context.revert(ErrorCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER, "given block height is newer " + this.blockHeader.getNumber() + " expect: " + mta.height());
        }

        this.blockWitness.verify(mta, this.blockHeader.getHash(), this.blockHeader.getNumber());
    }

    public BlockHeader getBlockHeader() {
        return this.blockHeader;
    }

    public BlockWitness getBlockWitness() {
        return this.blockWitness;
    }

    public static BlockProof fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new BlockProof(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("BlockProof", e.toString());
        }
    }
}