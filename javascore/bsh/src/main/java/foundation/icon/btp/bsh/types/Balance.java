package foundation.icon.btp.bsh.types;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Balance {
    private BigInteger usable;
    private BigInteger locked;
    private BigInteger refundable;

    public Balance(BigInteger usable, BigInteger locked, BigInteger refundable) {
        this.usable = usable;
        this.locked = locked;
        this.refundable = refundable;
    }


    public static void writeObject(ObjectWriter w, Balance v) {
        w.beginList(3);
        w.write( v.getUsable());
        w.write( v.getLocked());
        w.write( v.getRefundable());
        w.end();
    }

    public static Balance readObject(ObjectReader r) {
        r.beginList();
        Balance result= new Balance(r.readBigInteger(),r.readBigInteger(),r.readBigInteger());
        r.end();
        return result;
    }

    public BigInteger getUsable() {
        return usable;
    }

    public void setUsable(BigInteger usable) {
        this.usable = usable;
    }

    public BigInteger getLocked() {
        return locked;
    }

    public void setLocked(BigInteger locked) {
        this.locked = locked;
    }

    public BigInteger getRefundable() {
        return refundable;
    }

    public void setRefundable(BigInteger refundable) {
        this.refundable = refundable;
    }
}
