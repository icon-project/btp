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

public class RecoveryEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return recoveryCreated(input);
            case (byte)(0x01):
                return recoveryInitiated(input);
            case (byte)(0x02):
                return recoveryVouched(input);
            case (byte)(0x03):
                return recoveryClosed(input);
            case (byte)(0x04):
                return accountRecovered(input);
            case (byte)(0x05):
                return recoveryRemoved(input);
        }
        return null;
    }

    public static byte[] recoveryCreated(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] recoveryInitiated(ByteSliceInput input) {
        // this.lost = input.take(32);
        // this.rescuer = input.take(32);
        return input.take(32 + 32);
    }

    public static byte[] recoveryVouched(ByteSliceInput input) {
        // this.lost = input.take(32);
        // this.rescuer = input.take(32);
        // this.sender = input.take(32);
        return input.take(32 + 32 + 32);
    }

    public static byte[] recoveryClosed(ByteSliceInput input) {
        // this.lost = input.take(32);
        // this.rescuer = input.take(32);
        return input.take(32 + 32);
    }

    public static byte[] accountRecovered(ByteSliceInput input) {
        // this.lost = input.take(32);
        // this.rescuer = input.take(32);
        return input.take(32 + 32);
    }

    public static byte[] recoveryRemoved(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }
}