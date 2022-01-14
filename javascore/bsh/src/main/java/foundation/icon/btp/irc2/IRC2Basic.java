package foundation.icon.btp.irc2;

import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public class IRC2Basic implements IRC2 {
    protected static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);
    private final VarDB<String> name = Context.newVarDB("token_name", String.class);
    private final VarDB<String> symbol = Context.newVarDB("token_symbol", String.class);
    private final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);
    private final VarDB<BigInteger> totalSupply = Context.newVarDB("total_supply", BigInteger.class);
    private final DictDB<Address, BigInteger> balances = Context.newDictDB("balances", BigInteger.class);

    public IRC2Basic(String _name, String _symbol, int _decimals, BigInteger _initialSupply) {
        // initialize values only at first deployment
        if (this.name.get() == null) {
            this.name.set(ensureNotEmpty(_name));
            this.symbol.set(ensureNotEmpty(_symbol));

            // decimals must be larger than 0 and less than 21
            Context.require(_decimals >= 0, "decimals needs to be positive");
            Context.require(_decimals <= 21, "decimals needs to be equal or lower than 21");
            this.decimals.set(BigInteger.valueOf(_decimals));
        }

        Context.require(_initialSupply.compareTo(BigInteger.ZERO) >= 0, "initial supply needs to be positive");
        _mint(Context.getCaller(), _initialSupply.multiply(pow10(_decimals)));
    }

    private String ensureNotEmpty(String str) {
        Context.require(str != null && !str.trim().isEmpty(), "str is null or empty");
        assert str != null;
        return str.trim();
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
        return name.get();
    }

    @External(readonly=true)
    public String symbol() {
        return symbol.get();
    }

    @External(readonly=true)
    public BigInteger decimals() {
        return decimals.get();
    }

    @External(readonly=true)
    public BigInteger totalSupply() {
        return totalSupply.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger balanceOf(Address _owner) {
        return safeGetBalance(_owner);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Address _from = Context.getCaller();

        // check some basic requirements
        Context.require(_value.compareTo(BigInteger.ZERO) >= 0, "_value needs to be positive");
        Context.require(safeGetBalance(_from).compareTo(_value) >= 0, "Insufficient balance");

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
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");

        totalSupply.set(totalSupply.getOrDefault(BigInteger.ZERO).add(amount));
        safeSetBalance(owner, safeGetBalance(owner).add(amount));
        Transfer(ZERO_ADDRESS, owner, amount, "mint".getBytes());
    }

    /**
     * Destroys `amount` tokens from `owner`, reducing the total supply.
     */
    protected void _burn(Address owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");
        Context.require(safeGetBalance(owner).compareTo(amount) >= 0, "Insufficient balance");

        safeSetBalance(owner, safeGetBalance(owner).subtract(amount));
        totalSupply.set(totalSupply.getOrDefault(BigInteger.ZERO).subtract(amount));
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
