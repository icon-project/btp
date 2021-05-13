package foundation.icon.btp.lib.btpaddress;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BTPAddress {
    private String protocol;
    private String chain;
    private String address;
    private String nid;

    public BTPAddress(String protocol, String net, String address) {
        this.protocol = protocol;
        
        if (net != null) {
            int idx = net.indexOf(".");
            if (idx < 0) {
                throw new AssertionError("fail to parse net address");
            }

            this.nid = net.substring(0, idx);
            this.chain = net.substring(idx + 1);
        }

        this.address = address;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getChain() {
        return this.chain;
    }

    public String getAddress() {
        return this.address;
    }

    public String getNid() {
        return this.nid;
    }

    public String getNet() {
        return this.nid + "." + this.chain;
    }

    public boolean isValid() {
        return this.protocol != null && this.nid != null && this.address != null && this.chain != null;
    }

    public static BTPAddress fromString(String rawString) {
        if (rawString == null) {
            return null;
        }

        char[] rawCharArray = rawString.toCharArray();

        String protocol = null;
        String net = null;
        String address = null;
        for (int i = 0; i < rawCharArray.length; i++) {
            if (rawCharArray[i] == ':' && rawCharArray[i+1] == '/' && rawCharArray[i+2] == '/' && protocol == null) {
                protocol = rawString.substring(0, i);
                i = i+2;
            } else if (rawCharArray[i] == '/' && net == null) {
                net = rawString.substring(protocol.length() + 3, i);
                address = rawString.substring(protocol.length() + 4 + net.length());
            } 
        }

        return new BTPAddress(protocol, net, address);
    }

    public String toString() {
        return this.protocol + "://" + this.nid + "." + this.chain + "/" + this.address;
    }
}
