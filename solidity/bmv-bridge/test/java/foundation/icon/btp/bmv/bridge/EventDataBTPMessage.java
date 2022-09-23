package foundation.icon.btp.bmv.bridge;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;

public class EventDataBTPMessage {
    private final String next_bmc;
    private final BigInteger seq;
    private final byte[] msg;

    public EventDataBTPMessage(String next_bmc, BigInteger seq, byte[] msg) {
        this.next_bmc = next_bmc;
        this.seq = seq;
        this.msg = msg;
    }

    /**
     * Method to extract raw data directly from the reader without the TypeDecoder
     * @param reader
     * @return
     */
    public static EventDataBTPMessage fromRLPBytes(ObjectReader reader) {
        reader.beginList();
        String _nxt_bmc = reader.readString();
        BigInteger _seq = reader.readBigInteger();
        byte[] _msg = reader.readByteArray();
        reader.end();
        return new EventDataBTPMessage(_nxt_bmc, _seq, _msg);
    }

    public String getNext_bmc() {
        return next_bmc;
    }

    public BigInteger getSeq() {
        return seq;
    }

    public byte[] getMsg() {
        return msg;
    }

    public static EventDataBTPMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return EventDataBTPMessage.fromRLPBytes(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(1);
        writer.write(next_bmc);
        writer.write(seq);
        writer.write(msg);
        writer.end();
        return writer.toByteArray();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventDataBTPMessage{");
        sb.append("next_bmc='").append(next_bmc).append('\'');
        sb.append(", seq=").append(seq);
        sb.append(", msg=").append(StringUtil.toString(msg));
        sb.append('}');
        return sb.toString();
    }
}
