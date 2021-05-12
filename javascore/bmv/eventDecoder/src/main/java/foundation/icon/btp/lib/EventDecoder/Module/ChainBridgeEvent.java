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

public class ChainBridgeEvent {
    public static byte[] decodeEvent(byte subIndex, ByteSliceInput input) {
        switch (subIndex) {
            case (byte)(0x00):
                return relayerThresholdChanged(input);
            case (byte)(0x01):
                return chainWhitelisted(input);
            case (byte)(0x02):
                return relayerAdded(input);
            case (byte)(0x03):
                return relayerRemoved(input);
            case (byte)(0x04):
                return fungibleTransfer(input);
            case (byte)(0x05):
                return nonFungibleTransfer(input);
            case (byte)(0x06):
                return genericTransfer(input);
            case (byte)(0x07):
                return voteFor(input);
            case (byte)(0x08):
                return voteAgainst(input);
            case (byte)(0x09):
                return proposalApproved(input);
            case (byte)(0x0a):
                return proposalRejected(input);
            case (byte)(0x0b):
                return proposalSucceeded(input);
            case (byte)(0x0c):
                return proposalFailed(input);
        }
        return null;
    }

    public static byte[] relayerThresholdChanged(ByteSliceInput input) {
        // this.threshold = ScaleReader.readU32(input);
        return input.take(4);
    }

    public static byte[] chainWhitelisted(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        return input.take(1);
    }

    public static byte[] relayerAdded(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] relayerRemoved(ByteSliceInput input) {
        // this.accountId = input.take(32);
        return input.take(32);
    }

    public static byte[] fungibleTransfer(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        // this.resourceId = input.take(32);
        // this.balance = ScaleReader.readU256(input);
        int size = 1 + 8 + 32 + 32;
        int startPoint = input.getOffset();
        input.seek(startPoint + size);
        ScaleReader.readBytes(input); // metadata
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }

    public static byte[] nonFungibleTransfer(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        // this.resourceId = input.take(32);
        int size = 1 + 8 + 32;
        int startPoint = input.getOffset();
        input.seek(startPoint + size);
        ScaleReader.readBytes(input);
        ScaleReader.readBytes(input);
        ScaleReader.readBytes(input);
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }

    public static byte[] genericTransfer(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        // this.resourceId = input.take(32);
        int size = 1 + 8 + 32;
        int startPoint = input.getOffset();
        input.seek(startPoint + size);
        ScaleReader.readBytes(input);
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return input.take(endPoint - startPoint);
    }

    public static byte[] voteFor(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        // this.accountId = input.take(32);
        return input.take(1 + 8 + 32);
    }

    public static byte[] voteAgainst(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        // this.accountId = input.take(32);
        return input.take(1 + 8 + 32);
    }

    public static byte[] proposalApproved(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        return input.take(1 + 8);
    }

    public static byte[] proposalRejected(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        return input.take(1 + 8);
    }

    public static byte[] proposalSucceeded(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        return input.take(1 + 8);
    }

    public static byte[] proposalFailed(ByteSliceInput input) {
        // this.chainId = ScaleReader.readU8(input);
        // this.nonce = ScaleReader.readU64(input);
        return input.take(1 + 8);
    }
}