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

package foundation.icon.btp.bmc;

import score.*;

public class BMCProperties {
    public static final BMCProperties DEFAULT;

    static {
        DEFAULT = new BMCProperties();
    }

    private long feeGatheringTerm;
    private long feeGatheringNext;
    private Address feeAggregator;

    public long getFeeGatheringTerm() {
        return feeGatheringTerm;
    }

    public void setFeeGatheringTerm(long feeGatheringTerm) {
        this.feeGatheringTerm = feeGatheringTerm;
    }

    public long getFeeGatheringNext() {
        return feeGatheringNext;
    }

    public void setFeeGatheringNext(long feeGatheringNext) {
        this.feeGatheringNext = feeGatheringNext;
    }

    public Address getFeeAggregator() {
        return feeAggregator;
    }

    public void setFeeAggregator(Address feeAggregator) {
        this.feeAggregator = feeAggregator;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMCProperties{");
        sb.append("feeGatheringTerm=").append(feeGatheringTerm);
        sb.append(", feeGatheringNext=").append(feeGatheringNext);
        sb.append(", feeAggregator=").append(feeAggregator);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BMCProperties obj) {
        obj.writeObject(writer);
    }

    public static BMCProperties readObject(ObjectReader reader) {
        BMCProperties obj = new BMCProperties();
        reader.beginList();
        obj.setFeeGatheringTerm(reader.readLong());
        obj.setFeeGatheringNext(reader.readLong());
        obj.setFeeAggregator(reader.readNullable(Address.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getFeeGatheringTerm());
        writer.write(this.getFeeGatheringNext());
        writer.writeNullable(this.getFeeAggregator());
        writer.end();
    }

    public static BMCProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMCProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMCProperties.writeObject(writer, this);
        return writer.toByteArray();
    }
}
