package foundation.icon.btp.lib.blockproof;

import java.util.List;

import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.exception.mta.InvalidWitnessOldException;
import foundation.icon.btp.lib.exception.mta.MTAException;
import foundation.icon.btp.lib.mta.SerializableMTA;

import score.Context;

public class BlockWitness {
    private final long height;
    private final List<byte[]> witness;

    public BlockWitness(long height, List<byte[]> witness) {
        this.height = height;
        this.witness = witness;
    }

    public void verify(SerializableMTA  mta, byte[] hash, long height) {
        try {
            mta.verify(this.witness, hash, height, this.height);
        } catch (InvalidWitnessOldException e) {
            Context.revert(ErrorCode.INVALID_BLOCK_WITNESS_OLD, e.toString());
        } catch (MTAException e) {
            Context.revert(ErrorCode.INVALID_BLOCK_WITNESS, e.toString());
        }
    }

    public long getHeight() {
        return this.height;
    }

    public List<byte[]> getWitness() {
        return this.witness;
    }
}