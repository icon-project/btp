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

public class TreasuryRewardEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return treasuryMinting(input);
        }
        return null;
    }

    public static byte[] treasuryMinting(ByteSliceInput input) {
        // this.balance = ScaleReader.readU128(input);
        // this.blockNumber = ScaleReader.readU32(input);
        // this.accountId = input.take(32);
        return input.take(16 + 4 + 32);
    }
}