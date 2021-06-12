package foundation.icon.btp.lib.candidatereceipt;

import score.Context;
import scorex.util.ArrayList;

import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.scale.ScaleReader;

public class CandidateDescriptor {
    private long paraId;
    private byte[] relayParent;
    private byte[] collatorId;
    private byte[] persistedValidationDataHash;
    private byte[] povHash;
    private byte[] erasureRoot;
    private byte[] signature;
    private byte[] paraHead;
    private byte[] validationCodeHash;

    public CandidateDescriptor(ByteSliceInput input) {
        this.paraId = ScaleReader.readU32(input);
        this.relayParent = input.take(32);
        this.collatorId = input.take(32);
        this.persistedValidationDataHash = input.take(32);
        this.povHash = input.take(32);
        this.erasureRoot = input.take(32);
        this.signature = input.take(64);
        this.paraHead = input.take(32);
        this.validationCodeHash = input.take(32);
    }

    public long getParaId() {
        return this.paraId;
    }

    public byte[] getRelayParent() {
        return this.relayParent;
    }
    
    public byte[] getCollatorId() {
        return this.collatorId;
    }

    public byte[] getpPersistedValidationDataHash() {
        return this.persistedValidationDataHash;
    }

    public byte[] getPovHash() {
        return this.povHash;
    }

    public byte[] getErasureRoot() {
        return this.erasureRoot;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public byte[] getParaHead() {
        return this.paraHead;
    }

    public byte[] getValidationCodeHash() {
        return this.validationCodeHash;
    }
}
