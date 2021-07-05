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

import java.math.BigInteger;

public final class IRC31ReceiverScoreInterface implements IRC31Receiver {
  protected final Address address;

  protected final BigInteger valueForPayable;

  public IRC31ReceiverScoreInterface(Address address) {
    this.address = address;
    this.valueForPayable = null;
  }

  public IRC31ReceiverScoreInterface(Address address, BigInteger valueForPayable) {
    this.address = address;
    this.valueForPayable = valueForPayable;
  }

  public Address _getAddress() {
    return this.address;
  }

  public IRC31ReceiverScoreInterface _payable(BigInteger valueForPayable) {
    return new IRC31ReceiverScoreInterface(address,valueForPayable);
  }

  public IRC31ReceiverScoreInterface _payable(long valueForPayable) {
    return this._payable(BigInteger.valueOf(valueForPayable));
  }

  public BigInteger _getICX() {
    return this.valueForPayable;
  }

  @Override
  public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value,
      byte[] _data) {
    Context.call(this.address, "onIRC31Received", _operator, _from, _id, _value, _data);
  }

  @Override
  public void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids,
      BigInteger[] _values, byte[] _data) {
    Context.call(this.address, "onIRC31BatchReceived", _operator, _from, _ids, _values, _data);
  }
}
