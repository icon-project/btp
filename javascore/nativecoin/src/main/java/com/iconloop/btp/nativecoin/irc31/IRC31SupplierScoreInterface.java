package com.iconloop.btp.nativecoin.irc31;

import score.Address;
import score.Context;
import score.annotation.Optional;

import java.math.BigInteger;

public final class IRC31SupplierScoreInterface implements IRC31Supplier {
  protected final Address address;

  public IRC31SupplierScoreInterface(Address address) {
    this.address = address;
  }

  public Address _address() {
    return this.address;
  }

  public BigInteger balanceOf(Address _owner, BigInteger _id) {
    return Context.call(BigInteger.class, this.address, "balanceOf", _owner, _id);
  }

  public BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids) {
    return Context.call(BigInteger[].class, this.address, "balanceOfBatch", _owners, _ids);
  }

  public String tokenURI(BigInteger _id) {
    return Context.call(String.class, this.address, "tokenURI", _id);
  }

  public void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value,
      @Optional byte[] _data) {
    Context.call(this.address, "transferFrom", _from, _to, _id, _value, _data);
  }

  public void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values,
      @Optional byte[] _data) {
    Context.call(this.address, "transferFromBatch", _from, _to, _ids, _values, _data);
  }

  public void setApprovalForAll(Address _operator, boolean _approved) {
    Context.call(this.address, "setApprovalForAll", _operator, _approved);
  }

  public boolean isApprovedForAll(Address _owner, Address _operator) {
    return Context.call(boolean.class, this.address, "isApprovedForAll", _owner, _operator);
  }

  public BigInteger totalSupply(BigInteger _id) {
    return Context.call(BigInteger.class, this.address, "totalSupply", _id);
  }

  public void mint(Address _owner, BigInteger _id, BigInteger _amount) {
    Context.call(this.address, "mint", _owner, _id, _amount);
  }

  public void mintBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
    Context.call(this.address, "mintBatch", _owner, _ids, _amounts);
  }

  public void burn(Address _owner, BigInteger _id, BigInteger _amount) {
    Context.call(this.address, "burn", _owner, _id, _amount);
  }

  public void burnBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
    Context.call(this.address, "burnBatch", _owner, _ids, _amounts);
  }

  public void setTokenURI(BigInteger _id, String _uri) {
    Context.call(this.address, "setTokenURI", _id, _uri);
  }

  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id,
      BigInteger _value) {
    throw new RuntimeException("not supported EventLog method");
  }

  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids,
      byte[] _values) {
    throw new RuntimeException("not supported EventLog method");
  }

  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void ApprovalForAll(Address _owner, Address _operator, boolean _approved) {
    throw new RuntimeException("not supported EventLog method");
  }

  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void URI(BigInteger _id, String _value) {
    throw new RuntimeException("not supported EventLog method");
  }
}
