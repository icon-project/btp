package foundation.icon.btp.bmv.types;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

//TODO: convert the indexed to parse signature and recover the logs values message.py:367
public class ReceiptEventLog {

    final static String RLPn = "RLPn";

    private final byte[] address;
    private final List<byte[]> topics;
    private final byte[] data;

    public ReceiptEventLog(byte[] address, List<byte[]> topics, byte[] data) {
        this.address = address;
        this.topics = topics;
        this.data = data;
    }

    public static ReceiptEventLog fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;

        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();
        //TODO: remove later
        if (!reader.hasNext())
            return new ReceiptEventLog(null, new ArrayList<>(), new byte[]{0});
        //address
        byte[] address = reader.readByteArray();
        //indexed
        List<byte[]> topics = readByteArrayListFromRLP(reader);
        //data
        byte[] data = reader.readByteArray();
        reader.end();

        return new ReceiptEventLog(address, topics, data);
    }


    public static ReceiptEventLog readObject(ObjectReader reader) {
        reader.beginList();
        if (!reader.hasNext())
            return null;

        //address
        byte[] address = reader.readByteArray();
        //topics
        List<byte[]> topics = readByteArrayListFromRLP(reader);
        //data
        byte[] data = reader.readByteArray();
        reader.end();

        return new ReceiptEventLog(address, topics, data);
    }


    public static List<byte[]> readByteArrayListFromRLP(ObjectReader reader) {
        reader.beginList();
        List<byte[]> lists = new ArrayList<>();
        if (!reader.hasNext())
            return lists;

        while (reader.hasNext()) {
            lists.add(reader.readByteArray());
        }
        reader.end();

        return lists;
    }

    public byte[] getAddress() {
        return address;
    }

    public List<byte[]> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }
}
