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

public class GrandpaEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return newAuthorities(input);
            case (byte)(0x01):
                return paused(input);
            case (byte)(0x02):
                return resumed(input);
        }
        return null;
    }

    public static byte[] newAuthorities(ByteSliceInput input) {
        int startPoint = input.getOffset();
        int validatorSize = ScaleReader.readUintCompactSize(input);
        int compactSize = input.getOffset() - startPoint;
        // for (int i = 0; i < validatorSize; i++) {
        //     validators.add(input.take(32));
        //     input.take(8); // AuthorityWeight u64
        // }

        return input.take( (32 + 8 ) * validatorSize + compactSize);
    }

    public static byte[] paused(ByteSliceInput input) {
        return null;
    }

    public static byte[] resumed(ByteSliceInput input) {
        return null;
    }
}