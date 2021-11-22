package foundation.icon.btp.bsh.types;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Token {
    private String name;
    private String symbol;
    private BigInteger decimals;
    private BigInteger feeNumerator;

    public Token(String name, String symbol, BigInteger decimals, BigInteger feeNumerator) {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.feeNumerator = feeNumerator;
    }


    public static void writeObject(ObjectWriter w, Token v) {
        w.beginList(4);
        w.write( v.getName());
        w.write( v.getSymbol());
        w.write( v.getDecimals());
        w.write( v.getFeeNumerator());
        w.end();
    }

    public static Token readObject(ObjectReader r) {
        r.beginList();
        Token result= new Token(r.readString(),r.readString(),r.readBigInteger(),r.readBigInteger());
        r.end();
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigInteger getDecimals() {
        return decimals;
    }

    public void setDecimals(BigInteger decimals) {
        this.decimals = decimals;
    }

    public BigInteger getFeeNumerator() {
        return feeNumerator;
    }

    public void setFeeNumerator(BigInteger feeNumerator) {
        this.feeNumerator = feeNumerator;
    }
}
