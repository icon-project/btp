package foundation.icon.test.cases;

import java.util.Arrays;
import java.util.List;

import score.util.Crypto;
import scorex.util.ArrayList;

import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class BlockVote {
    private final byte[] signature;
    private final byte[] validator;

    public BlockVote(byte[] voteMessage, byte[] publicKey, byte[] secretKey) {
        this.validator = publicKey;
        this.signature = Crypto.sign("ed25519", voteMessage, secretKey);
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public byte[] getValidator() {
        return this.validator;
    }

    public byte[] encode() {
        List<RlpType> validatorSignature = new ArrayList<RlpType>(2);
        validatorSignature.add(RlpString.create(this.signature));
        validatorSignature.add(RlpString.create(this.validator));
        return RlpEncoder.encode(new RlpList(validatorSignature));
    }
}