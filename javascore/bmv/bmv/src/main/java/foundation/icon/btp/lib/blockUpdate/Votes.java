package foundation.icon.btp.lib.blockupdate;

import java.util.List;

import foundation.icon.btp.lib.utils.Arrays;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Signature;
import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import java.math.BigInteger;

import score.ObjectReader;
import score.Context;

import scorex.util.ArrayList;

public class Votes {
    private byte[] targetHash;
    private long targetNumber;
    private BigInteger round;
    private BigInteger setId;
    private byte[] serializedVoteMessage;
    private List<ValidatorSignature> signatures;

    // {
    //     message: {
    //       Precommit: {
    //         targetHash: "HASH",
    //         targetNumber: "u32"
    //       },
    //     },
    //     round: u64,
    //     setId: u64,
    //   }
    public Votes(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);

        rlpReader.beginList();
        this.serializedVoteMessage = rlpReader.readByteArray();

        this.signatures = new ArrayList<ValidatorSignature>(100); // 100 items, each signature size 64 bytes
        rlpReader.beginList();
        while (rlpReader.hasNext()) {
            this.signatures.add(ValidatorSignature.fromBytes(rlpReader.readByteArray()));
        }
        rlpReader.end();

        rlpReader.end();

        ByteSliceInput input = new ByteSliceInput(this.serializedVoteMessage);
        int optionPrefix = input.takeUByte(); 
        if (optionPrefix != 0x01) {
            throw new AssertionError("invalid precommit on vote");
        }

        this.targetHash = input.take(32);
        this.targetNumber = ScaleReader.readU32(input);
        this.round = ScaleReader.readU64(input);
        this.setId = ScaleReader.readU64(input);
    }

    public void verify(long height, byte[] blockHash, List<byte[]> validators, BigInteger currentSetId) {
        if (!java.util.Arrays.equals(targetHash, blockHash)) {
            Context.revert(ErrorCode.INVALID_VOTES, "validator signature invalid block hash");
        }

        if (this.targetNumber != height) {
            Context.revert(ErrorCode.INVALID_VOTES, "validator signature invalid block height");
        }

        if (!this.setId.equals(currentSetId)) {
            Context.revert(ErrorCode.INVALID_VOTES, "verify signature for invalid validator set id");
        }

        List<byte[]> containedValidators = new ArrayList<byte[]>(validators.size());
        for (int i = 0; i < this.signatures.size(); i++) {
            ValidatorSignature currentSignature = this.signatures.get(i);
            if (!currentSignature.verify(this.serializedVoteMessage)) {
                Context.revert(ErrorCode.INVALID_VOTES, "invalid signature");
            }

            if (!Arrays.arrayListContains(currentSignature.getId(), validators)) {
                Context.revert(ErrorCode.INVALID_VOTES, "one of signature is not belong to validator");
            }
            
            if (Arrays.arrayListContains(currentSignature.getId(), containedValidators)) {
                Context.revert(ErrorCode.INVALID_VOTES, "duplicated signature");
            }

            containedValidators.add(currentSignature.getId());
        }

        if (containedValidators.size() <= (validators.size() * 2/3)) {
            Context.revert(ErrorCode.INVALID_VOTES, "require signature +2/3");
        }
    }

    public BigInteger getSetId() {
        return this.setId;
    }

    public long getTargetNumber() {
        return this.targetNumber;
    }

    public byte[] getTargetHash() {
        return this.targetHash;
    }

    public List<ValidatorSignature> getSignatures() {
        return this.signatures;
    }

    public byte[] getMessage() {
        return this.serializedVoteMessage;
    }

    public static Votes fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new Votes(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("Votes", e.toString());
        }
    }
}