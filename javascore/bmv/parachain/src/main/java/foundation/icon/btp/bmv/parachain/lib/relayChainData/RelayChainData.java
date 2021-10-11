package foundation.icon.btp.bmv.parachain.lib.relayChainData;

import foundation.icon.btp.lib.blockProof.BlockProof;
import foundation.icon.btp.bmv.parachain.lib.blockUpdate.*;
import foundation.icon.btp.lib.stateProof.StateProof;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import java.util.List;

import scorex.util.ArrayList;

import score.Context;
import score.ObjectReader;

public class RelayChainData {
    private final List<RelayBlockUpdate> blockUpdates = new ArrayList<RelayBlockUpdate>(16);
    private BlockProof blockProof;
    private final List<StateProof> stateProofs = new ArrayList<StateProof>(10);

    public RelayChainData(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader r = Context.newByteArrayObjectReader("RLPn", serialized);
        r.beginList();

        // decode list of RelayBlockUpdate
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] encodedBlockUpdate = r.readByteArray();
                blockUpdates.add(RelayBlockUpdate.fromBytes(encodedBlockUpdate));
            }
            r.end();
        }

        // decode Parachain block proof
        if (r.hasNext()) {
            byte[] blockProofEncoded = r.readNullable(byte[].class);
            this.blockProof = (blockProofEncoded != null && blockProofEncoded.length > 0) ? BlockProof.fromBytes(blockProofEncoded) : null;
        } else {
            this.blockProof = null;
        }

        // decode Parachain state proofs
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] encodedStateProof = r.readByteArray();
                this.stateProofs.add(StateProof.fromBytes(encodedStateProof));
            }
            r.end();
        }

        r.end();
    }

    public List<RelayBlockUpdate> getBlockUpdate() {
        return this.blockUpdates;
    }

    public BlockProof getBlockProof() {
        return this.blockProof;
    }

    public List<StateProof> getStateProof() {
        return this.stateProofs;
    }

    public static RelayChainData fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new RelayChainData(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("RelayChainData: ", e.toString());
        }
    }
}