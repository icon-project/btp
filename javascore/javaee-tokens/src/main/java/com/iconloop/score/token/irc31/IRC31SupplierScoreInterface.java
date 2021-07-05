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

import score.Address;
import score.Context;
import score.annotation.Optional;

import java.math.BigInteger;

public final class IRC31SupplierScoreInterface implements IRC31Supplier {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public IRC31SupplierScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public IRC31SupplierScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public IRC31SupplierScoreInterface _payable(BigInteger valueForPayable) {
    return new IRC31SupplierScoreInterface(address,valueForPayable);
  }

  public IRC31SupplierScoreInterface _payable(long valueForPayable) {
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
    return Context.call(Boolean.class, this.address, "isApprovedForAll", _owner, _operator);
  }

  @Override
  public String tokenURI(BigInteger _id) {
    return Context.call(String.class, this.address, "tokenURI", _id);
  }

  @Override
  public BigInteger totalSupply(BigInteger _id) {
    return Context.call(BigInteger.class, this.address, "totalSupply", _id);
  }

  @Override
  public void mint(Address _owner, BigInteger _id, BigInteger _amount) {
    Context.call(this.address, "mint", _owner, _id, _amount);
  }

  @Override
  public void mintBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
    Context.call(this.address, "mintBatch", _owner, _ids, _amounts);
  }

  @Override
  public void burn(Address _owner, BigInteger _id, BigInteger _amount) {
    Context.call(this.address, "burn", _owner, _id, _amount);
  }

  @Override
  public void burnBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
    Context.call(this.address, "burnBatch", _owner, _ids, _amounts);
  }

  @Override
  public void setTokenURI(BigInteger _id, String _uri) {
    Context.call(this.address, "setTokenURI", _id, _uri);
  }
}
