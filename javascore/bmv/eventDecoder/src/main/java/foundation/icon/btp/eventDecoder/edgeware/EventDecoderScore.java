package foundation.icon.btp.eventdecoder.edgeware;

import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.ObjectReader;
import score.ByteArrayObjectWriter;

import scorex.util.Base64;
import scorex.util.ArrayList;

import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import foundation.icon.btp.lib.eventdecoder.EventRecord;
import foundation.icon.btp.lib.eventdecoder.EventDecoder;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;

import foundation.icon.btp.lib.utils.HexConverter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class EventDecoderScore {
  public EventDecoderScore() {}

  /**
   * decode edgeware event
   */
  @External(readonly=true)
  public List<EventRecord> decodeEvent(byte[] encodedEventList) {
    ByteSliceInput input = new ByteSliceInput(encodedEventList);
    int eventSize = ScaleReader.readUintCompactSize(input);
    List<EventRecord> result = new ArrayList<EventRecord>(eventSize);
    for (int i = 0; i<eventSize; i++) {
        result.add(EventDecoder.decode(input));
    }
    return result;
  }
}
