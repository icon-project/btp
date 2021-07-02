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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class SackMessage {
    private long height;
    private BigInteger seq;

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public BigInteger getSeq() {
        return seq;
    }

    public void setSeq(BigInteger seq) {
        this.seq = seq;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SackMessage{");
        sb.append("height=").append(height);
        sb.append(", seq=").append(seq);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, SackMessage obj) {
        obj.writeObject(writer);
    }

    public static SackMessage readObject(ObjectReader reader) {
        SackMessage obj = new SackMessage();
        reader.beginList();
        obj.setHeight(reader.readLong());
        obj.setSeq(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getHeight());
        writer.writeNullable(this.getSeq());
        writer.end();
    }

    public static SackMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return SackMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        SackMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
