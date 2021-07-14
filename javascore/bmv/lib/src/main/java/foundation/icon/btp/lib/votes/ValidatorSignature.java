package foundation.icon.btp.lib.votes;

import java.math.BigInteger;

import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import score.ObjectReader;
import score.Context;

public class ValidatorSignature {
    private byte[] signature;
    private byte[] id;

    public ValidatorSignature(byte[] serialized) {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();
        this.signature = rlpReader.readByteArray();
        this.id = rlpReader.readByteArray();
        rlpReader.end();
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public byte[] getId() {
        return this.id;
    }

    public boolean verify(byte[] msg) {
        return Context.verifySignature("ed25519", msg, this.signature, this.id);
    }

    public static ValidatorSignature fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new ValidatorSignature(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("ValidatorSignature", e.toString());
        }
    }
}