package foundation.icon.btp.nativecoinIRC2.irc2;

import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public class IRC2Basic implements IRC2 {
    protected static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);
    private final String name;
    private final String symbol;
    private final int decimals;
    private BigInteger totalSupply = BigInteger.ZERO;
    private final DictDB<Address, BigInteger> balances = Context.newDictDB("balances", BigInteger.class);

    public IRC2Basic(String _name, String _symbol, int _decimals, BigInteger _initialSupply) {
        this.name = _name;
        this.symbol = _symbol;
        this.decimals = _decimals;

        // decimals must be larger than 0 and less than 21
        Context.require(this.decimals >= 0);
        Context.require(this.decimals <= 21);
        Context.require(_initialSupply.compareTo(BigInteger.ZERO) >= 0);
        _mint(Context.getCaller(), _initialSupply.multiply(pow10(_decimals)));
    }

    private static BigInteger pow10(int exponent) {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(BigInteger.TEN);
        }
        return result;
    }

    @External(readonly=true)
    public String name() {
        return name;
    }

    @External(readonly=true)
    public String symbol() {
        return symbol;
    }

    @External(readonly=true)
    public int decimals() {
        return decimals;
    }

    @External(readonly=true)
    public BigInteger totalSupply() {
        return totalSupply;
    }

    @External(readonly=true)
    public BigInteger balanceOf(Address _owner) {
        return safeGetBalance(_owner);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Address _from = Context.getCaller();

        // check some basic requirements
        Context.require(_value.compareTo(BigInteger.ZERO) >= 0);
        Context.require(safeGetBalance(_from).compareTo(_value) >= 0);

        // adjust the balances
        safeSetBalance(_from, safeGetBalance(_from).subtract(_value));
        safeSetBalance(_to, safeGetBalance(_to).add(_value));

        // if the recipient is SCORE, call 'tokenFallback' to handle further operation
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", _from, _value, dataBytes);
        }

        // emit Transfer event
        Transfer(_from, _to, _value, dataBytes);
    }

    /**
     * Creates `amount` tokens and assigns them to `owner`, increasing the total supply.
     */
    protected void _mint(Address owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner));
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0);

        this.totalSupply = this.totalSupply.add(amount);
        safeSetBalance(owner, safeGetBalance(owner).add(amount));
        Transfer(ZERO_ADDRESS, owner, amount, "mint".getBytes());
    }

    /**
     * Destroys `amount` tokens from `owner`, reducing the total supply.
     */
    protected void _burn(Address owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner));
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0);
        Context.require(safeGetBalance(owner).compareTo(amount) >= 0);

        safeSetBalance(owner, safeGetBalance(owner).subtract(amount));
        this.totalSupply = this.totalSupply.subtract(amount);
        Transfer(owner, ZERO_ADDRESS, amount, "burn".getBytes());
    }

    private BigInteger safeGetBalance(Address owner) {
        return balances.getOrDefault(owner, BigInteger.ZERO);
    }

    private void safeSetBalance(Address owner, BigInteger amount) {
        balances.set(owner, amount);
    }

    @EventLog(indexed=3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}
}
