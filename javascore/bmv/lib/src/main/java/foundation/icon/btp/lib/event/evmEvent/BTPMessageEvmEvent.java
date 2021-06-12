package foundation.icon.btp.lib.event.evmevent;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.utils.AbiDecoder;
import foundation.icon.btp.lib.utils.ByteSliceInput;

import score.Context;
import scorex.util.ArrayList;

public class BTPMessageEvmEvent {
    private final String next;
    private final BigInteger seq;
    private final byte[] msg;

    public BTPMessageEvmEvent(byte[] data) {
        ByteSliceInput input = new ByteSliceInput(data);
        BigInteger nextPosition = AbiDecoder.decodeUInt(input);

        this.seq = AbiDecoder.decodeUInt(input);

        BigInteger msgPosition = AbiDecoder.decodeUInt(input);

        if (!nextPosition.equals(BigInteger.valueOf(input.getOffset()))) {
            throw new AssertionError("Message evm data invalid, next position incorrect" + nextPosition.toString() + "; expected: " + input.getOffset());
        }
        this.next = AbiDecoder.decodeString(input);

        if (!msgPosition.equals(BigInteger.valueOf(input.getOffset()))) {
            throw new AssertionError("Message evm data invalid, msg position incorrect : " + msgPosition.toString() + "; expected: " + input.getOffset());
        }
        this.msg = AbiDecoder.decodeBytes(input);
    }

    public String getNextBmc() {
        return this.next;
    }

    public BigInteger getSeq() {
        return this.seq;
    }

    public byte[] getMsg() {
        return this.msg;
    }
}