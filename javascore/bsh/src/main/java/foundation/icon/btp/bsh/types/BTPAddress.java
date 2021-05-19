package foundation.icon.btp.bsh.types;

import score.ObjectReader;
import score.ObjectWriter;

public class BTPAddress {

    private String protocol;
    private String net;
    private String contract;

    public BTPAddress(String protocol, String net, String contract) {
        this.protocol = protocol;
        this.net = net;
        this.contract = contract;

        String nid = net.substring(0, net.lastIndexOf("."));
        String chain = net.substring(net.lastIndexOf(".") + 1);
    }


    public static void writeObject(ObjectWriter w, BTPAddress v) {
        w.beginList(3);
        w.write( v.getProtocol());
        w.write( v.getNet());
        w.write( v.getContract());
        w.end();
    }

    public static BTPAddress readObject(ObjectReader r) {
        r.beginList();
        BTPAddress result= new BTPAddress(r.readString(),r.readString(),r.readString());
        r.end();
        return result;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getNet() {
        return net;
    }

    public String getContract() {
        return contract;
    }

    public static BTPAddress fromString(String addr) {
        // TODO use scorex tokenizer - requires rebuild of the goloop docker image to include the Jar
        String protocol = addr.substring(0, addr.lastIndexOf("://"));
        String net = addr.substring(protocol.length() + 3, addr.lastIndexOf("/"));
        int offset = protocol.length() + net.length() + 4;
        String contract = "";
        if (addr.length() > offset)
            contract = addr.substring(offset, addr.length());
        return new BTPAddress(protocol, net, contract);
    }
}
