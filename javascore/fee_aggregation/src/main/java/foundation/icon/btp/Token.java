package foundation.icon.btp;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Token {
    private static final BigInteger BIGINT_NO_EXISTED_IN_TOKEN_ID = BigInteger.ZERO;
    private String name;
    private Address address;
    /**
     *  Default value is BIGINT_NO_EXISTED_IN_TOKEN_ID, mean this token not exist tokenId
     */
    private BigInteger tokenId;

    public Token(String _name, Address _address) {
        this.name = _name;
        this.address = _address;
        this.tokenId = BIGINT_NO_EXISTED_IN_TOKEN_ID;
    }
    public Token(String _name, Address _address, BigInteger _tokenId) {
        if (_tokenId != null) {
            this.tokenId = _tokenId;
        }
        this.name = _name;
        this.address = _address;
    }

    public String name() {
        return this.name;
    }

    public Address address() {
        return this.address;
    }

    public BigInteger tokenId() {
        return this.tokenId;
    }

    public Boolean isIRC31() {
        return !this.tokenId.equals(BIGINT_NO_EXISTED_IN_TOKEN_ID);
    }

    public static void writeObject(ObjectWriter w, Token t) {
        w.beginList(3);
        w.write(t.name);
        w.write(t.address);
        w.writeNullable(t.tokenId);
        w.end();
    }

    public static Token readObject(ObjectReader r) {
        r.beginList();
        Token t = new Token(
                r.readString(),
                r.readAddress(),
                r.readNullable(BigInteger.class));
        r.end();
        return t;
    }
}