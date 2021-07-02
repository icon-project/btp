package com.iconloop.btp.lib;

import score.Address;
import score.Context;

import java.math.BigInteger;

public final class BMVScoreInterface implements BMV {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public BMVScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public BMVScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public BMVScoreInterface _payable(BigInteger valueForPayable) {
    return new BMVScoreInterface(address,valueForPayable);
  }

  public BMVScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
    return Context.call(byte[][].class, this.address, "handleRelayMessage", _bmc, _prev, _seq, _msg);
  }

  @Override
  public BMVStatus getStatus() {
    return Context.call(BMVStatus.class, this.address, "getStatus");
  }
}
