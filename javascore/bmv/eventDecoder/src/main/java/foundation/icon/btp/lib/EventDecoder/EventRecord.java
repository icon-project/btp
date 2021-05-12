package foundation.icon.btp.lib.eventdecoder;
import java.util.List;

public class EventRecord {
    public byte phaseEnum;
    public byte[] phaseData;
    public byte[] eventIndex;
    public byte[] eventData;
    public List<byte[]> topics;
}