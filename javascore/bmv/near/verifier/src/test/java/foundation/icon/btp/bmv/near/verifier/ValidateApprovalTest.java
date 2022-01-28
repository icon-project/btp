package foundation.icon.btp.bmv.near.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;
import foundation.icon.btp.bmv.near.verifier.types.ApprovalMessage;
import foundation.icon.btp.bmv.near.verifier.types.ApprovalEndorsement;
import foundation.icon.btp.bmv.near.verifier.types.ApprovalSkip;
import foundation.icon.btp.bmv.near.verifier.types.PublicKey;
import foundation.icon.btp.bmv.near.verifier.types.Signature;
import io.ipfs.multibase.Base58;

public class ValidateApprovalTest {

    @Test
    void validateApprovalEndorsementSuccess() throws DecoderException {
        try {
            var approvalEndorsement = new ApprovalEndorsement(
                    Base58.decode("67j1sSg3QrhmYGkMKhnvr64Sb5ww2PSTSM5Z99VwLawj"));
            var approvalMessage = new ApprovalMessage(approvalEndorsement, 78881303);
            assertTrue(
                    approvalMessage.verify(new PublicKey("ed25519:ydgzeXHJ5Xyt7M1gXLxqLBW1Ejx6scNV5Nx2pxFM8su"),
                            new Signature(
                                    "ed25519:vBH6PMVPVxG8V9yaKr7L483PC5Rdew2yBw5FmALGhERnNQ4yjFPGB9Zr2z7CN5GDwH1nq3X2E5e5mJecDeGVmyu")));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    @Test
    void validateInvalidApprovalEndorsementFail() throws DecoderException {
        try {
            var approvalEndorsement = new ApprovalEndorsement(
                    Base58.decode("67j1sSg3QrhmYGkMKhnvr64Sb5ww2PSTSM5Z99VwLawj"));
            var approvalMessage = new ApprovalMessage(approvalEndorsement, 78881302);
            assertFalse(
                    approvalMessage.verify(new PublicKey("ed25519:ydgzeXHJ5Xyt7M1gXLxqLBW1Ejx6scNV5Nx2pxFM8su"),
                            new Signature(
                                    "ed25519:vBH6PMVPVxG8V9yaKr7L483PC5Rdew2yBw5FmALGhERnNQ4yjFPGB9Zr2z7CN5GDwH1nq3X2E5e5mJecDeGVmyu")));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    @Test
    void validateApprovalSkipSuccess() throws DecoderException {
        try {
            var approvalSkip = new ApprovalSkip(78535701);
            var approvalMessage = new ApprovalMessage(approvalSkip, 78535703);
            assertTrue(
                    approvalMessage.verify(new PublicKey("ed25519:ydgzeXHJ5Xyt7M1gXLxqLBW1Ejx6scNV5Nx2pxFM8su"),
                            new Signature(
                                    "ed25519:4jmCWZswLYANNvbjz5mNPFRp7SCwVzJQxNvB9qyKENpCNQXL5k8nm4UF9iMHyJh3LNXnFQAUJ4fzxrQyavFE66hh")));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    @Test
    void validateApprovalSkipFail() throws DecoderException {
        try {
            var approvalSkip = new ApprovalSkip(78535700);
            var approvalMessage = new ApprovalMessage(approvalSkip, 78535703);
            assertFalse(
                    approvalMessage.verify(new PublicKey("ed25519:ydgzeXHJ5Xyt7M1gXLxqLBW1Ejx6scNV5Nx2pxFM8su"),
                            new Signature(
                                    "ed25519:4jmCWZswLYANNvbjz5mNPFRp7SCwVzJQxNvB9qyKENpCNQXL5k8nm4UF9iMHyJh3LNXnFQAUJ4fzxrQyavFE66hh")));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
