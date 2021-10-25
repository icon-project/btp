package foundation.icon.btp.lib.EventDecoder;

import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import scorex.util.ArrayList;

public class EventDecoderBase {
    public static EventRecord decode(ByteSliceInput input) {
        EventRecord eventRecord = new EventRecord();
        eventRecord.phaseEnum = input.takeByte();
        if ((eventRecord.phaseEnum & 0xff) == 0x00) {
            eventRecord.phaseData = input.take(4);
        }

        eventRecord.eventIndex = input.take(2);
        eventRecord.eventData = EventDecoder.decodeEvent(eventRecord.eventIndex[0], eventRecord.eventIndex[1], input);

        long topicsLength = ScaleReader.readUintCompactSize(input);
        eventRecord.topics = new ArrayList<byte[]>((int)topicsLength);
        for (int i = 0; i < topicsLength; i++) {
            eventRecord.topics.add(input.take(32));
        }
        return eventRecord;
    }
}