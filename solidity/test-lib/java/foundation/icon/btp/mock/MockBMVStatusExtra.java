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

package foundation.icon.btp.mock;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class MockBMVStatusExtra {
    private long offset;
    private long lastHeight;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLastHeight() {
        return lastHeight;
    }

    public void setLastHeight(long lastHeight) {
        this.lastHeight = lastHeight;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MockBMVExtra{");
        sb.append("offset=").append(offset);
        sb.append(", lastHeight=").append(lastHeight);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, MockBMVStatusExtra obj) {
        obj.writeObject(writer);
    }

    public static MockBMVStatusExtra readObject(ObjectReader reader) {
        MockBMVStatusExtra obj = new MockBMVStatusExtra();
        reader.beginList();
        obj.setOffset(reader.readLong());
        obj.setLastHeight(reader.readLong());
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getOffset());
        writer.write(this.getLastHeight());
        writer.end();
    }

    public static MockBMVStatusExtra fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MockBMVStatusExtra.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        MockBMVStatusExtra.writeObject(writer, this);
        return writer.toByteArray();
    }
}
