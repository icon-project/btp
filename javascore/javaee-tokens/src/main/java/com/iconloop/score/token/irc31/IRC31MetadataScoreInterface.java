package com.iconloop.score.token.irc31;

import score.Address;
import score.Context;

import java.math.BigInteger;

public final class IRC31MetadataScoreInterface implements IRC31Metadata {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public IRC31MetadataScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public IRC31MetadataScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public IRC31MetadataScoreInterface _payable(BigInteger valueForPayable) {
    return new IRC31MetadataScoreInterface(address,valueForPayable);
  }

  public IRC31MetadataScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public String tokenURI(BigInteger _id) {
    return Context.call(String.class, this.address, "tokenURI", _id);
  }
}
