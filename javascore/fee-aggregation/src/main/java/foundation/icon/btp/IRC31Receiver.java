package foundation.icon.btp;

import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class IRC31Receiver {

    public IRC31Receiver() {}

    /**
     *  A method for handling a single token type transfer, which is called from the multi token contract.
     *  It works by analogy with the fallback method of the normal transactions and returns nothing.
     *  @throws  if it rejects the transfer.
     *
     *  @param _operator: The address which initiated the transfer
     *  @param _from: the address which previously owned the token
     *  @param _id: the ID of the token being transferred
     *  @param _value: the amount of tokens being transferred
     *  @param _data: additional data with no specified format
     */
    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        IRC31Received(_operator, _from, _id, _value, _data);
    }

    /**
     *  A method for handling multiple token type transfers, which is called from the multi token contract.
     *  It works by analogy with the fallback method of the normal transactions and returns nothing.
     *  @throws if it rejects the transfer.
     *
     *  @param _operator: The address which initiated the transfer
     *  @param _from: the address which previously owned the token
     *  @param _ids: the list of IDs of each token being transferred (order and length must match `_values` list)
     *  @param _values: the list of amounts of each token being transferred (order and length must match `_ids` list)
     *  @param _data: additional data with no specified format
     */
    @External
    public void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids, BigInteger[] _values, byte[] _data) {
        for (int i = 0; i < _ids.length; i++) {
            IRC31Received(_operator, _from, _ids[i], _values[i], _data);
        }
    }

    @EventLog(indexed=3)
    protected void IRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {};
}
