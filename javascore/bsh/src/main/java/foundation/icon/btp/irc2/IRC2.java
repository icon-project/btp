package foundation.icon.btp.irc2;
import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;

public interface IRC2 {
    /**
     * Returns the name of the token. (e.g. "MySampleToken")
     */
    String name();

    /**
     * Returns the symbol of the token. (e.g. "MST")
     */
    String symbol();

    /**
     * Returns the number of decimals the token uses. (e.g. 18)
     */
    BigInteger decimals();

    /**
     * Returns the total token supply.
     */
    BigInteger totalSupply();

    /**
     * Returns the account balance of another account with address {@code _owner}.
     */
    BigInteger balanceOf(Address _owner);

    /**
     * Transfers {@code _value} amount of tokens to address {@code _to}, and MUST fire the {@code Transfer} event.
     * This function SHOULD throw if the caller account balance does not have enough tokens to spend.
     * If {@code _to} is a contract, this function MUST invoke the function {@code tokenFallback(Address, int, bytes)}
     * in {@code _to}. If the {@code tokenFallback} function is not implemented in {@code _to} (receiver contract),
     * then the transaction must fail and the transfer of tokens should not occur.
     * If {@code _to} is an externally owned address, then the transaction must be sent without trying to execute
     * {@code tokenFallback} in {@code _to}. {@code _data} can be attached to this token transaction.
     * {@code _data} can be empty.
     */
    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);

    /**
     * (EventLog) Must trigger on any successful token transfers.
     */
    void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data);
}
