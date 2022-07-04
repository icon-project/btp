package foundation.icon.btp.bmv.bridge;

import score.ObjectReader;

import java.math.BigInteger;

public class EventDataBTPMessage {

    final static String RLPn = "RLPn";
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
}
