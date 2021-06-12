package foundation.icon.btp.lib.event;

import java.math.BigInteger;
import java.util.List;

import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.candidatereceipt.CandidateReceipt;

import score.Context;
import scorex.util.ArrayList;

public class CandidateIncludedEvent {
    private CandidateReceipt candidateReceipt;
    private byte[] headData;
    private long coreIndex;
    private long groupIndex;

    public CandidateIncludedEvent(byte[] eventData) {
        ByteSliceInput input = new ByteSliceInput(eventData);
        this.candidateReceipt = new CandidateReceipt(input);
        this.headData = ScaleReader.readBytes(input);
        this.coreIndex = ScaleReader.readU32(input);
        this.groupIndex = ScaleReader.readU32(input);
    }

    public CandidateReceipt getCandidateReceipt() {
        return this.candidateReceipt;
    }

    public byte[] getHeadData() {
        return this.headData;
    }

    public long getCoreIndex() {
        return this.coreIndex;
    }

    public long getGroupIndex() {
        return this.groupIndex;
    }
}
