package com.iconloop.btp.lib;

import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public final class BMCScoreInterface implements BMC {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public BMCScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public BMCScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public BMCScoreInterface _payable(BigInteger valueForPayable) {
    return new BMCScoreInterface(address,valueForPayable);
  }

  public BMCScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public void addVerifier(String _net, Address _addr) {
    Context.call(this.address, "addVerifier", _net, _addr);
  }

  @Override
  public void removeVerifier(String _net) {
    Context.call(this.address, "removeVerifier", _net);
  }

  @Override
  public Map getVerifiers() {
    return Context.call(Map.class, this.address, "getVerifiers");
  }

  @Override
  public void addService(String _svc, Address _addr) {
    Context.call(this.address, "addService", _svc, _addr);
  }

  @Override
  public void removeService(String _svc) {
    Context.call(this.address, "removeService", _svc);
  }

  @Override
  public Map getServices() {
    return Context.call(Map.class, this.address, "getServices");
  }

  @Override
  public void addLink(String _link) {
    Context.call(this.address, "addLink", _link);
  }

  @Override
  public void removeLink(String _link) {
    Context.call(this.address, "removeLink", _link);
  }

  @Override
  public BMCStatus getStatus(String _link) {
    return Context.call(BMCStatus.class, this.address, "getStatus", _link);
  }

  @Override
  public String[] getLinks() {
    return Context.call(String[].class, this.address, "getLinks");
  }

  @Override
  public void addRoute(String _dst, String _link) {
    Context.call(this.address, "addRoute", _dst, _link);
  }

  @Override
  public void removeRoute(String _dst) {
    Context.call(this.address, "removeRoute", _dst);
  }

  @Override
  public Map getRoutes() {
    return Context.call(Map.class, this.address, "getRoutes");
  }

  @Override
  public void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
    Context.call(this.address, "sendMessage", _to, _svc, _sn, _msg);
  }

  @Override
  public void handleRelayMessage(String _prev, String _msg) {
    Context.call(this.address, "handleRelayMessage", _prev, _msg);
  }

  @Override
  public String getBtpAddress() {
    return Context.call(String.class, this.address, "getBtpAddress");
  }
}
