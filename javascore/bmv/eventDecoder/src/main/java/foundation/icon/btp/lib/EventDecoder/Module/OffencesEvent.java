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

public class OffencesEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return offence(input);
        }
        return null;
    }

    public static byte[] offence(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte[] kind = input.take(16);
        int timeslotSize = ScaleReader.readUintCompactSize(input);
        byte[] timeslot = input.take(timeslotSize);
        byte applied = input.takeByte();
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }
}