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

import com.iconloop.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BMCMessage {
    private String type;
    private byte[] payload;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMCMessage{");
        sb.append("type='").append(type).append('\'');
        sb.append(", payload=").append(StringUtil.toString(payload));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BMCMessage obj) {
        obj.writeObject(writer);
    }

    public static BMCMessage readObject(ObjectReader reader) {
        BMCMessage obj = new BMCMessage();
        reader.beginList();
        obj.setType(reader.readNullable(String.class));
        obj.setPayload(reader.readNullable(byte[].class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getType());
        writer.writeNullable(this.getPayload());
        writer.end();
    }

    public static BMCMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMCMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMCMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
