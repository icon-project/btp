package com.iconloop.score.util;

import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.List;

public final class OwnerManagerScoreInterface implements OwnerManager {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public OwnerManagerScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public OwnerManagerScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public OwnerManagerScoreInterface _payable(BigInteger valueForPayable) {
    return new OwnerManagerScoreInterface(address,valueForPayable);
  }

  public OwnerManagerScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public void addOwner(Address _addr) {
    Context.call(this.address, "addOwner", _addr);
  }

  @Override
  public void removeOwner(Address _addr) {
    Context.call(this.address, "removeOwner", _addr);
  }

  @Override
  public List getOwners() {
    return Context.call(List.class, this.address, "getOwners");
  }

  @Override
  public boolean isOwner(Address _addr) {
    return Context.call(boolean.class, this.address, "isOwner", _addr);
  }
}
