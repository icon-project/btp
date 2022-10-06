/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class Fee {
    private String destination;
    private BigInteger[] values;

    public Fee() {
    }

    public Fee(String destination, BigInteger[] values) {
        this.destination = destination;
        this.values = values;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public BigInteger[] getValues() {
        return values;
    }

    public void setValues(BigInteger[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Fee{");
        sb.append("destination=").append(destination);
        sb.append(", values=").append(StringUtil.toString(values));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Fee obj) {
        obj.writeObject(writer);
    }

    public static Fee readObject(ObjectReader reader) {
        Fee obj = new Fee();
        reader.beginList();
        obj.setDestination(reader.readString());
        reader.beginList();
        List<BigInteger> valuesList = new ArrayList<>();
        while(reader.hasNext()) {
            valuesList.add(reader.readBigInteger());
        }
        BigInteger[] values = new BigInteger[valuesList.size()];
        for(int i=0; i<valuesList.size(); i++) {
            values[i] = valuesList.get(i);
        }
        obj.setValues(values);
        reader.end();
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getDestination());
        BigInteger[] values = this.getValues();
        writer.beginList(values.length);
        for(BigInteger v : values) {
            writer.write(v);
        }
        writer.end();
        writer.end();
    }

    public static Fee fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Fee.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Fee.writeObject(writer, this);
        return writer.toByteArray();
    }
}
