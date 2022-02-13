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

package foundation.icon.btp.nativecoin.irc2;

import score.Address;
import score.Context;
import score.annotation.Optional;

import java.math.BigInteger;

public final class IRC2SupplierScoreInterface implements IRC2Supplier {
  protected final Address address;

  public IRC2SupplierScoreInterface(Address address) {
    this.address = address;
  }

  public Address _address() {
    return this.address;
  }

  public String name() {
    return Context.call(String.class, this.address, "name");
  }
  
  public String symbol() {
    return Context.call(String.class, this.address, "symbol");
  }

  public BigInteger decimals() {
    return Context.call(BigInteger.class, this.address, "symbol");
  }

  public BigInteger totalSupply() {
    return Context.call(BigInteger.class, this.address, "totalSupply");
  }

  public BigInteger balanceOf(Address _owner) {
    return Context.call(BigInteger.class, this.address, "balanceOf", _owner);
  }

  public BigInteger allowance(Address owner, Address spender) {
    return Context.call(BigInteger.class, this.address, "allowance", owner, spender);
  }

  public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
    Context.call(this.address, "transfer", _to, _value, _data);
  }

  public void burn(BigInteger _amount) {
    Context.call(this.address, "burn", _amount);
  }

  public void mint(Address _to, BigInteger _amount) {
    Context.call(this.address, "mint", _to, _amount);
  }

  public void setMinter(Address _minter) {
    Context.call(this.address, "setMinter", _minter);
  }

  public boolean approve(Address spender, BigInteger amount) {
    return Context.call(Boolean.class, this.address, "approve", spender, amount);
  }

  public boolean transferFrom(
        Address _from,
        Address _to,
        BigInteger _value,
        @Optional byte[] _data
  ) {
    return Context.call(Boolean.class, this.address, "transferFrom", _from, _to, _value, _data);
  }

  public boolean increaseAllowance(Address spender, BigInteger addedValue) {
    return Context.call(Boolean.class, this.address, "increaseAllowance", spender, addedValue);
  }

  public boolean decreaseAllowance(Address spender, BigInteger subtractedValue) {
    return Context.call(Boolean.class, this.address, "decreaseAllowance", spender, subtractedValue);
  }

  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    throw new RuntimeException("not supported EventLog method");
  }
  
  /**
   * @deprecated Do not use this method, this is generated only for preventing compile error. not supported EventLog method
   * @throws RuntimeException
   */
  @Deprecated
  public void Approval(Address owner, Address spender, BigInteger value) {
    throw new RuntimeException("not supported EventLog method");
  }
}
