package foundation.icon.btp.lib.eventdecoder;
import java.util.List;
import java.util.Map;
import scorex.util.HashMap;

public class EventRecord {
    public byte phaseEnum;
    public byte[] phaseData;
    public byte[] eventIndex;
    public byte[] eventData;
    public List<byte[]> topics;

    public Map<String, Object> toMap() {
        Map<String, Object> eventRecordMap = new HashMap<String, Object>();
        eventRecordMap.put("phaseEnum", this.phaseEnum);
        eventRecordMap.put("phaseData", this.phaseData);
        eventRecordMap.put("eventIndex", this.eventIndex);
        eventRecordMap.put("eventData", this.eventData);
        eventRecordMap.put("topics", this.topics);
        return eventRecordMap;
    }

}