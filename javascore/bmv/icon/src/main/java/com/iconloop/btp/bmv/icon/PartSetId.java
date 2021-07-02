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
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class PartSetId {
    private long count;
    private byte[] hash;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PartSetId{");
        sb.append("count=").append(count);
        sb.append(", hash=").append(StringUtil.bytesToHex(hash));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, PartSetId obj) {
        obj.writeObject(writer);
    }

    public static PartSetId readObject(ObjectReader reader) {
        PartSetId obj = new PartSetId();
        reader.beginList();
        obj.setCount(reader.readLong());
        obj.setHash(reader.readNullable(byte[].class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getCount());
        writer.writeNullable(this.getHash());
        writer.end();
    }

    public static PartSetId fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return PartSetId.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        PartSetId.writeObject(writer, this);
        return writer.toByteArray();
    }
}
