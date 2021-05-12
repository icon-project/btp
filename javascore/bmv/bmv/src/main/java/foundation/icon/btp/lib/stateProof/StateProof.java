package foundation.icon.btp.lib.stateproof;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.mpt.MPTNode;
import foundation.icon.btp.lib.mpt.MerklePatriciaTree;
import foundation.icon.btp.lib.mpt.Nibbles;
import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

public class StateProof {
    private final byte[] key;
    private final ArrayList<MPTNode> proofs = new ArrayList<MPTNode>(5);

    public StateProof(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();
        this.key = rlpReader.readByteArray();

        rlpReader.beginList();
        while (rlpReader.hasNext()) {
            MPTNode n = new MPTNode(rlpReader.readByteArray());
            this.proofs.add(n);
        }
        rlpReader.end();

        rlpReader.end();
    }

    public byte[] prove(byte[] root) {
        try {
            Nibbles keyNibbles = new Nibbles(this.key, false); 
            return MerklePatriciaTree.prove(root, keyNibbles, this.proofs);
        } catch (Exception e) {
            Context.revert(ErrorCode.INVALID_MPT, e.toString());
            return null;
        }
    }

    public byte[] getKey() {
        return this.key;
    }

    public ArrayList<MPTNode> getProofs() {
        return this.proofs;
    }

    public static StateProof fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new StateProof(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("StateProof ", e.toString());
        }
    }
}