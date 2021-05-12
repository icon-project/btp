package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class ElectionsEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return newTerm(input);
            case (byte)(0x01):
                return emptyTerm(input);
            case (byte)(0x02):
                return electionError(input);
            case (byte)(0x03):
                return memberKicked(input);
            case (byte)(0x04):
                return candidateSlashed(input);
            case (byte)(0x05):
                return seatHolderSlashed(input);
            case (byte)(0x06):
                return memberRenounced(input);
            case (byte)(0x07):
                return voterReported(input);
        }
        return null;
    }

    public static byte[] newTerm(ByteSliceInput input) {
        int startPoint = input.getOffset();
        int memberSize = ScaleReader.readUintCompactSize(input);
        int compactSize = input.getOffset() - startPoint;
        return input.take((32 + 16) * memberSize + compactSize);
    }

    public static byte[] emptyTerm(ByteSliceInput input) {
        return null;
    }

    public static byte[] electionError(ByteSliceInput input) {
        return null;
    }

    public static byte[] memberKicked(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] candidateSlashed(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] seatHolderSlashed(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] memberRenounced(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] voterReported(ByteSliceInput input) {
        // this.voter = input.take(32);
        // this.reporter = input.take(32);
        // this.success = input.takeByte();
        return input.take(32 + 32 + 1);
    }
}