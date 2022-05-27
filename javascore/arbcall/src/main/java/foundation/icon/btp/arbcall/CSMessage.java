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

package foundation.icon.btp.arbcall;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class CSMessage {
    public static final int REQUEST = 1;
    public static final int RESPONSE = 2;

    private final int type;
    private final byte[] data;

    public CSMessage(int type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public static void writeObject(ObjectWriter w, CSMessage m) {
        w.beginList(2);
        w.write(m.type);
        w.writeNullable(m.data);
        w.end();
    }

    public static CSMessage readObject(ObjectReader r) {
        r.beginList();
        CSMessage m = new CSMessage(
                r.readInt(),
                r.readNullable(byte[].class)
        );
        r.end();
        return m;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        CSMessage.writeObject(writer, this);
        return writer.toByteArray();
    }

    public static CSMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }
}
