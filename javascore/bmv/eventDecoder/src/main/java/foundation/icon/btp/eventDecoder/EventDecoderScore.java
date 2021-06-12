package foundation.icon.btp.eventdecoder;

import scorex.util.ArrayList;

import score.annotation.External;
import foundation.icon.btp.lib.eventdecoder.EventRecord;
import foundation.icon.btp.lib.eventdecoder.EventDecoder;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;

import java.util.List;
import java.util.Map;

public class EventDecoderScore {
  public EventDecoderScore() {}

  /**
   * decode edgeware event
   */
  @External(readonly=true)
  public List<Map<String, Object>> decodeEvent(byte[] encodedEventList) {
    ByteSliceInput input = new ByteSliceInput(encodedEventList);
    int eventSize = ScaleReader.readUintCompactSize(input);
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(eventSize);
    for (int i = 0; i<eventSize; i++) {
        result.add(EventDecoder.decode(input).toMap());
    }
    return result;
  }
}
