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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class ResponseInfo {
    private BigInteger nsn;
    private FeeInfo feeInfo;

    public ResponseInfo() {
    }

    public ResponseInfo(BigInteger nsn, FeeInfo feeInfo) {
        this.nsn = nsn;
        this.feeInfo = feeInfo;
    }

    public BigInteger getNsn() {
        return nsn;
    }

    public void setNsn(BigInteger nsn) {
        this.nsn = nsn;
    }

    public FeeInfo getFeeInfo() {
        return feeInfo;
    }

    public void setFeeInfo(FeeInfo feeInfo) {
        this.feeInfo = feeInfo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponseInfo{");
        sb.append("nsn=").append(nsn);
        sb.append(", feeInfo=").append(feeInfo);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, ResponseInfo obj) {
        obj.writeObject(writer);
    }

    public static ResponseInfo readObject(ObjectReader reader) {
        ResponseInfo obj = new ResponseInfo();
        reader.beginList();
        obj.setNsn(reader.readBigInteger());
        obj.setFeeInfo(reader.readNullable(FeeInfo.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getNsn());
        writer.writeNullable(this.getFeeInfo());
        writer.end();
    }

    public static ResponseInfo fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ResponseInfo.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ResponseInfo.writeObject(writer, this);
        return writer.toByteArray();
    }
}
