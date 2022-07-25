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

package foundation.icon.btp.xcall;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class CSMessageRequest {
    private final String from;
    private final String to;
    private final boolean rollback;
    private final byte[] data;

    public CSMessageRequest(String from, String to, boolean rollback, byte[] data) {
        this.from = from;
        this.to = to;
        this.rollback = rollback;
        this.data = data;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public byte[] getData() {
        return data;
    }

    public boolean needRollback() {
        return rollback;
    }

    public static void writeObject(ObjectWriter w, CSMessageRequest m) {
        w.beginList(4);
        w.write(m.from);
        w.write(m.to);
        w.write(m.rollback);
        w.writeNullable(m.data);
        w.end();
    }

    public static CSMessageRequest readObject(ObjectReader r) {
        r.beginList();
        CSMessageRequest m = new CSMessageRequest(
                r.readString(),
                r.readString(),
                r.readBoolean(),
                r.readNullable(byte[].class)
        );
        r.end();
        return m;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        CSMessageRequest.writeObject(writer, this);
        return writer.toByteArray();
    }

    public static CSMessageRequest fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }
}
