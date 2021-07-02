package com.iconloop.score.token.irc31;

import score.Address;
import score.Context;
import score.annotation.Optional;

import java.math.BigInteger;

public final class IRC31ScoreInterface implements IRC31 {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public IRC31ScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public IRC31ScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public IRC31ScoreInterface _payable(BigInteger valueForPayable) {
    return new IRC31ScoreInterface(address,valueForPayable);
  }

  public IRC31ScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public BigInteger balanceOf(Address _owner, BigInteger _id) {
    return Context.call(BigInteger.class, this.address, "balanceOf", _owner, _id);
  }

  @Override
  public BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids) {
    return Context.call(BigInteger[].class, this.address, "balanceOfBatch", _owners, _ids);
  }

  @Override
  public void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value,
      @Optional byte[] _data) {
    Context.call(this.address, "transferFrom", _from, _to, _id, _value, _data);
  }

  @Override
  public void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values,
      @Optional byte[] _data) {
    Context.call(this.address, "transferFromBatch", _from, _to, _ids, _values, _data);
  }

  @Override
  public void setApprovalForAll(Address _operator, boolean _approved) {
    Context.call(this.address, "setApprovalForAll", _operator, _approved);
  }

  @Override
  public boolean isApprovedForAll(Address _owner, Address _operator) {
    return Context.call(boolean.class, this.address, "isApprovedForAll", _owner, _operator);
  }
}
