package foundation.icon.btp.lib.eventdecoder;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class EventDecoder {
    public static byte[] decodeEvent(byte mainIndex, byte subIndex, ByteSliceInput input) {
        switch (mainIndex) {
            case (byte)(0x00):
                return SystemEvent.decodeEvent(subIndex, input);
            case (byte)(0x01):
                return UtilityEvent.decodeEvent(subIndex, input);
            case (byte)(0x05):
                return IndicesEvent.decodeEvent(subIndex, input);
            case (byte)(0x06):
                return BalancesEvent.decodeEvent(subIndex, input);
            case (byte)(0x08):
                return StakingEvent.decodeEvent(subIndex, input);
            case (byte)(0x09):
                return SessionEvent.decodeEvent(subIndex, input);
            case (byte)(0x0a):
                return DemocracyEvent.decodeEvent(subIndex, input);
            case (byte)(0x0b):
                return CouncilEvent.decodeEvent(subIndex, input);
            case (byte)(0x0c):
                return ElectionsEvent.decodeEvent(subIndex, input);
            case (byte)(0x0e):
                return GrandpaEvent.decodeEvent(subIndex, input);
            case (byte)(0x0f):
                return TreasuryEvent.decodeEvent(subIndex, input);
            case (byte)(0x10):
                return ContractsEvent.decodeEvent(subIndex, input);
            case (byte)(0x11):
                return SudoEvent.decodeEvent(subIndex, input);
            case (byte)(0x12):
                return ImOnlineEvent.decodeEvent(subIndex, input);
            case (byte)(0x14):
                return OffencesEvent.decodeEvent(subIndex, input);
            case (byte)(0x17):
                return IdentityEvent.decodeEvent(subIndex, input);
            case (byte)(0x18):
                return RecoveryEvent.decodeEvent(subIndex, input);
            case (byte)(0x19):
                return VestingEvent.decodeEvent(subIndex, input);
            case (byte)(0x1a):
                return SchedulerEvent.decodeEvent(subIndex, input);
            case (byte)(0x1b):
                return ProxyEvent.decodeEvent(subIndex, input);
            case (byte)(0x1c):
                return MultisigEvent.decodeEvent(subIndex, input);
            case (byte)(0x1d):
                return AssetsEvent.decodeEvent(subIndex, input);
            case (byte)(0x20):
                return TreasuryRewardEvent.decodeEvent(subIndex, input);
            case (byte)(0x21):
                return EthereumEvent.decodeEvent(subIndex, input);
            case (byte)(0x22):
                return EVMEvent.decodeEvent(subIndex, input);
            case (byte)(0x23):
                return ChainBridgeEvent.decodeEvent(subIndex, input);
            case (byte)(0x24):
                return EdgeBridgeEvent.decodeEvent(subIndex, input);
            case (byte)(0x25):
                return BountiesEvent.decodeEvent(subIndex, input);
            case (byte)(0x26):
                return TipsEvent.decodeEvent(subIndex, input);
        }
        return null;
    }

    public static EventRecord decode(ByteSliceInput input) {
        EventRecord eventRecord = new EventRecord();
        eventRecord.phaseEnum = input.takeByte();
        if ((eventRecord.phaseEnum & 0xff) == 0x00) {
            eventRecord.phaseData = input.take(4);
        }

        eventRecord.eventIndex = input.take(2);
        eventRecord.eventData = decodeEvent(eventRecord.eventIndex[0], eventRecord.eventIndex[1], input);

        long topicsLength = ScaleReader.readUintCompactSize(input);
        eventRecord.topics = new ArrayList<byte[]>((int)topicsLength);
        for (int i = 0; i < topicsLength; i++) {
            eventRecord.topics.add(input.take(32));
        }
        return eventRecord;
    }
}