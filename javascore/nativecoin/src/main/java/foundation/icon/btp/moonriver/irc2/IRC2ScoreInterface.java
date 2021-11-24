package foundation.icon.btp.moonriver.irc2;

import score.Address;
import score.Context;

import java.math.BigInteger;

public class IRC2ScoreInterface {
    protected final Address address;

    public IRC2ScoreInterface(Address address) {
        this.address = address;
    }

    public Address _address() {
        return this.address;
    }

    public Address _check() {
        return this.address;
    }

    public BigInteger _balanceOf(Address _owner) {
        return (BigInteger)Context.call( this.address, "balanceOf", _owner);
        //return BigInteger.valueOf(1);
    }

    public void transfer(Address _to, BigInteger _value, byte[] _data) {
        Context.call(this.address, "transfer", _to, _value, _data);
    }

    public void _Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
        throw new RuntimeException("not supported EventLog method");
    }

    public String _name() {
        return (String)Context.call(this.address, "name");
    }

    public String _symbol() {
        return (String)Context.call(this.address, "symbol");
    }

    public int _decimals() {
        return (Integer)Context.call(this.address, "decimals");
    }

    public BigInteger _totalSupply() {
        return Context.call(BigInteger.class, this.address, "totalSupply");
    }

}
