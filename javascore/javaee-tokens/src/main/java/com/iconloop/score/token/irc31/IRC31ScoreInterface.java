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
    return Context.call(Boolean.class, this.address, "isApprovedForAll", _owner, _operator);
  }
}
