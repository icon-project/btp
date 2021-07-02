/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.token.irc31;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public abstract class AbstractIRC31 implements IRC31, IRC31Event, IRC31Metadata {
    protected static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    //id => (owner => balance)
    private final BranchDB<BigInteger, DictDB<Address, BigInteger>> balances = Context.newBranchDB("balances", BigInteger.class);
    //owner => (operator => approved)
    private final BranchDB<Address, DictDB<Address, Boolean>> operatorApproval = Context.newBranchDB("approval", Boolean.class);
    //id => token URI
    private final DictDB<BigInteger, String> tokenURIs = Context.newDictDB("token_uri", String.class);

    protected byte[] encode(BigInteger[] ids) {
        Context.require(ids != null);
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(ids.length);
        for(BigInteger v : ids) {
            writer.write(v);
        }
        writer.end();
        return writer.toByteArray();
    }

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner, BigInteger _id) {
        return balances.at(_id).get(_owner);
    }

    @External(readonly = true)
    public BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids) {
        Context.require(_owners.length == _ids.length);
        BigInteger[] balances = new BigInteger[_owners.length];
        for (int i = 0; i < _owners.length; i++) {
            balances[i] = this.balances.at(_ids[i]).get(_owners[i]);
        }
        return balances;
    }

    @External
    public void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value, @Optional byte[] _data) {
        //"_to must be non-zero"
        Context.require(!ZERO_ADDRESS.equals(_to));
        //"Need operator approval for 3rd party transfers"
        Address caller = Context.getCaller();
        Context.require(_from.equals(caller) || isApprovedForAll(_from, caller));
        //"Insufficient funds"
        Context.require(_value.compareTo(BigInteger.ZERO) > -1);

        DictDB<Address, BigInteger> dictDB = balances.at(_id);
        BigInteger balance = dictDB.getOrDefault(_from, BigInteger.ZERO);
        //"Insufficient funds"
        Context.require(_value.compareTo(balance) < 1);

        //transfer funds
        dictDB.set(_from, balance.subtract(_value));
        dictDB.set(_to, dictDB.getOrDefault(_to, BigInteger.ZERO).add(_value));

        //emit event
        TransferSingle(caller, _from, _to, _id, _value);

        if (_to.isContract()) {
            IRC31ReceiverScoreInterface irc31Receiver = new IRC31ReceiverScoreInterface(_to);
            irc31Receiver.onIRC31Received(caller, _from, _id, _value, _data == null ? new byte[]{} : _data);
        }
    }

    @External
    public void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values, @Optional byte[] _data) {
        //"_to must be non-zero"
        Context.require(!ZERO_ADDRESS.equals(_to));
        //"id/value pairs mismatch"
        Context.require(_ids.length == _values.length);
        //"Need operator approval for 3rd party transfers"
        Address caller = Context.getCaller();
        Context.require(_from.equals(caller) || isApprovedForAll(_from, caller));

        for (int i = 0; i < _ids.length; i++) {
            BigInteger id = _ids[i];
            BigInteger value = _values[i];
            //"Insufficient funds"
            Context.require(value.compareTo(BigInteger.ZERO) > -1);

            DictDB<Address, BigInteger> dictDB = balances.at(id);
            BigInteger balance = dictDB.getOrDefault(_from, BigInteger.ZERO);
            //"Insufficient funds"
            Context.require(value.compareTo(balance) < 1);

            //transfer funds
            dictDB.set(_from, balance.subtract(value));
            dictDB.set(_to, dictDB.getOrDefault(_to, BigInteger.ZERO).add(value));
        }

        //emit event
        TransferBatch(caller, _from, _to, encode(_ids), encode(_values));

        if (_to.isContract()) {
            IRC31ReceiverScoreInterface irc31Receiver = new IRC31ReceiverScoreInterface(_to);
            irc31Receiver.onIRC31BatchReceived(caller, _from, _ids, _values, _data == null ? new byte[]{} : _data);
        }
    }

    @External
    public void setApprovalForAll(Address _operator, boolean _approved) {
        Address caller = Context.getCaller();
        operatorApproval.at(caller).set(_operator, _approved);
        ApprovalForAll(caller, _operator, _approved);
    }

    @External(readonly = true)
    public boolean isApprovedForAll(Address _owner, Address _operator) {
        return operatorApproval.at(_owner).getOrDefault(_operator, false);
    }

    @EventLog(indexed = 3)
    public void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value) {

    }

    @EventLog(indexed = 3)
    public void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values) {

    }

    @EventLog(indexed = 2)
    public void ApprovalForAll(Address _owner, Address _operator, boolean _approved) {

    }

    @EventLog(indexed = 1)
    public void URI(BigInteger _id, String _value) {

    }

    @External(readonly = true)
    public String tokenURI(BigInteger _id) {
        return tokenURIs.get(_id);
    }

    protected void setTokenURI(BigInteger id, String uri) {
        tokenURIs.set(id, uri);
        URI(id, uri);
    }

    protected void mint(Address owner, BigInteger id, BigInteger amount) {
        //"invalid amount"
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        DictDB<Address, BigInteger> dictDB = balances.at(id);
        BigInteger balance = dictDB.getOrDefault(owner, BigInteger.ZERO);
        dictDB.set(owner, balance.add(amount));

        //emit transfer event for Mint semantic
        TransferSingle(owner, ZERO_ADDRESS, owner, id, amount);
    }

    protected void mintBatch(Address owner, BigInteger[] ids, BigInteger[] amounts) {
        //"id/amount pairs mismatch"
        Context.require(ids.length == amounts.length);

        for (int i = 0; i < ids.length; i++) {
            BigInteger id = ids[i];
            BigInteger amount = amounts[i];

            //"invalid amount"
            Context.require(amount.compareTo(BigInteger.ZERO) > 0);

            DictDB<Address, BigInteger> dictDB = balances.at(id);
            BigInteger balance = dictDB.getOrDefault(owner, BigInteger.ZERO);
            dictDB.set(owner, balance.add(amount));
        }

        //emit transfer event for Mint semantic
        TransferBatch(owner, ZERO_ADDRESS, owner, encode(ids), encode(amounts));
    }

    protected void burn(Address owner, BigInteger id, BigInteger amount) {
        //"invalid amount"
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        DictDB<Address, BigInteger> dictDB = balances.at(id);
        BigInteger balance = dictDB.get(owner);
        //"Not an owner"
        Context.require(balance != null);
        //"Insufficient funds"
        Context.require(amount.compareTo(balance) < 1);

        dictDB.set(owner, balance.subtract(amount));

        // emit transfer event for Burn semantic
        TransferSingle(owner, owner, ZERO_ADDRESS, id, amount);
    }

    protected void burnBatch(Address owner, BigInteger[] ids, BigInteger[] amounts) {
        //"id/amount pairs mismatch"
        Context.require(ids.length == amounts.length);

        for (int i = 0; i < ids.length; i++) {
            BigInteger id = ids[i];
            BigInteger amount = amounts[i];

            //"invalid amount"
            Context.require(amount.compareTo(BigInteger.ZERO) > 0);

            DictDB<Address, BigInteger> dictDB = balances.at(id);
            BigInteger balance = dictDB.get(owner);
            //"Not an owner"
            Context.require(balance != null);
            //"Insufficient funds"
            Context.require(amount.compareTo(balance) < 1);

            dictDB.set(owner, balance.subtract(amount));
        }

        // emit transfer event for Burn semantic
        TransferBatch(owner, owner, ZERO_ADDRESS, encode(ids), encode(amounts));
    }

}
