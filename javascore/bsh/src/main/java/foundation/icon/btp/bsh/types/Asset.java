package foundation.icon.btp.bsh.types;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Asset {
    private String name;
    private BigInteger value;
    private BigInteger fee;

    public Asset(String name, BigInteger value, BigInteger fee) {
        this.name = name;
        this.value = value;
        this.fee = fee;
    }
    public static void writeObject(ObjectWriter w, Asset v) {
        w.beginList(3);
        w.write( v.getName());
        w.write( v.getValue());
        w.write( v.getFee());
        w.end();
    }

    public static Asset readObject(ObjectReader r) {
        r.beginList();
        Asset result= new Asset(r.readString(),r.readBigInteger(),r.readBigInteger());
        r.end();
        return result;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }
}
