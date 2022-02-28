package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.Borsh;
import org.near.borshj.BorshBuffer;
import score.Context;
import score.ObjectReader;

public class ApprovalMessage implements Borsh {
    private ApprovalInner approvalInner;
    private long targetHeight;

    public ApprovalMessage(ApprovalInner approvalInner, long targetHeight) {
        this.approvalInner = approvalInner;
        this.targetHeight = targetHeight;
    }

    public ApprovalMessage() {
    }

    public ApprovalInner getApprovalInner() {
        return approvalInner;
    }

    public long getTargetHeight() {
        return targetHeight;
    }

    public byte[] borshSerialize() {
        byte[] bytes = Borsh.serialize(this);
        return bytes;
    }

    @Override
    public void append(BorshBuffer writer) {
        writer.write(approvalInner);
        writer.write(targetHeight);
    }

    public boolean verify(PublicKey publicKey, Signature signature) {
        if (publicKey.getKeyType() != signature.getKeyType()) {
            throw new IllegalArgumentException("invalid PublicKey Signature Pair");
        }

        switch (publicKey.getKeyType()) {
            case ED25519:
                return Context.verifySignature("ed25519", this.borshSerialize(), signature.getKey(),
                        publicKey.getKey());
            case SECP256K1:
                return Context.verifySignature("ecdsa-secp256k1", this.borshSerialize(), signature.getKey(),
                        publicKey.getKey());
        }
        return false;
    }

    public static ApprovalMessage readObject(ObjectReader reader) {
        ApprovalMessage approvalMessage = new ApprovalMessage();
        reader.beginList();
        byte[] approvalType = reader.readByteArray();
        switch (approvalType[0]) {
            case 0:
                approvalMessage.approvalInner = new ApprovalEndorsement(reader.readByteArray());
                break;
            case 1:
                approvalMessage.approvalInner = new ApprovalSkip(reader.readLong());
                break;
            default:
                throw new IllegalArgumentException("not supported approval type");
        }
        approvalMessage.targetHeight = reader.readLong();
        reader.end();
        return approvalMessage;
    }
}
