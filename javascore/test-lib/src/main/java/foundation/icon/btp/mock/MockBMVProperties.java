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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BMVStatus;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Keep;

public class MockBMVProperties extends BMVStatus {
    public static final MockBMVProperties DEFAULT;

    static {
        DEFAULT = new MockBMVProperties();
        DEFAULT.setHeight(Context.getBlockHeight());
        DEFAULT.setOffset(Context.getBlockHeight());
        DEFAULT.setLast_height(Context.getBlockHeight());
    }

    public MockBMVProperties() {
        super();
    }

    public MockBMVProperties(BMVStatus obj) {
        super();
        this.setHeight(obj.getHeight());
        this.setOffset(obj.getOffset());
        this.setLast_height(obj.getLast_height());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MockBMVProperties{");
        sb.append('}').append(super.toString());
        return sb.toString();
    }

    @Keep
    public static void writeObject(ObjectWriter writer, BMVStatus obj) {
        MockBMVProperties.writeObject(writer, obj instanceof MockBMVProperties ? (MockBMVProperties)obj : new MockBMVProperties(obj));
    }

    public static void writeObject(ObjectWriter writer, MockBMVProperties obj) {
        obj.writeObject(writer);
    }

    public static MockBMVProperties readObject(ObjectReader reader) {
        MockBMVProperties obj = new MockBMVProperties();
        reader.beginList();
        obj.setHeight(reader.readLong());
        obj.setOffset(reader.readLong());
        obj.setLast_height(reader.readLong());
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getHeight());
        writer.write(this.getOffset());
        writer.write(this.getLast_height());
        writer.end();
    }

    public static MockBMVProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MockBMVProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        MockBMVProperties.writeObject(writer, this);
        return writer.toByteArray();
    }

    public static byte[] toBytes(BMVStatus obj) {
        return obj instanceof MockBMVProperties ? ((MockBMVProperties)obj).toBytes() : new MockBMVProperties(obj).toBytes();
    }

}
