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

package com.iconloop.btp.nativecoin;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Asset {
    private String coinName;
    private BigInteger amount;

    public Asset() {}

    public Asset(Asset asset) {
        setCoinName(asset.getCoinName());
        setAmount(asset.getAmount());
    }

    public String getCoinName() {
        return coinName;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Asset{");
        sb.append("coinName='").append(coinName).append('\'');
        sb.append(", amount=").append(amount);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Asset obj) {
        obj.writeObject(writer);
    }

    public static Asset readObject(ObjectReader reader) {
        Asset obj = new Asset();
        reader.beginList();
        obj.setCoinName(reader.readNullable(String.class));
        obj.setAmount(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getCoinName());
        writer.writeNullable(this.getAmount());
        writer.end();
    }

    public static Asset fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Asset.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Asset.writeObject(writer, this);
        return writer.toByteArray();
    }
}
