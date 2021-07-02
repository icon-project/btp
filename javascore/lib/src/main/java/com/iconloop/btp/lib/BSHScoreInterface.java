package com.iconloop.btp.lib;

import score.Address;
import score.Context;

import java.math.BigInteger;

public final class BSHScoreInterface implements BSH {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public BSHScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public BSHScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public BSHScoreInterface _payable(BigInteger valueForPayable) {
    return new BSHScoreInterface(address,valueForPayable);
  }

  public BSHScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public void handleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) {
    Context.call(this.address, "handleBTPMessage", _from, _svc, _sn, _msg);
  }

  @Override
  public void handleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) {
    Context.call(this.address, "handleBTPError", _src, _svc, _sn, _code, _msg);
  }

  @Override
  public void handleFeeGathering(String _fa, String _svc) {
    Context.call(this.address, "handleFeeGathering", _fa, _svc);
  }
}
