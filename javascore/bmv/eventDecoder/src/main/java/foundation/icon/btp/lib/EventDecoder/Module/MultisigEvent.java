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

public class MultisigEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return newMultisig(input);
            case (byte)(0x01):
                return multisigApproval(input);
            case (byte)(0x02):
                return multisigExecuted(input);
            case (byte)(0x03):
                return multisigCancelled(input);
        }
        return null;
    }

    public static byte[] newMultisig(ByteSliceInput input) {
        // this.approving = input.take(32);
        // this.multisig = input.take(32);
        // this.callHash = input.take(32);
        return input.take(32 + 32 + 32);
    }

    public static byte[] multisigApproval(ByteSliceInput input) {
        // this.approving = input.take(32);
        // this.timepointBlockNumber = ScaleReader.readU32(input);
        // this.timepointIndex = ScaleReader.readU32(input);
        // this.multisig = input.take(32);
        // this.callHash = input.take(32);
        return input.take(32 + 4 + 4 + 32 + 32);
    }

    public static byte[] multisigExecuted(ByteSliceInput input) {
        // this.approving = input.take(32);
        // this.timepointBlockNumber = ScaleReader.readU32(input);
        // this.timepointIndex = ScaleReader.readU32(input);
        // this.multisig = input.take(32);
        // this.callHash = input.take(32);
        int size = 32 + 4 + 4 + 32 + 32;

        int startPoint = input.getOffset();
        input.seek(startPoint + size);

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

    public static byte[] multisigCancelled(ByteSliceInput input) {
        // this.cancelling = input.take(32);
        // this.timepointBlockNumber = ScaleReader.readU32(input);
        // this.timepointIndex = ScaleReader.readU32(input);
        // this.multisig = input.take(32);
        // this.callHash = input.take(32);
        return input.take(32 + 4 + 4 + 32 + 32);
    }
}