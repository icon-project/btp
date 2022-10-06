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

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BTPMessage {
    private BTPAddress src;
    private BTPAddress dst;
    private String svc;
    private BigInteger sn;
    private byte[] payload;
    private FeeInfo feeInfo;

    public BTPAddress getSrc() {
        return src;
    }

    public void setSrc(BTPAddress src) {
        this.src = src;
    }

    public BTPAddress getDst() {
        return dst;
    }

    public void setDst(BTPAddress dst) {
        this.dst = dst;
    }

    public String getSvc() {
        return svc;
    }

    public void setSvc(String svc) {
        this.svc = svc;
    }

    public BigInteger getSn() {
        return sn;
    }

    public void setSn(BigInteger sn) {
        this.sn = sn;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public FeeInfo getFeeInfo() {
        return feeInfo;
    }

    public void setFeeInfo(FeeInfo feeInfo) {
        this.feeInfo = feeInfo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BTPMessage{");
        sb.append("src=").append(src);
        sb.append(", dst=").append(dst);
        sb.append(", svc='").append(svc).append('\'');
        sb.append(", sn=").append(sn);
        sb.append(", payload=").append(StringUtil.bytesToHex(payload));
        sb.append(", feeInfo=").append(feeInfo);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BTPMessage obj) {
        obj.writeObject(writer);
    }

    public static BTPMessage readObject(ObjectReader reader) {
        BTPMessage obj = new BTPMessage();
        reader.beginList();
        obj.setSrc(reader.read(BTPAddress.class));
        obj.setDst(reader.read(BTPAddress.class));
        obj.setSvc(reader.read(String.class));
        obj.setSn(reader.read(BigInteger.class));
        obj.setPayload(reader.read(byte[].class));
        obj.setFeeInfo(reader.readNullable(FeeInfo.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(6);
        writer.write(this.getSrc());
        writer.write(this.getDst());
        writer.write(this.getSvc());
        writer.write(this.getSn());
        writer.write(this.getPayload());
        writer.writeNullable(this.getFeeInfo());
        writer.end();
    }

    public static BTPMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BTPMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BTPMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
