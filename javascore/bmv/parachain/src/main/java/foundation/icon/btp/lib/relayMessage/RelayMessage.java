package foundation.icon.btp.lib.parachain.relaymessage;

import foundation.icon.btp.lib.blockproof.BlockProof;
import foundation.icon.btp.lib.parachain.blockupdate.*;
import foundation.icon.btp.lib.stateproof.StateProof;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import java.util.List;

import scorex.util.ArrayList;

import score.Context;
import score.ObjectReader;

public class RelayMessage {
    private final List<ParaBlockUpdate> blockUpdates = new ArrayList<ParaBlockUpdate>(16);
    private BlockProof blockProof;
    private final List<StateProof> stateProofs = new ArrayList<StateProof>(10);

    public RelayMessage(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader r = Context.newByteArrayObjectReader("RLPn", serialized);
        r.beginList();

        // decode list of Prachain BlockUpdate
        if (r.hasNext()) {
            r.beginList();
            while (r.hasNext()) {
                byte[] encodedBlockUpdate = r.readByteArray();
                blockUpdates.add(ParaBlockUpdate.fromBytes(encodedBlockUpdate));
            }
            r.end();
        }

        // decode Parachain block proof
        if (r.hasNext()) {
            byte[] blockProofEncoded = r.readByteArray();
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

    public List<ParaBlockUpdate> getBlockUpdate() {
        return this.blockUpdates;
    }

    public BlockProof getBlockProof() {
        return this.blockProof;
    }

    public List<StateProof> getStateProof() {
        return this.stateProofs;
    }

    public static RelayMessage fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new RelayMessage(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("RelayMessage", e.toString());
        }
    }
}