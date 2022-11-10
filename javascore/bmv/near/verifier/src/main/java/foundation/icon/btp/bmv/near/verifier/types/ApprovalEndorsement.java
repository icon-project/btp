package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.BorshBuffer;

public class ApprovalEndorsement implements ApprovalInner {
    private byte[] blockHash;

    public ApprovalEndorsement(byte[] blockHash) {
        this.blockHash = blockHash;
    }

    @Override
    public void append(BorshBuffer writer) {
        writer.write((byte)(0));
        writer.write(blockHash);
    }  
}
