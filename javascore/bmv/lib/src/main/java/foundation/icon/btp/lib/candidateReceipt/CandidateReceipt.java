package foundation.icon.btp.lib.candidatereceipt;

import score.Context;
import scorex.util.ArrayList;

import foundation.icon.btp.lib.utils.ByteSliceInput;

public class CandidateReceipt {
    private CandidateDescriptor descriptor;
    private byte[] commitmentsHash;

    public CandidateReceipt(ByteSliceInput input) {
        this.descriptor = new CandidateDescriptor(input);
        this.commitmentsHash = input.take(32);
    }

    public CandidateDescriptor getCandidateDescriptor() {
        return this.descriptor;
    }

    public byte[] getCommitmentsHash() {
        return this.commitmentsHash;
    }
}
