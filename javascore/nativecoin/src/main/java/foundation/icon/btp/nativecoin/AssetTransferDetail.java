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

package foundation.icon.btp.nativecoin;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class AssetTransferDetail extends Asset {
  private BigInteger fee;

  public BigInteger getFee() {
    return fee;
  }

  public void setFee(BigInteger fee) {
    this.fee = fee;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AssetTransferDetail{");
    sb.append("fee=").append(fee);
    sb.append('}').append(super.toString());
    return sb.toString();
  }

  public static void writeObject(ObjectWriter writer, AssetTransferDetail obj) {
    obj.writeObject(writer);
  }

  public static AssetTransferDetail readObject(ObjectReader reader) {
    AssetTransferDetail obj = new AssetTransferDetail();
    reader.beginList();
    obj.setCoinName(reader.readNullable(String.class));
    obj.setAmount(reader.readNullable(BigInteger.class));
    obj.setFee(reader.readNullable(BigInteger.class));
    reader.end();
    return obj;
  }

  public void writeObject(ObjectWriter writer) {
    writer.beginList(3);
    writer.writeNullable(this.getCoinName());
    writer.writeNullable(this.getAmount());
    writer.writeNullable(this.getFee());
    writer.end();
  }

  public static AssetTransferDetail fromBytes(byte[] bytes) {
    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
    return AssetTransferDetail.readObject(reader);
  }

  public byte[] toBytes() {
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    AssetTransferDetail.writeObject(writer, this);
    return writer.toByteArray();
  }

}
