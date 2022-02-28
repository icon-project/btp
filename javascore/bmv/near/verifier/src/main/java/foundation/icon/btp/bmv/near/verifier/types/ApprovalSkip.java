package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.BorshBuffer;

public class ApprovalSkip implements ApprovalInner {
    private long height;

    public ApprovalSkip(long height) {
        this.height = height;
    }

    @Override
    public void append(BorshBuffer writer) {
        writer.write((byte)(1));
        writer.write(height);
    }
}
