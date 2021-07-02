package com.iconloop.btp.nativecoin;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Keep;

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
