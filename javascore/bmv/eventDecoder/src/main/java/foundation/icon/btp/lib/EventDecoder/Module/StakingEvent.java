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

public class StakingEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return eraPayout(input);
            case (byte)(0x01):
                return reward(input);
            case (byte)(0x02):
                return slash(input);
            case (byte)(0x03):
                return oldSlashingReportDiscarded(input);
            case (byte)(0x04):
                return stakingElection(input);
            case (byte)(0x05):
                return solutionStored(input);
            case (byte)(0x06):
                return bonded(input);
            case (byte)(0x07):
                return unbonded(input);
            case (byte)(0x08):
                return withdrawn(input);
        }
        return null;
    }

    public static byte[] eraPayout(ByteSliceInput input) {
        // this.eraIndex = ScaleReader.readU32(input);
        // this.payout = ScaleReader.readU128(input);
        // this.remainder = ScaleReader.readU128(input);
        return input.take(32 + 16 + 16);
    }

    public static byte[] reward(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }
    
    public static byte[] slash(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] oldSlashingReportDiscarded(ByteSliceInput input) {
        // this.sessionIndex = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] stakingElection(ByteSliceInput input) {
        // this.electionCompute = input.takeByte();
        return input.take(1);
    }

    public static byte[] solutionStored(ByteSliceInput input) {
        // this.electionCompute = input.takeByte();
        return input.take(1);
    }

    public static byte[] bonded(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] unbonded(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }

    public static byte[] withdrawn(ByteSliceInput input) {
        // this.accountId = input.take(32);
        // this.balance = ScaleReader.readU128(input);
        return input.take(32 + 16);
    }
}