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

import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BMCRequest {
    private String dst;
    private BMCMessage msg;
    private Address caller;

    public BMCRequest() {
    }

    public BMCRequest(String dst, BMCMessage msg, Address caller) {
        this.dst = dst;
        this.msg = msg;
        this.caller = caller;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public BMCMessage getMsg() {
        return msg;
    }

    public void setMsg(BMCMessage msg) {
        this.msg = msg;
    }

    public Address getCaller() {
        return caller;
    }

    public void setCaller(Address caller) {
        this.caller = caller;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMCRequest{");
        sb.append("dst='").append(dst).append('\'');
        sb.append(", msg=").append(msg);
        sb.append(", caller=").append(caller);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BMCRequest obj) {
        obj.writeObject(writer);
    }

    public static BMCRequest readObject(ObjectReader reader) {
        BMCRequest obj = new BMCRequest();
        reader.beginList();
        obj.setDst(reader.readString());
        obj.setMsg(reader.read(BMCMessage.class));
        obj.setCaller(reader.readAddress());
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getDst());
        writer.write(this.getMsg());
        writer.write(this.getCaller());
        writer.end();
    }

    public static BMCRequest fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMCRequest.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMCRequest.writeObject(writer, this);
        return writer.toByteArray();
    }
}
