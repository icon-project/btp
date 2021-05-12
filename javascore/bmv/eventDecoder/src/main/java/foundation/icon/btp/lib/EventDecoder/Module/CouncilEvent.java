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

public class CouncilEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return proposed(input);
            case (byte)(0x01):
                return voted(input);
            case (byte)(0x02):
                return approved(input);
            case (byte)(0x03):
                return disapproved(input);
            case (byte)(0x04):
                return executed(input);
            case (byte)(0x05):
                return memberExecuted(input);
            case (byte)(0x06):
                return closed(input);
        }
        return null;
    }

    public static byte[] proposed(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.proposalIndex = ScaleReader.readU32(input);
        // this.proposalHash = input.take(32);
        // this.threshold = ScaleReader.readU32(input);
        return input.take(32 + 4 + 32 + 4);
    }

    public static byte[] voted(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.proposalHash = input.take(32);
        // this.voted = input.takeByte();
        // this.numberYes = ScaleReader.readU32(input);
        // this.numberNo = ScaleReader.readU32(input);
        return input.take(32 + 32 + 1 + 4 + 4);
    }

    public static byte[] approved(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        return input.take(32);
    }

    public static byte[] disapproved(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        return input.take(32);
    }

    public static byte[] executed(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte[] proposalHash = input.take(32);
        byte dispatchResultEnum = input.takeByte();
        if (dispatchResultEnum == (byte) (0x01 & 0xff)) {
            byte dispatchErrorEnum = input.takeByte();
            if (dispatchErrorEnum == (byte) (0x03 & 0xff)) {
                byte[] dispatchError = input.take(2);
            }
        }
        int endpoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endpoint - startPoint);
    }

    public static byte[] memberExecuted(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte[] proposalHash = input.take(32);
        byte dispatchResultEnum = input.takeByte();
        if (dispatchResultEnum == (byte) (0x01 & 0xff)) {
            byte dispatchErrorEnum = input.takeByte();
            if (dispatchErrorEnum == (byte) (0x03 & 0xff)) {
                byte[] dispatchError = input.take(2);
            }
        }
        int endpoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endpoint - startPoint);
    }

    public static byte[] closed(ByteSliceInput input) {
        // this.proposalHash = input.take(32);
        // this.numberYes = ScaleReader.readU32(input);
        // this.numberNo = ScaleReader.readU32(input);
        return input.take(32 + 4 + 4);
    }
}