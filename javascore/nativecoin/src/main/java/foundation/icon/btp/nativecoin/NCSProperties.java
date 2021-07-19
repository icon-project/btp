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

public class NCSProperties {
    public static final NCSProperties DEFAULT;

    static {
        DEFAULT = new NCSProperties();
        DEFAULT.setSn(BigInteger.ZERO);
        DEFAULT.setFeeRatio(BigInteger.valueOf(10));
    }

    private BigInteger sn;
    private BigInteger feeRatio;

    public BigInteger getSn() {
        return sn;
    }

    public void setSn(BigInteger sn) {
        this.sn = sn;
    }

    public BigInteger getFeeRatio() {
        return feeRatio;
    }

    public void setFeeRatio(BigInteger feeRatio) {
        this.feeRatio = feeRatio;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NCSProperties{");
        sb.append("sn=").append(sn);
        sb.append(", feeRatio=").append(feeRatio);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, NCSProperties obj) {
        obj.writeObject(writer);
    }

    public static NCSProperties readObject(ObjectReader reader) {
        NCSProperties obj = new NCSProperties();
        reader.beginList();
        obj.setSn(reader.readNullable(BigInteger.class));
        obj.setFeeRatio(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getSn());
        writer.writeNullable(this.getFeeRatio());
        writer.end();
    }

    public static NCSProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return NCSProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        NCSProperties.writeObject(writer, this);
        return writer.toByteArray();
    }

}
