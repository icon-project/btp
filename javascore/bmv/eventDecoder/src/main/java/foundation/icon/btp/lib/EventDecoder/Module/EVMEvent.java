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

public class EVMEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return log(input);
            case (byte)(0x01):
                return created(input);
            case (byte)(0x02):
                return createdFailed(input);
            case (byte)(0x03):
                return executed(input);
            case (byte)(0x04):
                return executedFailed(input);
            case (byte)(0x05):
                return balanceDeposit(input);
            case (byte)(0x06):
                return balanceWithdraw(input);
        }
        return null;
    }

    public static byte[] log(ByteSliceInput input) {
        int startPoint = input.getOffset();
        byte[] address = input.take(20); // 20 bytes address of contract
        int topicSize = ScaleReader.readUintCompactSize(input); // u32 compact number of item in list
        for (int i = 0; i < topicSize; i++) {
            input.take(32); // 32 bytes of topic;
        }

        int evmDataSize = ScaleReader.readUintCompactSize(input); // u32 compact number of bytes of evm data
        byte[] evmData = input.take(evmDataSize);
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }

    public static byte[] created(ByteSliceInput input) {
        // this.address = input.take(20);
        return input.take(20);
    }

    public static byte[] createdFailed(ByteSliceInput input) {
        // this.address = input.take(20);
        return input.take(20);
    }

    public static byte[] executed(ByteSliceInput input) {
        // this.address = input.take(20);
        return input.take(20);
    }

    public static byte[] executedFailed(ByteSliceInput input) {
        // this.address = input.take(20);
        return input.take(20);
    }

    public static byte[] balanceDeposit(ByteSliceInput input) {
        // his.sender = input.take(32);
        // this.address = input.take(20);
        // this.value = ScaleReader.readU256(input);
        return input.take(32 + 20 + 32);
    }

    public static byte[] balanceWithdraw(ByteSliceInput input) {
        // this.sender = input.take(32);
        // this.address = input.take(20);
        // this.value = ScaleReader.readU256(input);
        return input.take(32 + 20 + 32);
    }
}