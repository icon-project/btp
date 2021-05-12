package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;
import java.math.BigInteger;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class ImOnlineEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return heartbeatReceived(input);
            case (byte)(0x01):
                return allGood(input);
            case (byte)(0x02):
                return someOffline(input);
        }
        return null;
    }

    public static byte[] heartbeatReceived(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] allGood(ByteSliceInput input) {
        return null;
    }

    public static byte[] someOffline(ByteSliceInput input) {
        int startPoint = input.getOffset();
        int identificationSize = ScaleReader.readUintCompactSize(input);
        for (int i = 0; i < identificationSize; i++) {
            input.take(32); // validatorId
            BigInteger res =  ScaleReader.readCompacBigInteger(input); // totalBalance
            ScaleReader.readCompacBigInteger(input); // ownBalance
            long individualExposureSize = ScaleReader.readUintCompactSize(input);
            for (int j = 0; j < individualExposureSize; j++) {
                input.take(32); // who
                ScaleReader.readCompacBigInteger(input); // value
            }
        }
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }
}