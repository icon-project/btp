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

package foundation.icon.btp.lib;

import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.Map;

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
  public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
    return Context.call(byte[][].class, this.address, "handleRelayMessage", _bmc, _prev, _seq, _msg);
  }

  @Override
  public Map getStatus() {
    return Context.call(Map.class, this.address, "getStatus");
  }

  public <T> T getStatus(Class<T> clazz) {
    return Context.call(clazz, this.address, "getStatus");
  }
}
