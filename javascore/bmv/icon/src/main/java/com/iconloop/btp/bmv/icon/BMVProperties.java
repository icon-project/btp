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

package com.iconloop.btp.bmv.icon;

import com.iconloop.score.util.StringUtil;
import score.*;

public class BMVProperties {
    public static final BMVProperties DEFAULT;

    static {
        DEFAULT = new BMVProperties();
    }

    private Address bmc;
    private String net;
    private long lastHeight;
    private Validators validators;
    private MerkleTreeAccumulator mta;

    public Address getBmc() {
        return bmc;
    }

    public void setBmc(Address bmc) {
        this.bmc = bmc;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public long getLastHeight() {
        return lastHeight;
    }

    public void setLastHeight(long lastHeight) {
        this.lastHeight = lastHeight;
    }

    public Validators getValidators() {
        return validators;
    }

    public void setValidators(Validators validators) {
        this.validators = validators;
    }

    public MerkleTreeAccumulator getMta() {
        return mta;
    }

    public void setMta(MerkleTreeAccumulator mta) {
        this.mta = mta;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVProperties{");
        sb.append("bmc=").append(bmc);
        sb.append(", net='").append(net).append('\'');
        sb.append(", lastHeight=").append(lastHeight);
        sb.append(", validators=").append(validators);
        sb.append(", mta=").append(mta);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BMVProperties obj) {
        obj.writeObject(writer);
    }

    public static BMVProperties readObject(ObjectReader reader) {
        BMVProperties obj = new BMVProperties();
        reader.beginList();
        obj.setBmc(reader.readNullable(Address.class));
        obj.setNet(reader.readNullable(String.class));
        obj.setLastHeight(reader.readLong());
        obj.setValidators(reader.readNullable(Validators.class));
        obj.setMta(reader.readNullable(MerkleTreeAccumulator.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(5);
        writer.writeNullable(this.getBmc());
        writer.writeNullable(this.getNet());
        writer.write(this.getLastHeight());
        Validators validators = this.getValidators();
        writer.writeNullable(validators);
        MerkleTreeAccumulator mta = this.getMta();
        writer.writeNullable(mta);
        writer.end();
    }

    public static BMVProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMVProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMVProperties.writeObject(writer, this);
        return writer.toByteArray();
    }
}
