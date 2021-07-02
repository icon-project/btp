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

package com.iconloop.btp.bmc;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Relayer {
    private Address addr; //primary key
    private String desc;
    private long since;
    private int sinceExtra;
    private BigInteger bond;
    private BigInteger reward;

    public Address getAddr() {
        return addr;
    }

    public void setAddr(Address addr) {
        this.addr = addr;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getSince() {
        return since;
    }

    public void setSince(long since) {
        this.since = since;
    }

    public int getSinceExtra() {
        return sinceExtra;
    }

    public void setSinceExtra(int sinceExtra) {
        this.sinceExtra = sinceExtra;
    }

    public BigInteger getBond() {
        return bond;
    }

    public void setBond(BigInteger bond) {
        this.bond = bond;
    }

    public BigInteger getReward() {
        return reward;
    }

    public void setReward(BigInteger reward) {
        this.reward = reward;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Relayer{");
        sb.append("addr=").append(addr);
        sb.append(", desc='").append(desc).append('\'');
        sb.append(", since=").append(since);
        sb.append(", sinceExtra=").append(sinceExtra);
        sb.append(", bond=").append(bond);
        sb.append(", reward=").append(reward);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Relayer obj) {
        obj.writeObject(writer);
    }

    public static Relayer readObject(ObjectReader reader) {
        Relayer obj = new Relayer();
        reader.beginList();
        obj.setAddr(reader.readNullable(Address.class));
        obj.setDesc(reader.readNullable(String.class));
        obj.setSince(reader.readLong());
        obj.setSinceExtra(reader.readInt());
        obj.setBond(reader.readNullable(BigInteger.class));
        obj.setReward(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(6);
        writer.writeNullable(this.getAddr());
        writer.writeNullable(this.getDesc());
        writer.write(this.getSince());
        writer.write(this.getSinceExtra());
        writer.writeNullable(this.getBond());
        writer.writeNullable(this.getReward());
        writer.end();
    }
}
